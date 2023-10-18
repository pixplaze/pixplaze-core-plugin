package com.pixplaze.api.dao;

import com.pixplaze.api.info.ServerShortInfo;
import com.pixplaze.plugin.PixplazeCorePlugin;

public class ServerDAO {

    private static PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();

    public static ServerShortInfo getServerShortInfo() {
        var server = plugin.getServer();
        var name = server.getMotd();
        var maxPlayers = server.getMaxPlayers();
        var difficulty = server.getWorlds().get(0).getDifficulty().name();
        var mapAddress = "";
        return new ServerShortInfo(name, maxPlayers, difficulty, mapAddress);
    }
}
