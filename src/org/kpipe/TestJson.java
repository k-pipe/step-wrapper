package org.kpipe;

import java.util.Map;

public class TestJson {

    public static void main(String... args) {
        test("""
         1       
         """, Map.of(
                "", "1"
        ));


        test("""
         " abc "       
         """, Map.of(
                "", " abc "
        ));


        test("""
         { 
            "a": 1        
         }       
         """, Map.of(
                 ".a", "1"
        ));


        test("""
         { 
            "a": "1"        
         }       
         """, Map.of(
                ".a", "1"
        ));

        test("""
         [ 
            1, 
            2,
            "a"
         ]        
         """, Map.of(
                "[0]", "1",
                "[1]", "2",
                "[2]", "a"
        ));

        test("""
         [ 
            1, 
            {
              "a": 2
            }
         ]        
         """, Map.of(
                "[0]", "1",
                "[1].a", "2"
        ));


        test("""
         [ 
            1, 
            2,
            {
               "a": 3,
               "b": [4,5]
            }
         ]        
         """, Map.of(
                "[0]", "1",
                "[1]", "2",
                "[2].a", "3",
                "[2].b[0]", "4",
                "[2].b[1]", "5"
        ));


        test("""
         { 
            "a": {
               "b": 1,
               "c": 2
            },
            "e": 3        
         }       
         """, Map.of(
                        ".a.b", "1",
                        ".a.c", "2",
                        ".e", "3"
        ));
        System.out.println("All json tests passed");
    }

    private static void test(String json, Map<String, String> kv) {
        Map<String, String> map = Json.asMap(json);
        kv.forEach((jsonPath, expected) -> {
            String found = map.get(jsonPath);
            if (!expected.equals(found)) {
                System.out.println("Json tested: ");
                System.out.println(json);
                System.out.println();
                System.out.println("Map returned: ");
                map.forEach((k, v) -> System.out.println("  " + k + ": " + v));
                throw new RuntimeException("Expected '" + expected + "' but found '" + found + "'");
            }
        });
        if (kv.size() != map.size()) {
            throw new RuntimeException("Expected map size " + kv.size() + " but found map of size " + map.size());
        }
    }
}
