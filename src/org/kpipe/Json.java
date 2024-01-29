package org.kpipe;

import java.util.*;

public class Json {

    public static Map<String,String> asMap(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        addEntries(result, "", asTree(json.trim()));
        return result;
    }

    private static void addEntries(Map<String, String> result, String prefix, Object object) {
        if (object instanceof List) {
            int i = 0;
            for (Object item : (List)object) {
                addEntries(result, prefix+"["+i+"]", item);
                i++;
            }
        } else if (object instanceof Map) {
            ((Map)object).forEach((k,v) -> addEntries(result, prefix+"."+k, v));
        } else {
            result.put(prefix, object.toString());
        }
    }

    public static Object asTree(String json) {
        if (json.startsWith("{")) {
            return parseMap(json);
        } else if (json.startsWith("[")) {
            return parseList(json);
        }
        if (json.startsWith("\"")) {
            return parseString(json);
        } else return json;
    }

    private static Map<String, Object> parseMap(String json) {
        Map<String, Object> res = new LinkedHashMap<>();
        if (!json.endsWith("}")) {
            throw new JsonException("expected closing } at the end of: "+json);
        }
        splitRespectingBraketsAndQuotes(json.substring(1, json.length()-1), ',')
                .forEach(p -> addKeyValue(res, splitRespectingBraketsAndQuotes(p, ':')));
        return res;
    }

    private static void addKeyValue(Map<String, Object> res, List<String> keyValue) {
        if (keyValue.size() != 2) {
            throw new JsonException("expected key and value separated by : character");
        }
        res.put(parseString(keyValue.get(0)), asTree(keyValue.get(1)));
    }

    private static List<Object> parseList(String json) {
        List<Object> res = new LinkedList<>();
        if (!json.endsWith("]")) {
            throw new JsonException("expected closing ] at the end of: "+json);
        }
        splitRespectingBraketsAndQuotes(json.substring(1, json.length()-1), ',')
                .forEach(v -> res.add(asTree(v)));
        return res;
    }

    private static String parseString(String s) {
        if (!(s.startsWith("\"") && s.endsWith("\""))) {
            throw new JsonException("expected closing quote at the end of: "+s);
        }
        return s.substring(1, s.length()-1);
    }

    private static List<String> splitRespectingBraketsAndQuotes(String string, char separator) {
        List<String> res = new ArrayList<>();
        int prevPos = -1;
        boolean insideQuote = false;
        int numSlashes = 0;
        int curlBracketLevel = 0;
        int rectBracketLevel = 0;
        for (int pos = 0; pos < string.length(); pos++) {
            char ch = string.charAt(pos);
            if ((ch == separator) && (curlBracketLevel == 0) && (rectBracketLevel == 0) && !insideQuote) {
                res.add(string.substring(prevPos+1, pos).trim());
                prevPos = pos;
            }
            if ((ch == '"') && (curlBracketLevel == 0) && (rectBracketLevel == 0)) {
                if ((numSlashes % 2) == 0) {
                    insideQuote = !insideQuote;
                }
            }
            if (ch == '\\') {
                numSlashes++;
            } else {
                numSlashes = 0;
            }
            if ((ch == '{') && !insideQuote) {
                curlBracketLevel++;
            }
            if ((ch == '}') && !insideQuote) {
                curlBracketLevel--;
                if (curlBracketLevel < 0) {
                    throw new JsonException("more } chars than { chars in Json string");
                }
            }
            if ((ch == '[') && !insideQuote) {
                rectBracketLevel++;
            }
            if ((ch == ']') && !insideQuote) {
                rectBracketLevel--;
                if (rectBracketLevel < 0) {
                    throw new JsonException("more ] chars than [ chars in Json string");
                }
            }
        }
        if (rectBracketLevel != 0) {
            throw new JsonException("mismatching ] chars and [ chars in Json string");
        }
        if (curlBracketLevel != 0) {
            throw new JsonException("mismatching } chars and { chars in Json string");
        }
        res.add(string.substring(prevPos+1).trim());
        return res;
    }

}
