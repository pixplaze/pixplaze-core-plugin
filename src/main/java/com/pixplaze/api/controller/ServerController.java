package com.pixplaze.api.controller;

import com.pixplaze.exchange.annotations.*;
import com.pixplaze.plugin.PixplazeCorePlugin;
import io.javalin.http.Context;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Optional;
import java.util.stream.Collectors;

@Deprecated(forRemoval = true)
@RestController("/server")
public class ServerController {
    private final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();

    @GetHandler("/version")
    public void getServerVersion(Context context) {
        context.result(
                plugin.getServer().getBukkitVersion()
        ).status(200);
    }

    @GetHandler("/worlds")
    public void getServerWorlds(Context context) {
        context.result("[%s]".formatted(plugin.getServer().getWorlds().stream()
                .map(World::getName)
                .collect(Collectors.joining(", ")))
        ).status(200);
    }

    @PostHandler("/stop")
    public void postServerStop(Context context) {
        plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), "stop");
        context.status(200);
    }

    @PostHandler("/broadcast")
    public void postServerBroadcast(Context context) {
        Optional.ofNullable(context.queryParam("message"))
                .ifPresentOrElse(
                        message -> {
                            plugin.getServer().broadcastMessage(message);
                            context.result(message).status(200);
                        },
                        () -> context.status(400));

    }

    @PutHandler("/putin")
    public void getUniverseEmperor(Context context) {
        context.result("pidor").status(200);
    }

    @DeleteHandler("/delete")
    public void deleteServerLOL(Context context) {
        context.status(200);
    }
}
