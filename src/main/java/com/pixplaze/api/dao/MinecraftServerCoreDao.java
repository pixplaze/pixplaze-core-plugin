package com.pixplaze.api.dao;

import com.pixplaze.api.ext.data.server.MinecraftServerCoreInfo;
import org.bukkit.Bukkit;
import org.bukkit.Server;

public class MinecraftServerCoreDao {
    private final Server server = Bukkit.getServer();
    public MinecraftServerCoreInfo getMinecraftCoreInfo() {
        final var name = server.getName();
        final var version = server.getVersion();

        return new MinecraftServerCoreInfo(
                name,
                version
        );
    }
}
