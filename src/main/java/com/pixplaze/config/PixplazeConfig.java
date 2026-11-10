package com.pixplaze.config;

import com.pixplaze.plugin.PixplazeCorePlugin;
import com.pixplaze.util.ServerUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public record PixplazeConfig(
        Gateway gateway,
        Api api,
        Http http,
        Authorization authorization
) {
    public static final String GATEWAY = "pixplaze.gateway";
    public static final String PUBLIC_HOST = "api.host";
    public static final String PUBLIC_MAP = "api.map";
    public static final String PUBLIC_API = "api.api";
    public static final String HTTP_IP = "http.ip";
    public static final String HTTP_PORT = "http.port";
    public static final String AUTHORIZATION_ENABLED = "authorization.enabled";
    public static final String AUTHORIZATION_TIMEOUT_MINUTES = "authorization.timeout-minutes";
    public static final String AUTHORIZATION_AUTO_LOGIN = "authorization.auto-login";

    public record Gateway(String path) {
        public static Gateway load(PixplazeCorePlugin plugin) {
            return new Gateway(plugin.getConfig().getString(GATEWAY, "api.pixplaze.net"));
        }
    }

    public record Api(String host, String map, String api) {
        public static Api load(PixplazeCorePlugin plugin) {
            final var config = plugin.getConfig();
            return new Api(
                    config.getString(PUBLIC_HOST, "localhost"),
                    config.getString(PUBLIC_MAP, "map"),
                    config.getString(PUBLIC_API, "api")
            );
        }
    }

    public record Http(String ip, int port) {
        public static Http load(PixplazeCorePlugin plugin) {
            final var config = plugin.getConfig();
            final var ip = config.getString(HTTP_IP, "auto");
            return new Http(
                    ip.equals("auto") || ip.trim().isBlank() ? "0.0.0.0" : ip,
                    config.getInt(HTTP_PORT, 25566)
            );
        }
    }

    public record Authorization(boolean enabled, boolean licensed, Duration timeout, boolean autoLogin) {
        public static Authorization load(PixplazeCorePlugin plugin) {
            final var licensed = ServerUtils.isLicensed(plugin.getServer());
            final var config = plugin.getConfig();
            var enabled = config.getBoolean(AUTHORIZATION_ENABLED, false);

            if (!licensed && !enabled) {
                enabled = true;
                config.set(AUTHORIZATION_ENABLED, true);
                plugin.saveConfig();
                plugin.getLogger().warning(
                        "The server does not check accounts with Mojang (offline mode or behind a proxy):" +
                                " " + AUTHORIZATION_ENABLED + " is turned on, players must sign in to Pixplaze"
                );
            }

            final var minutes = Math.max(config.getLong(AUTHORIZATION_TIMEOUT_MINUTES, 5), 0);
            final var autoLogin = config.getBoolean(AUTHORIZATION_AUTO_LOGIN, true);

            return new Authorization(enabled, licensed, Duration.ofMinutes(minutes), autoLogin);
        }
    }

    /**
     * Загружает все секции конфигурации и возвращает новый PluginConfig.
     */
    public static PixplazeConfig load(PixplazeCorePlugin plugin) {
        plugin.saveDefaultConfig();
        upgrade(plugin);

        return new PixplazeConfig(
                Gateway.load(plugin),
                Api.load(plugin),
                Http.load(plugin),
                Authorization.load(plugin)
        );
    }

    /**
     * Сбрасывает кэш FileConfiguration на диске и порождает полностью новый PluginConfig.
     */
    public static PixplazeConfig reload(PixplazeCorePlugin plugin) {
        plugin.reloadConfig();
        return load(plugin);
    }

    /// Writes missing keys with defaults and comments; saves if anything was added.
    private static void upgrade(PixplazeCorePlugin plugin) {
        final var added = addMissing(plugin.getConfig());
        if (!added.isEmpty()) {
            plugin.saveConfig();
            plugin.getLogger().warning("config.yml: added keys with default values: " + String.join(", ", added) + ". Check them");
        }
    }

    /// Adds keys of the jar defaults that the file lacks; returns them. {@code config} must have the defaults set.
    private static List<String> addMissing(FileConfiguration config) {
        final var added = new ArrayList<String>();
        final ConfigurationSection defaults = config.getDefaults();
        if (defaults == null) {
            return added;
        }
        for (final var key : defaults.getKeys(true)) {
            if (config.contains(key, true)) {
                continue;
            }
            if (defaults.isConfigurationSection(key)) {
                config.createSection(key);
            } else {
                config.set(key, defaults.get(key));
                added.add(key);
            }
            config.setComments(key, defaults.getComments(key));
            config.setInlineComments(key, defaults.getInlineComments(key));
        }
        return added;
    }
}
