package com.pixplaze.api.rest;

import com.pixplaze.api.dao.PlayerDAO;
import com.pixplaze.exchange.ExchangeController;
import com.pixplaze.exchange.JavalinExchangeServer;
import com.pixplaze.plugin.PixplazeCorePlugin;
import io.javalin.http.Context;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class PlayerController implements ExchangeController<JavalinExchangeServer> {
    private final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();
    private final Server server = plugin.getServer();

    public void getPlayer(Context context) {
        try {
            var uuid = UUID.fromString(context.pathParam("uuid"));
            context.result(PlayerDAO.getPlayerInfo(uuid).toString()).status(200);
        } catch (IllegalArgumentException e) {
            context.status(400);
        }
    }

    public void getPlayers(Context context) {
        var status = Optional.ofNullable(context.queryParam("status")).orElse("");

        switch (status) {
            case "online" -> context.result(PlayerDAO.getOnlinePlayers().toString()).status(200);
        }
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

    @Override
    public void register(JavalinExchangeServer server) {
        final var app = server.provide();
        app.routes(() -> path("/players", () -> {
            get("/<uuid>", this::getPlayer);
            get("", this::getPlayers);
        }));
    }
}