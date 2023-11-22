package com.pixplaze.api.dao;

import com.pixplaze.api.info.ExtendedServerInfo;
import com.pixplaze.api.info.ServerInfo;
import com.pixplaze.api.info.ServerStatusInfo;
import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import java.util.ArrayList;
import java.util.Arrays;

public class ServerDAO {

    private final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();
    private final Server server = plugin.getServer();

    public ServerInfo getServerShortInfo() {
        var address = server.getIp();
        var primary = true;
        var name = server.getMotd();
        var thumbnail = "0JHQu9GP0KXQvtGH0YPQmtCw0LrQsNGC0YzQltC+0YHQutCwKSkp";
        var maxPlayers = server.getMaxPlayers();
        var difficulty = server.getWorlds().get(0).getDifficulty().name();
        var plugins = Arrays.stream(server.getPluginManager().getPlugins()).map(Plugin::getName).toList();
        return new ServerInfo(
                address,
                null,
                primary,
                name,
                thumbnail,
                null,
                null,
                null,
                null,
                maxPlayers,
                difficulty,
                plugins
        );
    }

    /**
     * Временный метод для замены данных с бэкенда.
     * Является обёрткой для ServerDAO#getServerShortInfo().
     */
    public ExtendedServerInfo getServerExtendedShortInfo() {
        var name = "ServerName";
        var tags = Arrays.asList("some", "tags");
        var rating = 5;
        var description = "The best server!";
        var server = getServerShortInfo();
        return new ExtendedServerInfo(name, tags, rating, description, server);
    }

    public ServerInfo getServerFullInfo() {
        var address = server.getIp();
        var apiPort = 25566;
        var primary = true;
        var name = server.getMotd();
        var thumbnail = "0JHQu9GP0KXQvtGH0YPQmtCw0LrQsNGC0YzQltC+0YHQutCwKSkp";
        var coreName = server.getName();
        var coreVersion = server.getVersion();
        var minecraftVersion = server.getBukkitVersion();
        var mapPort = 25567;
        var maxPlayers = server.getMaxPlayers();
        var difficulty = server.getWorlds().get(0).getDifficulty().name();
        var plugins = Arrays.stream(server.getPluginManager().getPlugins()).map(Plugin::getName).toList();
        return new ServerInfo(
                address,
                apiPort,
                primary,
                name,
                thumbnail,
                coreName,
                coreVersion,
                minecraftVersion,
                mapPort,
                maxPlayers,
                difficulty,
                plugins
        );
    }

    /**
     * Временный метод для замены данных с бэкенда.
     * Является обёрткой для ServerDAO#getServerFullInfo().
     */
    public ExtendedServerInfo getServerExtendedFullInfo() {
        var name = "ServerName";
        var tags = Arrays.asList("some", "tags");
        var rating = 5;
        var description = "The best server!";
        var server = getServerFullInfo();
        return new ExtendedServerInfo(name, tags, rating, description, server);
    }

    public ServerStatusInfo getServerStatusInfo() {
        var onlineStatus = "online";
        var players = server.getOnlinePlayers().size();
        var uptime = (long) 0;
        return new ServerStatusInfo(onlineStatus, players, uptime);
    }
}
