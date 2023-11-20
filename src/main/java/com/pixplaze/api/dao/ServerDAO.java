package com.pixplaze.api.dao;

import com.pixplaze.api.info.ServerInfo;
import com.pixplaze.api.info.ServerStatusInfo;
import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

public class ServerDAO {

    private final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();
    private final Server server = plugin.getServer();

    public ServerInfo getServerShortInfo() {
        var name = server.getMotd();
        var maxPlayers = server.getMaxPlayers();
        var difficulty = server.getWorlds().get(0).getDifficulty().name();
        var mapAddress = "";
        var coreName = server.getName();
        var coreVersion = server.getVersion();
        var plugins = Arrays.stream(server.getPluginManager().getPlugins()).map(Plugin::getName).toList();
        return new ServerInfo(name, maxPlayers, difficulty, mapAddress, coreName, coreVersion, plugins);
    }

    public ServerStatusInfo getServerStatusInfo() {
        var onlineStatus = "online";
        var players = server.getOnlinePlayers().size();
        var uptime = (long) 0;
        return new ServerStatusInfo(onlineStatus, players, uptime);
    }
}
