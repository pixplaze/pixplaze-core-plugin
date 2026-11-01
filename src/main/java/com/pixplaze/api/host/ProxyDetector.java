package com.pixplaze.api.host;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/// Best-effort определение, работает ли сервер за прокси, по конфигам ядра.
///
/// Читает файлы из рабочего каталога сервера:
/// - {@code spigot.yml} -> {@code settings.bungeecord} (BungeeCord/Waterfall);
/// - {@code config/paper-global.yml} -> {@code proxies.velocity.enabled} (Velocity, Paper).
///
/// Используется для предупреждения оператору (резолвер хоста определяется реально приходящими
/// адресами) и для лицензии сервера ({@link ServerLicense}): сервер за прокси считается нелицензионным.
public final class ProxyDetector {

    private ProxyDetector() {
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
