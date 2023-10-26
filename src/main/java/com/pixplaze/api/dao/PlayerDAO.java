package com.pixplaze.api.dao;

import com.pixplaze.api.info.PlayerInfo;
import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.Server;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerDAO {

    private final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();
    private final Server server = plugin.getServer();

    public PlayerInfo getPlayerInfo(UUID uuid) {
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

    public PlayerInfo getPlayerInfo(Player player) {
        return getPlayerInfo(player.getUniqueId());
    }

    public List<PlayerInfo> getOnlinePlayers() {
        return server.getOnlinePlayers().stream()
                .map(this::getPlayerInfo)
                .collect(Collectors.toList());
    }
}
