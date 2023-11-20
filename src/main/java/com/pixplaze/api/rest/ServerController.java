package com.pixplaze.api.rest;

import com.pixplaze.api.dao.ServerDAO;
import com.pixplaze.exchange.ExchangeController;
import com.pixplaze.exchange.JavalinExchangeServer;
import com.pixplaze.plugin.PixplazeCorePlugin;
import io.javalin.http.Context;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.*;

public class ServerController implements ExchangeController<JavalinExchangeServer> {
    private final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();
    private final ServerDAO serverDAO = new ServerDAO();

    public void getServer(Context context) {
        var view = Optional.ofNullable(context.queryParam("view")).orElse("short");

        switch (view) {
            case "short" -> {
                context.json(serverDAO.getServerExtendedShortInfo()).status(200);
            }
            case "status" -> {
                context.json(serverDAO.getServerStatusInfo()).status(200);
            }
            case "full" -> {
                context.json(serverDAO.getServerExtendedFullInfo()).status(200);
            }
            default -> {
                context.status(400);
            }
        }
    }

    public void getServerVersion(Context context) {
        context.result(
                plugin.getServer().getBukkitVersion()
        ).status(200);
    }

    public void getServerWorlds(Context context) {
        context.result("[%s]".formatted(plugin.getServer().getWorlds().stream()
                .map(World::getName)
                .collect(Collectors.joining(", ")))
        ).status(200);
    }

    public void postServerStop(Context context) {
        context.status(200).result("Server stopped");
        execute(plugin, "stop");

    }

    private void execute(JavaPlugin plugin, String command) {
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }.runTask(plugin);
    }

    public void postServerBroadcast(Context context) {
        Optional.ofNullable(context.queryParam("message"))
                .ifPresentOrElse(
                        message -> {
                            plugin.getServer().broadcastMessage(message);
                            context.result(message).status(200);
                        },
                        () -> context.status(400));

    }

    public void getUniverseEmperor(Context context) {
        context.result("pidor").status(200);
    }

    public void deleteServerLOL(Context context) {
        context.status(200);
    }

    @Override
    public void register(JavalinExchangeServer server) {
        final var app = server.provide();
        app.routes(() -> path("/server", () -> {
            get("", this::getServer);
        }));
    }
}
