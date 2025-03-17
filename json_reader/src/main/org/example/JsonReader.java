package org.example;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static org.example.JsonTokenType.*;

public final class JsonReader
{
    private int batchSize = -1;
    public JsonReader()
    {
    }

    public List<JsonToken> tokenize(String filePath) throws IOException
    {
        FileChannel channel = FileChannel.open(Path.of(filePath), StandardOpenOption.READ);
        if(channel.size() > Integer.MAX_VALUE){
            throw new IllegalArgumentException("Test");
        }
        List<JsonToken> tokens = new ArrayList<>();
        tokens.add(DefaultToken.START_FILE_TOKEN);
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        int batchSize = (this.batchSize == -1) ? chooseBatchSize(channel.size()) : this.batchSize;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(batchSize);
        CharBuffer charBuffer = CharBuffer.allocate(batchSize);
        while(channel.read(byteBuffer) != -1){
            byteBuffer.flip();
            if(tokens.getLast().type() == BATCH_SPLIT){
                charBuffer.put(tokens.removeLast().value());
            }
            decoder.decode(byteBuffer, charBuffer, false);
            charBuffer.flip();
            tokenizeBuffer(charBuffer, tokens);
            charBuffer.clear();
            byteBuffer.clear();
        }
        tokens.add(DefaultToken.END_FILE_TOKEN);
        channel.close();
        return tokens;
    }

    private int chooseBatchSize(long channelSize)
    {
        if(channelSize <= 1_048_576){
            return (int)channelSize;
        }else if(channelSize < 104_857_600){
            return 1_048_576;
        }else{
            return 5_242_880;
        }
    }

    private void tokenizeBuffer(CharBuffer charBuffer, List<JsonToken> tokens)
    {
        while(charBuffer.hasRemaining()) {
            char c = charBuffer.get();
            if(Character.isWhitespace(c)){
                continue;
            }
            switch (c) {
                case '{' -> tokens.add(DefaultToken.START_OBJECT_TOKEN);
                case '}' -> tokens.add(DefaultToken.END_OBJECT_TOKEN);
                case '[' -> tokens.add(DefaultToken.START_ARRAY_TOKEN);
                case ']' -> tokens.add(DefaultToken.END_ARRAY_TOKEN);
                case ':' -> tokens.add(DefaultToken.COLON_TOKEN);
                case ',' -> tokens.add(DefaultToken.COMMA_TOKEN);
                case '"' -> tokens.add(readWord(charBuffer));
                case 't', 'f', 'n' -> {
                    charBuffer.position(charBuffer.position() - 1);
                    tokens.add(readKeyword(charBuffer));
                }
                default -> {
                    if (isNumber(c)) {
                        charBuffer.position(charBuffer.position() - 1);
                        tokens.add(readNumber(charBuffer));
                    } else {
                        throw new IllegalArgumentException("Invalid character in buffer.");
                    }
                }
            }
        }
    }

    private JsonToken readKeyword(CharBuffer buffer)
    {
        int start = buffer.position();
        while(buffer.hasRemaining()){
            char c = buffer.get();
            if(Character.isWhitespace(c) || c == ']' || c == '}' || c == ','){
                buffer.position(buffer.position() - 1);
                break;
            }
        }
        String s = new String(buffer.array(), start, buffer.position() - start);
        switch(s){
            case "true" -> {return DefaultToken.TRUE_TOKEN;}
            case "false" -> {return DefaultToken.FALSE_TOKEN;}
            case "null" -> {return DefaultToken.NULL_TOKEN;}
            default -> {
                if(buffer.position() != buffer.limit()){
                    throw new IllegalJsonFormatException("Cannot tokenize an invalid keyword.");
                }
                return new JsonToken(JsonTokenType.BATCH_SPLIT, s);
            }
        }
    }

    private JsonToken readWord(CharBuffer charBuffer)
    {
        int start = charBuffer.position();
        boolean isEscapeSequence = false;
        while(charBuffer.hasRemaining()){
            char c = charBuffer.get();
            if(c == '"' && !isEscapeSequence){
                return new JsonToken(
                        STRING,
                        new String(charBuffer.array(), start, charBuffer.position() - start - 1)
                );
            }
            isEscapeSequence = c == '\\' && !isEscapeSequence;
        }
        return new JsonToken(
                BATCH_SPLIT,
                new String(charBuffer.array(), start - 1, charBuffer.position() - start + 1)
        );
    }

    private JsonToken readNumber(CharBuffer charBuffer)
    {
        int start = charBuffer.position();
        while(charBuffer.hasRemaining()){
            char c = charBuffer.get();
            if(!isNumber(c)){
                charBuffer.position(charBuffer.position() - 1);
                return new JsonToken(
                        JsonTokenType.NUMBER,
                        new String(charBuffer.array(), start, charBuffer.position() - start)
                );
            }
        }
        return new JsonToken(
                JsonTokenType.BATCH_SPLIT,
                new String(charBuffer.array(), start, charBuffer.position() - start)
        );
    }

    public void setBatchSize(int size)
    {
        if(size <= 0){
            throw new IllegalArgumentException("Batch size cannot be negative.");
        }
        this.batchSize = size;
    }

    public Map<String, Object> parse(List<JsonToken> tokens)
    {
        validateTokens(tokens);
        if(tokens.size() == 2){
            return new HashMap<>();
        }
        switch(tokens.get(1).type()){
            case START_OBJECT -> {return parseObject(tokens, new Index(1));}
            case START_ARRAY -> {
                Map<String, Object> map = new HashMap<>();
                map.put("root", parseArray(tokens, new Index(1)));
                return map;
            }
            default -> throw new IllegalJsonFormatException("Json file has to start with { or [.");
        }
    }

    private void validateTokens(List<JsonToken> tokens)
    {
        Stack<JsonTokenType> stack = new Stack<>();
        JsonTokenType previous = null;
        for(int i = 0; i < tokens.size(); i++){
            JsonToken token = tokens.get(i);
            switch(token.type()){
                case BATCH_SPLIT -> throw new IllegalStateException("Batch Split token should never exist.");
                case SOF -> {
                    if(i != 0){
                        throw new IllegalStateException("Start Of File token can only exist as the first token.");
                    }
                }
                case EOF -> {
                    if(i != tokens.size() - 1 && previous != END_OBJECT && previous != END_ARRAY){
                        throw new IllegalStateException("End Of File token can only exist as the last token, and after a closing bracket.");
                    }
                }
                case START_OBJECT -> {
                    if(previous != SOF && previous != COLON && previous != COMMA && previous != START_ARRAY){
                        throw new IllegalJsonFormatException(String.format("Start Object token cannot follow %s", previous));
                    }
                    stack.push(START_OBJECT);
                }
                case END_OBJECT -> {
                    if(stack.size() == 0 || stack.pop() != START_OBJECT){
                        throw new IllegalJsonFormatException("End Of Object token does not match a Start Of Object token.");
                    }
                    if(previous != START_OBJECT && previous != STRING && previous != NUMBER && previous != NULL
                            && previous != TRUE && previous != FALSE && previous != END_ARRAY && previous != END_OBJECT){
                        throw new IllegalJsonFormatException(String.format("End Object token cannot follow %s", previous));
                    }
                }
                case COLON -> {
                    if(previous != STRING){
                        throw new IllegalJsonFormatException(String.format("Colon token cannot follow %s.", previous));
                    }
                }
                case STRING -> {
                    if(previous != COLON && previous != START_OBJECT && previous != START_ARRAY && previous != COMMA){
                        throw new IllegalJsonFormatException(String.format("%s token cannot follow %s", token.type(), previous));
                    }
                    if(i < tokens.size() - 1 && tokens.get(i + 1).type() == COLON){
                        tokens.set(i, new JsonToken(KEY, tokens.get(i).value()));
                    }
                }
                case NUMBER, TRUE, FALSE, NULL -> {
                    if(previous != COLON){
                        throw new IllegalJsonFormatException(String.format("%s token cannot follow %s", token.type(), previous));
                    }
                }
                case COMMA -> {
                    if(previous != END_OBJECT && previous != END_ARRAY && previous != STRING && previous != NUMBER
                            && previous != NULL && previous != TRUE && previous != FALSE){
                        throw new IllegalJsonFormatException(String.format("Comma token cannot follow %s.", previous));
                    }
                }
                case KEY -> {
                    if(previous != START_OBJECT && previous != COMMA){
                        throw new IllegalJsonFormatException(String.format("Key token cannot follow %s.", previous));
                    }
                }
                case START_ARRAY -> {
                    if(previous != SOF && previous != COLON && previous != COMMA && previous != START_ARRAY){
                        throw new IllegalJsonFormatException(String.format("Start Array token cannot follow %s", previous));
                    }
                    stack.push(START_ARRAY);
                }
                case END_ARRAY -> {
                    if(stack.size() == 0 || stack.pop() != START_ARRAY){
                        throw new IllegalJsonFormatException("End Of Array token does not match a Start Of Array token.");
                    }
                    if(previous != START_ARRAY && previous != END_ARRAY && previous != END_OBJECT && previous != STRING && previous != NUMBER
                            && previous != NULL && previous != TRUE && previous != FALSE){
                        throw new IllegalJsonFormatException(String.format("End Array token cannot follow %s", previous));
                    }
                }
                default -> throw new IllegalJsonFormatException(String.format("Illegal json token: %s", token.type()));
            }
            previous = token.type();
        }
        assert(stack.isEmpty());
    }

    private Map<String, Object> parseObject(List<JsonToken> tokens, Index index)
    {
        Map<String, Object> map = new HashMap<>();
        String lastKey = null;
        for(;index.getIndex() < tokens.size(); index.increment()){
            JsonToken token = tokens.get(index.getIndex());
            switch(token.type()){
                case EOF, END_OBJECT -> { return map; }
                case TRUE, FALSE, NULL, NUMBER, STRING -> map.put(lastKey, token.value());
                case START_OBJECT -> map.put(lastKey, parseObject(tokens, index.increment()));
                case START_ARRAY -> map.put(lastKey, parseArray(tokens, index.increment()));
                case KEY -> lastKey = token.value();
            }
        }
        return map;
    }

    private List<Object> parseArray(List<JsonToken> tokens, Index index)
    {
        List<Object> list = new ArrayList<>();
        for(;index.getIndex() < tokens.size(); index.increment()){
            JsonToken token = tokens.get(index.getIndex());
            switch(token.type()){
                case EOF, END_ARRAY -> { return list; }
                case TRUE, FALSE, NULL, NUMBER, STRING -> list.add(token.value());
                case START_OBJECT -> list.add(parseObject(tokens, index.increment()));
                case START_ARRAY -> list.add(parseArray(tokens, index.increment()));
            }
        }
        return list;
    }

    private boolean isNumber(char c)
    {
        return Character.isDigit(c) || c == '-';
    }

    private static class DefaultToken
    {
        private static final JsonToken START_OBJECT_TOKEN = new JsonToken(START_OBJECT, "{");
        private static final JsonToken END_OBJECT_TOKEN = new JsonToken(END_OBJECT, "}");
        private static final JsonToken START_ARRAY_TOKEN = new JsonToken(START_ARRAY, "[");
        private static final JsonToken END_ARRAY_TOKEN = new JsonToken(END_ARRAY, "]");
        private static final JsonToken START_FILE_TOKEN = new JsonToken(SOF, "");
        private static final JsonToken END_FILE_TOKEN = new JsonToken(EOF, "");
        private static final JsonToken COLON_TOKEN = new JsonToken(COLON, ":");
        private static final JsonToken COMMA_TOKEN = new JsonToken(COMMA, ",");
        private static final JsonToken TRUE_TOKEN = new JsonToken(TRUE, "true");
        private static final JsonToken FALSE_TOKEN = new JsonToken(FALSE, "false");
        private static final JsonToken NULL_TOKEN = new JsonToken(NULL, "null");
    }
}
