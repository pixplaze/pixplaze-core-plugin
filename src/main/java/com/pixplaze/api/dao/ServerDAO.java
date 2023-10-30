package com.pixplaze.api.dao;

import com.pixplaze.api.info.ServerShortInfo;
import com.pixplaze.api.info.ServerStatusInfo;
import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.Server;

public class ServerDAO {

    private final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();
    private final Server server = plugin.getServer();

    public ServerShortInfo getServerShortInfo() {
        var name = server.getMotd();
        var maxPlayers = server.getMaxPlayers();
        var difficulty = server.getWorlds().get(0).getDifficulty().name();
        var mapAddress = "";
        var coreName = server.getName();
        var coreVersion = server.getBukkitVersion();
        return new ServerShortInfo(name, maxPlayers, difficulty, mapAddress, coreName, coreVersion);
    }

    public ServerStatusInfo getServerStatusInfo() {
        var onlineStatus = "online";
        var players = server.getOnlinePlayers().size();
        var uptime = (long) 0;
        return new ServerStatusInfo(onlineStatus, players, uptime);
    }
}
