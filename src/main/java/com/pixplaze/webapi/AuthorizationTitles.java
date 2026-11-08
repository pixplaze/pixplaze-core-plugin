package com.pixplaze.webapi;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

final class AuthorizationTitles {
    public static void sendAuthorized(Player player) {
        final var title = ChatColor.GREEN + "Authorized";
        final var subtitle = ChatColor.GREEN + "have a good game!";
        player.sendTitle(title, subtitle, 10, 30, 10);
    }

    public static void sendUnauthorized(Player player) {
        final var title = ChatColor.RED + "Unauthorized";
        final var subtitle = ChatColor.YELLOW + "follow the instructions in the chat.";
        player.sendTitle(title, subtitle, 0, 30, 0);
    }

    public static void sendUnavailable(Player player) {
        final var title = ChatColor.RED + "Unavailable";
        final var subtitle = ChatColor.YELLOW + "you cannot play until the sign-in is confirmed";
        player.sendTitle(title, subtitle, 0, 30, 0);
    }
}
