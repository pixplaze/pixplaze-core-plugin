package com.pixplaze.util;

public class Utils {

    public static String dequotify(String string) {
        string = string.trim();

        final var firstChar = string.charAt(0);
        final var lastChar = string.charAt(string.length() - 1);

        if (firstChar == lastChar && (firstChar == '\'' || firstChar == '\"')) {
            string = string.substring(1, string.length() - 1);
        }

        return string.trim();
    }
}
