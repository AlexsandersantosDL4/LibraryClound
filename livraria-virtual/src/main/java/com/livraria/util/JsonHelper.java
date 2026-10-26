package com.livraria.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitário leve em Java puro para manipulação de JSON.
 */
public class JsonHelper {

    public static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return map;
        }

        String content = json.trim();
        if (content.startsWith("{")) content = content.substring(1);
        if (content.endsWith("}")) content = content.substring(0, content.length() - 1);

        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"[^\"]*\"|true|false|null|-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String key = matcher.group(1);
            String rawVal = matcher.group(2).trim();

            if (rawVal.startsWith("\"") && rawVal.endsWith("\"")) {
                String unquoted = rawVal.substring(1, rawVal.length() - 1);
                unquoted = unquoted.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n").replace("\\r", "\r");
                map.put(key, unquoted);
            } else if ("null".equalsIgnoreCase(rawVal)) {
                map.put(key, null);
            } else {
                map.put(key, rawVal);
            }
        }
        return map;
    }

    public static String getString(Map<String, String> map, String key, String defaultValue) {
        if (!map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }
        return map.get(key).trim();
    }

    public static int getInt(Map<String, String> map, String key, int defaultValue) {
        try {
            if (map.containsKey(key) && map.get(key) != null) {
                return Integer.parseInt(map.get(key).trim());
            }
        } catch (NumberFormatException ignored) {
        }
        return defaultValue;
    }

    public static String escape(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
