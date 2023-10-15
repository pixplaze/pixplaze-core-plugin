package com.pixplaze.api.controller;

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

@Deprecated(forRemoval = true)
public class ServerController implements ExchangeController<JavalinExchangeServer> {
    private final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();

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
    public void register(JavalinExchangeServer instance) {
        final var app = instance.provide();
        app.routes(() -> path("/server", () -> {
            get("/version", this::getServerVersion);
            get("/worlds", this::getServerWorlds);
            post("/stop", this::postServerStop);
            post("/broadcast", this::postServerBroadcast);
            put("/putin", this::getUniverseEmperor);
            delete("/delete", this::deleteServerLOL);
        }));
    }
}
