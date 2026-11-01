package com.pixplaze.plugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Keys of {@code config.yml}.
 *
 * <p>{@code saveDefaultConfig} copies {@code config.yml} from the jar only when there is no file, so after an update
 * new keys would be missing and silently taken from the jar defaults. On start the missing keys are written with
 * their default values and comments; values the admin set stay. Old keys are not migrated (alpha).
 */
public final class PluginConfig {

    public static final String GATEWAY = "pixplaze.gateway";
    public static final String PUBLIC_HOST = "public.host";
    public static final String PUBLIC_MAP = "public.map";
    public static final String PUBLIC_API = "public.api";
    public static final String HTTP_IP = "http.ip";
    public static final String HTTP_PORT = "http.port";
    public static final String AUTHORIZATION_ENABLED = "authorization.enabled";
    public static final String AUTHORIZATION_TIMEOUT_MINUTES = "authorization.timeout-minutes";
    public static final String AUTHORIZATION_AUTO_LOGIN = "authorization.auto-login";

    private PluginConfig() {
    }

    /// Writes missing keys with defaults and comments; saves if anything was added.
    static void upgrade(PixplazeCorePlugin plugin) {
        final var added = addMissing(plugin.getConfig());
        if (!added.isEmpty()) {
            plugin.saveConfig();
            plugin.getLogger().warning("config.yml: added keys with default values: " + String.join(", ", added) + ". Check them");
        }
    }

    /// Adds keys of the jar defaults that the file lacks; returns them. {@code config} must have the defaults set.
    static List<String> addMissing(FileConfiguration config) {
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
