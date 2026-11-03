package com.pixplaze.util;

import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ServerUtils {
    public static boolean isLicensed(Server server) {
        return server.getOnlineMode() && !isProxied();
    }

    public static boolean isProxied() {
        return isBungeeEnabled() || isVelocityEnabled();
    }

    private static boolean isBungeeEnabled() {
        return readBoolean(new File("spigot.yml"), "settings.bungeecord");
    }

    private static boolean isVelocityEnabled() {
        return readBoolean(new File("config/paper-global.yml"), "proxies.velocity.enabled");
    }

    private static boolean readBoolean(File file, String path) {
        if (!file.exists()) {
            return false;
        }
        try {
            return YamlConfiguration.loadConfiguration(file).getBoolean(path, false);
        } catch (Exception e) {
            return false; // повреждённый/нестандартный конфиг — молча считаем «не прокси»
        }
    }
}
