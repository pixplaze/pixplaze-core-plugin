package com.pixplaze.util;

import com.pixplaze.plugin.PixplazeCorePlugin;

public class Utils {

    public static boolean checkToken(String token) {
        return token.equals(PixplazeCorePlugin.getInstance().getConfig().getString("access-token"));
    }

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
