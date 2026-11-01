package com.pixplaze.auth;

import com.pixplaze.api.ext.data.auth.Authority;
import org.bukkit.Bukkit;

/**
 * Which token may do what in the plugin: the role from the token and the live state of the server
 * (п. 21, 28, 31 of the authorization design). Game rights are checked on {@link org.bukkit.OfflinePlayer}:
 * {@code hasPermission} exists only for online players.
 */
public final class PluginAccess {

    private PluginAccess() {
    }

    /// `/server`, `/server/state`, `/server/plugins/*`: a server member, or web-api itself.
    public static boolean canReadServer(VerifiedToken token) {
        return token.isSystem() || (token.isUser() && token.authority().is(Authority.Role.MINECRAFT_PLAYER));
    }

    /// `/players`, `/players/{uuid}`, `/server/players` (IP, bans, whitelist): an operator, or web-api itself.
    public static boolean canReadPlayers(VerifiedToken token) {
        return token.isSystem() || isOperator(token);
    }

    /// WebSocket `/console`: an operator by token and on the server right now.
    public static boolean canUseConsole(VerifiedToken token) {
        return isOperator(token);
    }

    /// WebSocket `/chat`: a member not banned and, with the whitelist on, in it.
    public static boolean canUseChat(VerifiedToken token) {
        if (!token.isUser() || !token.authority().is(Authority.Role.MINECRAFT_PLAYER)) {
            return false;
        }
        return token.playerUuid()
                .map(Bukkit::getOfflinePlayer)
                .map(player -> !player.isBanned() && (!Bukkit.hasWhitelist() || player.isWhitelisted()))
                .orElse(false);
    }

    private static boolean isOperator(VerifiedToken token) {
        return token.isUser()
                && token.authority().is(Authority.Role.MINECRAFT_OPERATOR)
                && token.playerUuid().map(uuid -> Bukkit.getOfflinePlayer(uuid).isOp()).orElse(false);
    }
}
