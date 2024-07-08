package io.github.digitalsmile;

import javax.lang.model.SourceVersion;
import java.util.HashMap;
import java.util.Map;

public class PrettyName {

    private static final Map<String, String> NAMING_CACHE = new HashMap<>();

    public static void addName(String name, String javaName) {
        NAMING_CACHE.put(name, checkJavaName(javaName));
    }

    public static String getVariableName(String name) {
        var cachedName = NAMING_CACHE.get(name);
        if (cachedName != null) {
            return cachedName;
        }
        name = checkJavaName(name);
        if (name.matches("([a-z]+[a-zA-Z0-9]+)+")) {
            return name;
        }
        var words = name.split("[\\W_-]+");
        var builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            var word = words[i];
            if (i == 0) {
                word = word.isEmpty() ? word : word.toLowerCase();
            } else {
                word = word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
            }
            builder.append(word);
        }
        return builder.toString();
    }

    public static String getObjectName(String name) {
        var cachedName = NAMING_CACHE.get(name);
        if (cachedName != null) {
            return cachedName;
        }
        name = checkJavaName(name);
        if (name.matches("([A-Z]+[a-zA-Z0-9]+)+")) {
            return name;
        }
        var words = name.split("[\\W_-]+");
        var builder = new StringBuilder();
        for (String s : words) {
            var word = s;
            word = word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
            builder.append(word);
        }
        return builder.toString();
    }

    private static String checkJavaName(String name) {
        return SourceVersion.isKeyword(name) ? "_" + name : name;
    }
}
