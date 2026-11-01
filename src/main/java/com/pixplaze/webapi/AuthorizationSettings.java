package com.pixplaze.webapi;

import com.pixplaze.api.host.ServerLicense;
import com.pixplaze.plugin.PixplazeCorePlugin;
import com.pixplaze.plugin.PluginConfig;

import java.time.Duration;

/**
 * Settings of the mandatory sign-in (п. 33–34 of the authorization design).
 *
 * <p>{@code authorization.enabled}: on a licensed server the owner chooses (default off). On a non-licensed one
 * (including any server behind a proxy) it is always on, in memory and in the config: the nickname proves
 * nothing there. Written to the config it stays on even if the server later becomes licensed.
 *
 * @param enabled  players must sign in to Pixplaze to play
 * @param licensed the server checks accounts with Mojang itself
 * @param timeout   how long a player may stay unsigned before the kick; zero — no kick
 * @param autoLogin start the sign-in for an unsigned player on join, as {@code /pixplaze login} would
 */
public record AuthorizationSettings(boolean enabled, boolean licensed, Duration timeout, boolean autoLogin) {

    private static final String ENABLED = PluginConfig.AUTHORIZATION_ENABLED;
    private static final String TIMEOUT = PluginConfig.AUTHORIZATION_TIMEOUT_MINUTES;

    public static AuthorizationSettings load(PixplazeCorePlugin plugin) {
        final var config = plugin.getConfig();
        final var licensed = ServerLicense.isLicensed(plugin.getServer());
        var enabled = config.getBoolean(ENABLED, false);
        if (!licensed && !enabled) {
            enabled = true;
            config.set(ENABLED, true);
            plugin.saveConfig();
            plugin.getLogger().warning("The server does not check accounts with Mojang (offline mode or behind a proxy):"
                    + " " + ENABLED + " is turned on, players must sign in to Pixplaze");
        }
        final var minutes = Math.max(config.getLong(TIMEOUT, 5), 0);
        final var autoLogin = config.getBoolean(PluginConfig.AUTHORIZATION_AUTO_LOGIN, true);
        return new AuthorizationSettings(enabled, licensed, Duration.ofMinutes(minutes), autoLogin);
    }
}
