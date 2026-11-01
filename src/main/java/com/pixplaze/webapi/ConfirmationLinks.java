package com.pixplaze.webapi;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

/// Clickable links to confirm a device authorization in Pixplaze (sign-in of a player, connection of the server).
final class ConfirmationLinks {

    private ConfirmationLinks() {
    }

    /// Main thread: "{prompt} (code …): [open Pixplaze]".
    static void send(Player player, String prompt, String url, String userCode) {
        final var link = new TextComponent("[open Pixplaze]");
        link.setColor(ChatColor.AQUA);
        link.setUnderlined(true);
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, clickable(url)));
        player.spigot().sendMessage(new ComponentBuilder(prompt + " (code " + userCode + "): ")
                .color(ChatColor.GRAY).append(link).create());
    }

    /// The client opens only http(s) links, and web-api may give the address without the scheme.
    static String clickable(String url) {
        return url.contains("://") ? url : "https://" + url;
    }
}
