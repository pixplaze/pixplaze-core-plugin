package com.pixplaze.webapi;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/// Join and quit of players for {@link PlayerAuthorization} while `authorization.enabled` is off: tokens are
/// kept fresh, nobody is frozen. With the option on, {@link AuthorizationGate} handles joins instead.
public final class PlayerAuthorizationListener implements Listener {

    private final PlayerAuthorization authorization;

    public PlayerAuthorizationListener(PlayerAuthorization authorization) {
        this.authorization = authorization;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        authorization.onJoin(event.getPlayer(), false, false, check -> {});
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        authorization.onQuit(event.getPlayer());
    }
}
