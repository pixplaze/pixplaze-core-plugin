package com.pixplaze.api.rest;

import com.pixplaze.api.dao.MinecraftServerDao;
import com.pixplaze.exchange.ExchangeController;
import com.pixplaze.exchange.JavalinExchangeServer;
import com.pixplaze.plugin.PixplazeCorePlugin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.router.JavalinDefaultRouting;
import org.apache.commons.lang.ObjectUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;
import java.util.stream.Collectors;

public class ServerController implements ExchangeController<JavalinExchangeServer> {
    private final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();
    private final MinecraftServerDao minecraftServerDao = new MinecraftServerDao();

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

    private void handleServerInfoRequest(Context context) {
        final var retrieveThumbnail = Boolean.parseBoolean(context.queryParam("thumbnail"));
        context.json(minecraftServerDao.getServerInfo(retrieveThumbnail)).status(HttpStatus.OK);
    }

    public void handleServerStateRequest(Context context) {
        context.json(minecraftServerDao.getServerState()).status(HttpStatus.OK);
    }

    private void handleServerVersionRequest(Context context) {
        context.json(minecraftServerDao.getServerVersion()).status(HttpStatus.OK);
    }

    private void handleServerCoreRequest(Context context) {
        context.json(minecraftServerDao.getServerCore()).status(HttpStatus.OK);
    }

    private void handleInstalledPluginsRequest(Context context) {
        context.json(minecraftServerDao.getInstalledPlugins()).status(HttpStatus.OK);
    }

    private void handleEnabledPluginsRequest(Context context) {
        context.json(minecraftServerDao.getEnabledPlugins()).status(HttpStatus.OK);
    }

    private void handlePlayersRequest(Context context) {
        final var viewMode = (String) ObjectUtils.defaultIfNull(context.queryParam("view"), "all");

        final var players = switch (viewMode) {
            case "online" -> minecraftServerDao.getOnlinePlayers();
            case "offline" -> minecraftServerDao.getOfflinePlayers();
            case "banned" -> minecraftServerDao.getBannedPlayers();
            case "whitelisted" -> minecraftServerDao.getWhitelistedPlayers();
            case "operators" -> minecraftServerDao.getOpPlayers();
            default -> minecraftServerDao.getAllPlayers();
        };

        context.json(players).status(HttpStatus.OK);
    }

    @Override
    public void register(JavalinDefaultRouting routing) {
        routing.get("/server", this::handleServerInfoRequest);
        routing.get("/server/state", this::handleServerStateRequest);
        routing.get("/server/plugins/installed", this::handleInstalledPluginsRequest);
        routing.get("/server/plugins/enabled", this::handleEnabledPluginsRequest);
        routing.get("/server/players", this::handlePlayersRequest);
    }
}
