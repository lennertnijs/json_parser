package example;

import org.example.JsonReader;
import org.example.JsonToken;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.example.JsonTokenType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonReaderTest
{

    private static final JsonReader reader = new JsonReader();

    public String getFilePath1()
    {
        return "json_reader/src/test/data/player.json";
    }

    @Test
    public void testTokenizer1()
    {
        try{
            List<JsonToken> tokens = reader.tokenize(getFilePath1());
            List<JsonToken> expectedTokens = new ArrayList<>();
            expectedTokens.add(DefaultToken.START_FILE_TOKEN);
            expectedTokens.add(DefaultToken.START_OBJECT_TOKEN);
            expectedTokens.add(new JsonToken(STRING, "position"));
            expectedTokens.add(DefaultToken.COLON_TOKEN);
            expectedTokens.add(DefaultToken.START_OBJECT_TOKEN);
            expectedTokens.add(new JsonToken(STRING, "x"));
            expectedTokens.add(DefaultToken.COLON_TOKEN);
            expectedTokens.add(new JsonToken(NUMBER, "200"));
            expectedTokens.add(DefaultToken.COMMA_TOKEN);
            expectedTokens.add(new JsonToken(STRING, "y"));
            expectedTokens.add(DefaultToken.COLON_TOKEN);
            expectedTokens.add(new JsonToken(NUMBER, "300"));
            expectedTokens.add(DefaultToken.END_OBJECT_TOKEN);
            expectedTokens.add(DefaultToken.COMMA_TOKEN);
            expectedTokens.add(new JsonToken(STRING, "map"));
            expectedTokens.add(DefaultToken.COLON_TOKEN);
            expectedTokens.add(new JsonToken(STRING, "outside"));
            expectedTokens.add(DefaultToken.COMMA_TOKEN);
            expectedTokens.add(new JsonToken(STRING, "inventory"));
            expectedTokens.add(DefaultToken.COLON_TOKEN);
            expectedTokens.add(DefaultToken.START_OBJECT_TOKEN);
            expectedTokens.add(new JsonToken(STRING, "size"));
            expectedTokens.add(DefaultToken.COLON_TOKEN);
            expectedTokens.add(new JsonToken(NUMBER, "9"));
            expectedTokens.add(DefaultToken.END_OBJECT_TOKEN);
            expectedTokens.add(DefaultToken.END_OBJECT_TOKEN);
            expectedTokens.add(DefaultToken.END_FILE_TOKEN);
            assertEquals(tokens, expectedTokens);
        }catch(IOException e){
            System.out.println("IO error.");
        }
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
