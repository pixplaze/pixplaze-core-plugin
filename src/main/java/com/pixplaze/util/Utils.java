package com.pixplaze.util;

import com.pixplaze.plugin.PixplazeRootsAPI;

public class Utils {

    public static boolean checkToken(String token) {
        return token.equals(PixplazeRootsAPI.getInstance().getConfig().getString("access-token"));
    }
}
