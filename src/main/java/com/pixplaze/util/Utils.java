package com.pixplaze.util;

import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Utils {

    public static boolean checkToken(String token) {
        return token.equals(PixplazeCorePlugin.getInstance().getConfig().getString("access-token"));
    }

    public static List<File> getPlugins() {
        return Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .map(Plugin::getDataFolder)
                .toList();
    }
}
