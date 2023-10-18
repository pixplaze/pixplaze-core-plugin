package com.pixplaze.api.dao;

import com.pixplaze.api.info.PlayerInfo;
import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.Statistic;

import java.util.Arrays;
import java.util.UUID;

public class PlayerDAO {

    private final static PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();

    public static PlayerInfo getPlayerInfo(UUID uuid) {
        var server = plugin.getServer();

        var player = server.getPlayer(uuid);
        if (player != null) {
            var username = player.getName();
            var status = "online";
            var playtime = (long) player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
            return new PlayerInfo(uuid, username, status, playtime);
        }

        var offlinePlayer = server.getOfflinePlayer(uuid);
        var username = offlinePlayer.getName();
        var status = "offline";
        var playtime = (long) offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
        return new PlayerInfo(uuid, username, status, playtime);
    }
}
