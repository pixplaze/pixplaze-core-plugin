package com.pixplaze.api.rest;

import com.pixplaze.api.dao.MinecraftPlayerDao;
import com.pixplaze.exchange.ExchangeController;
import com.pixplaze.exchange.JavalinExchangeServer;
import com.pixplaze.plugin.PixplazeCorePlugin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.router.JavalinDefaultRouting;
import org.bukkit.Server;

import java.util.Optional;
import java.util.UUID;

public class PlayerController implements ExchangeController<JavalinExchangeServer> {
    private final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();
    private final MinecraftPlayerDao minecraftPlayerDao = new MinecraftPlayerDao();
    private final Server server = plugin.getServer();

    public void getPlayer(Context context) {
        try {
            var player = minecraftPlayerDao.getPlayerInfo(UUID.fromString(context.pathParam("uuid")));

            if (player.username() == null) {
                context.status(404);
                return;
            }
            context.json(player).status(200);
        } catch (IllegalArgumentException e) {
            context.status(400);
        }
    }

    public void getPlayers(Context context) {
        var status = Optional.ofNullable(context.queryParam("status"))
                .orElse("all");

        switch (status) {
            case "online" -> context.json(minecraftPlayerDao.getOnlinePlayers()).status(HttpStatus.OK);
            case "offline" -> context.json(minecraftPlayerDao.getOfflinePlayers()).status(HttpStatus.OK);
            case "banned" -> context.json(minecraftPlayerDao.getBannedPlayers()).status(HttpStatus.OK);
            case "whitelisted" -> context.json(minecraftPlayerDao.getWhitelistedPlayers()).status(HttpStatus.OK);
            case "all" -> context.json(minecraftPlayerDao.getAllPlayers()).status(HttpStatus.OK);
            default -> context.status(400);
        }
    }

    @Override
    public void register(JavalinDefaultRouting routing) {
        routing.get("/players/{uuid}", this::getPlayer);
        routing.get("/players", this::getPlayers);
    }
}