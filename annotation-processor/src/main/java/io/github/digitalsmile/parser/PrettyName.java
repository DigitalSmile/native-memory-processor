package io.github.digitalsmile.parser;

public class PrettyName {
    public static String get(String name) {
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

    public static String getFileName(String name) {
        if (name.matches("([A-Z]+[a-zA-Z0-9]+)+")) {
            return name;
        }
        var words = name.split("[\\W_-]+");
        var builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            var word = words[i];
            word = word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
            builder.append(word);
        }
        return builder.toString();
    }
}
