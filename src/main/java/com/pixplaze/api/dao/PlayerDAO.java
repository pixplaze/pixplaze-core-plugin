package com.pixplaze.api.dao;

import com.pixplaze.api.info.PlayerInfo;
import com.pixplaze.api.info.PlayerListInfo;
import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.Server;
import org.bukkit.Statistic;
import org.checkerframework.checker.units.qual.A;

import java.util.*;

public class PlayerDAO {

    private final static PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();
    private static final Server server = plugin.getServer();

    public static PlayerInfo getPlayerInfo(UUID uuid) {
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

    public static PlayerListInfo getOnlinePlayers() {
        List<PlayerInfo> onlinePlayers = new ArrayList<>();
        server.getOnlinePlayers().forEach(x -> onlinePlayers.add(getPlayerInfo(x.getUniqueId())));
        return new PlayerListInfo(onlinePlayers);
    }
}
