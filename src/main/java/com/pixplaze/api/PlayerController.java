package com.pixplaze.api;

import com.pixplaze.api.server.annotations.GetHandler;
import com.pixplaze.api.server.annotations.RestController;
import com.pixplaze.plugin.PixplazeCorePlugin;
import io.javalin.http.Context;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@RestController("/path")
public class PlayerController {

    private final PixplazeCorePlugin plugin;

    public PlayerController() {
        this.plugin = PixplazeCorePlugin.getInstance();
    }

    @GetHandler("/player")
    public void getPlayerList(Context context) {
        var server = plugin.getServer();
        var status = Optional.ofNullable(context.queryParam("status"))
                .orElse("");

        var players = new HashSet<String>();

        switch (status) {
            case "online" -> players.addAll(server.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList());
            case "offline" -> players.addAll(Arrays.stream(server.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .toList());
            case "banned" -> players.addAll(server.getBannedPlayers().stream()
                    .map(OfflinePlayer::getName)
                    .toList());
            default -> players.addAll(getAllPlayers(server));
        }

        context.result(players.toString()).status(200);
    }

    private @NotNull Collection<String> getAllPlayers(Server server) {
        var players = new HashSet<String>();

        players.addAll(server.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toSet()));
        players.addAll(server.getBannedPlayers().stream()
                .map(OfflinePlayer::getName)
                .collect(Collectors.toSet()));
        players.addAll(Arrays.stream(server.getOfflinePlayers())
                .map(OfflinePlayer::getName)
                .collect(Collectors.toSet()));

        return players;
    }
}