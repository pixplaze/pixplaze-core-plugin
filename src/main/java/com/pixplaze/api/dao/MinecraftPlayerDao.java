package com.pixplaze.api.dao;

import com.mojang.authlib.properties.Property;
import com.pixplaze.api.ext.data.player.MinecraftPlayerInfo;
import com.pixplaze.plugin.PixplazeCorePlugin;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Statistic;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MinecraftPlayerDao {

    private final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();
    private final Server server = plugin.getServer();

    public MinecraftPlayerInfo getPlayerInfo(@Nonnull UUID uuid) {
        return getPlayerInfo(Objects.requireNonNull(server.getPlayer(uuid)));
    }

    private MinecraftPlayerInfo getPlayerInfo(OfflinePlayer player) {
        final var uuid = player.getUniqueId();
        final var username = player.getName();
        final var online = player.isOnline();
        final var whitelisted = player.isWhitelisted();
        final var banned = player.isBanned();
        final var operator = player.isOp();
        final var playtime = getPlayerPlaytime(player);

        return new MinecraftPlayerInfo(
                uuid,
                username,
                online,
                whitelisted,
                banned,
                operator,
                playtime
        );
    }

    public Set<MinecraftPlayerInfo> getOnlinePlayers() {
        return server.getOnlinePlayers().stream()
                .map(this::getPlayerInfo)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<MinecraftPlayerInfo> getOfflinePlayers() {
        return Arrays.stream(server.getOfflinePlayers())
                .map(this::getPlayerInfo)
                .filter(this::isPlayerOffline)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<MinecraftPlayerInfo> getBannedPlayers() {
        return server.getBannedPlayers().stream()
                .map(this::getPlayerInfo)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<MinecraftPlayerInfo> getWhitelistedPlayers() {
        return server.getWhitelistedPlayers().stream()
                .map(this::getPlayerInfo)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<MinecraftPlayerInfo> getOpPlayers() {
        return server.getOperators().stream()
                .map(this::getPlayerInfo)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<MinecraftPlayerInfo> getAllPlayers() {
        return Arrays.stream(server.getOfflinePlayers())
                .map(this::getPlayerInfo)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Property getTexturesProperty() {
        String texture = "ewogICJ0aW1lc3RhbXAiIDogMTc0Nzc4NTc4MjI3NSwKICAicHJvZmlsZUlkIiA6ICJjMDFmM2QwYTU4Y2E0YWVhYTFiOTVjZjhmMTcyYjY2NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJFbWJlcmF0aSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lNWFhMjA0ODRkMTM2ZTJhZGVkZDUxMjk1ZGJiN2JmNDAwNWMwY2VmYWY0OGIxOTVkMjZmNzczMDhjNDEwNDA3IgogICAgfSwKICAgICJDQVBFIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jZDlkODJhYjE3ZmQ5MjAyMmRiZDRhODZjZGU0YzM4MmE3NTQwZTExN2ZhZTdiOWEyODUzNjU4NTA1YTgwNjI1IgogICAgfQogIH0KfQ==";
        String signature = "nojeWCkdmZnpLbLBAoU11p00YnMHf2W5wsv6jhavKcAmk7LHqXhYqsWEuzx9pGqmDpmUk57fl3gCm6FOgofWfp qrgaC5TLWYDTvabHKuJM2TE SjGIotvt2w1zbSplTgC3XDynMJpBIn2rVsFN6T11L70n4KH6iT5n1LNiciE5L2wqp7BEkVqORFEdialCa1gPQZx/FCuz1vGvrLiApzurX1HmNNSEPsvZQNtoTbMww99gbN9/ZlVOI0G7tiiUOhbWZ5rU07iZUZ6PYrumipE7Fwfty kHgKUyK726GTrg4XdR53GSPw1Ps9ZIvLgGRDKsDKCxorCRPHAM6tECWfaUKIxT2YRvKOdEiLe6Hh0Cd1YDBTestg4/1OS7C CXIJEVa1cuNEf2J40mTmGsYeBl2FLe3rCzl43 En4vgFPdhLnH4bKruPoTrhFot9AD9OU9e PLadwwycJezkPeb3YR/GJOTIJ63t3 0fENaorZ9qXlOHYlAWJbhRwru tZpN03fgVjr vRSzVwsupAo zjvGYDdPnlJb7nEI3pYe MEPg73qY3bFyTVbg7dj5C6981 1P0AUTDO7 1rSdwRC6Ij/iHV3mRjhlGBYq1KMHDuekuzZQK6qPJE5Im3OjeduphF3 85SvwONlBqDcLb3Me/pyl9DXf1lh/rLuvnTFc=";
        return new Property("textures", texture, signature);
    }

//    public void setSkin(Player player, Property property) {
//
//    }

//    public void reloadPlayerSkin(Player player) {
//        final var properties = player.getGameProfile().getProperties();
//        MinecraftServer.getServer().getLevel().getEntity
//        connection.send(new ClientboundAddEntityPacket(player.getEntityData(), MinecraftServer.getServer().getPlayerList().getPlayerByName("Emberati").getBukkitEntity()));
//
//        properties.removeAll("textures");
//        properties.put("textures", getTexturesProperty());
//
//        connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER));
//    }

    public void reloadPlayerSkin0(ServerPlayer serverPlayer) {
        final var serverPlayerConnection = serverPlayer.connection;
        final var serverPlayerGameProfile = serverPlayer.getGameProfile();
        final var serverPlayerProperties = serverPlayerGameProfile.getProperties();

        new BukkitRunnable() {
            @Override
            public void run() {
                serverPlayerConnection.send(new ClientboundPlayerInfoRemovePacket(List.of(serverPlayer.getUUID())));

                serverPlayerProperties.removeAll("textures");
                serverPlayerProperties.put("textures", getTexturesProperty());

                serverPlayerConnection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(serverPlayer)));
            }
        }.runTaskLater(PixplazeCorePlugin.getInstance(), 5);
    }

    public void reloadPlayerSkin1(ServerPlayer serverPlayer) {
        final var serverPlayerConnection = serverPlayer.connection;
        final var serverPlayerGameProfile = serverPlayer.getGameProfile();
        final var serverPlayerProperties = serverPlayerGameProfile.getProperties();

        serverPlayerProperties.removeAll("textures");
        serverPlayerProperties.put("textures", getTexturesProperty());

        new BukkitRunnable() {
            @Override
            public void run() {
                try (final var level = serverPlayer.serverLevel()) {
                    final var spawnInfo = serverPlayer.createCommonSpawnInfo(level);
                    serverPlayerConnection.send(new ClientboundRespawnPacket(spawnInfo, ClientboundRespawnPacket.KEEP_ATTRIBUTE_MODIFIERS));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.runTaskLater(PixplazeCorePlugin.getInstance(), 5);
    }

    public void reloadPlayerSkin2(ServerPlayer serverPlayer) {
        final var serverPlayerConnection = serverPlayer.connection;
        final var serverPlayerGameProfile = serverPlayer.getGameProfile();
        final var serverPlayerProperties = serverPlayerGameProfile.getProperties();
        new BukkitRunnable() {
            @Override
            public void run() {
                serverPlayerConnection.send(new ClientboundPlayerInfoRemovePacket(List.of(serverPlayer.getUUID())));

                serverPlayerProperties.removeAll("textures");
                serverPlayerProperties.put("textures", getTexturesProperty());

                serverPlayerConnection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(serverPlayer)));
            }
        }.runTaskLater(PixplazeCorePlugin.getInstance(), 5);
    }

    public long getPlayerPlaytime(OfflinePlayer player) {
        return (long) player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
    }

    private boolean isPlayerOffline(MinecraftPlayerInfo p) {
        return !p.online();
    }
}
