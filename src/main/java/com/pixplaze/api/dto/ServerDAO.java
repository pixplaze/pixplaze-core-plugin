package com.pixplaze.api.dto;

import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.Bukkit;

import java.util.Optional;

public class ServerDAO {

    private static PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();

    public static ServerShortDTO getServerShortInfo() {
        var server = plugin.getServer();
        var name = server.getMotd();
        var maxPlayers = server.getMaxPlayers();
        var difficulty = server.getWorlds().get(0).getDifficulty().name();
        var mapAddress = "";
        return new ServerShortDTO(name, maxPlayers, difficulty, mapAddress);
    }
}
