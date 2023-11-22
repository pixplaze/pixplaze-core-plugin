package com.pixplaze.util;

import com.pixplaze.plugin.PixplazeCorePlugin;

public class Utils {

    public static boolean checkToken(String token) {
        return token.equals(PixplazeCorePlugin.getInstance().getConfig().getString("access-token"));
    }
}
