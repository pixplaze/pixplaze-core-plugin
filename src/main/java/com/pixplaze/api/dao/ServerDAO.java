package com.pixplaze.api.dao;

import com.pixplaze.api.info.ServerShortInfo;
import com.pixplaze.api.info.ServerStatusInfo;
import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.Server;

public class ServerDAO {

    private static final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();
    private static final Server server = plugin.getServer();

    public static ServerShortInfo getServerShortInfo() {
        var name = server.getMotd();
        var maxPlayers = server.getMaxPlayers();
        var difficulty = server.getWorlds().get(0).getDifficulty().name();
        var mapAddress = "";
        return new ServerShortInfo(name, maxPlayers, difficulty, mapAddress);
    }

    public static ServerStatusInfo getServerStatusInfo() {
        var onlineStatus = "online";
        var players = server.getOnlinePlayers().size();
        var uptime = (long) 0;
        return new ServerStatusInfo(onlineStatus, players, uptime);
    }
}
