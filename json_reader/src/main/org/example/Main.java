package org.example;

import java.io.IOException;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        JsonReader jsonReader = new JsonReader();
        try{
            System.out.println(System.getProperty("user.dir"));
            test(jsonReader, "getTokens with charbuffer directly ");
        }catch(IOException e){
            System.out.println("failed");
        }
    }

    public static void test(JsonReader reader, String testName) throws IOException {
        long totalCount = 0;
        int iter = 10;
        for(int i = 0; i < iter; i++){
            long count = 0;
            for(int j = 0; j < iter; j++){
                long start = System.currentTimeMillis();
                Map<String, Object> map = reader.parse(reader.tokenize("json_reader/src/data/player2.json"));
                count += System.currentTimeMillis() - start;
            }
            totalCount += count;
        }
        System.out.printf("The total time for %d iterations of %s is: %d%n", iter, testName, totalCount);
        System.out.printf("The average time for %d iterations of %s is: %d%n", iter, testName, totalCount / (iter * iter));
        //System.out.println(tokens);
    }
}