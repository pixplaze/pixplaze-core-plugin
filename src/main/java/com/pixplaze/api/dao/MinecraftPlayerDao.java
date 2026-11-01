package com.pixplaze.api.dao;

import com.pixplaze.api.ext.data.player.MinecraftPlayerInfo;
import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Statistic;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

        return MinecraftPlayerInfo.builder()
                .uuid(uuid)
                .username(username)
                .isOnline(online)
                .isWhitelisted(whitelisted)
                .isBanned(banned)
                .isOperator(operator)
                .playtime(playtime)
                .build();
    }

    public List<MinecraftPlayerInfo> getOnlinePlayers() {
        return server.getOnlinePlayers().stream()
                .filter(MinecraftPlayerDao::isNamed)
                .map(this::getPlayerInfo)
                .toList();
    }

    /// Ники игроков в сети без учёта регистра по алфавиту — без статистики и проверок, как у
    /// [#getOnlinePlayers]. Читает Bukkit API: вызывать из главного потока.
    public List<String> getOnlinePlayerNames() {
        return server.getOnlinePlayers().stream()
                .map(OfflinePlayer::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<MinecraftPlayerInfo> getOfflinePlayers() {
        return Arrays.stream(server.getOfflinePlayers())
                .filter(MinecraftPlayerDao::isNamed)
                .map(this::getPlayerInfo)
                .filter(this::isPlayerOffline)
                .toList();
    }

    public List<MinecraftPlayerInfo> getBannedPlayers() {
        return server.getBannedPlayers().stream()
                .filter(MinecraftPlayerDao::isNamed)
                .map(this::getPlayerInfo)
                .toList();
    }

    public List<MinecraftPlayerInfo> getWhitelistedPlayers() {
        return server.getWhitelistedPlayers().stream()
                .filter(MinecraftPlayerDao::isNamed)
                .map(this::getPlayerInfo)
                .toList();
    }

    public List<MinecraftPlayerInfo> getOpPlayers() {
        return server.getOperators().stream()
                .filter(MinecraftPlayerDao::isNamed)
                .map(this::getPlayerInfo)
                .toList();
    }

    public List<MinecraftPlayerInfo> getAllPlayers() {
        return Arrays.stream(server.getOfflinePlayers())
                .filter(MinecraftPlayerDao::isNamed)
                .map(this::getPlayerInfo)
                .toList();
    }

//    public Property getTexturesProperty() {
//        String texture = "ewogICJ0aW1lc3RhbXAiIDogMTc0Nzc4NTc4MjI3NSwKICAicHJvZmlsZUlkIiA6ICJjMDFmM2QwYTU4Y2E0YWVhYTFiOTVjZjhmMTcyYjY2NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJFbWJlcmF0aSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lNWFhMjA0ODRkMTM2ZTJhZGVkZDUxMjk1ZGJiN2JmNDAwNWMwY2VmYWY0OGIxOTVkMjZmNzczMDhjNDEwNDA3IgogICAgfSwKICAgICJDQVBFIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jZDlkODJhYjE3ZmQ5MjAyMmRiZDRhODZjZGU0YzM4MmE3NTQwZTExN2ZhZTdiOWEyODUzNjU4NTA1YTgwNjI1IgogICAgfQogIH0KfQ==";
//        String signature = "nojeWCkdmZnpLbLBAoU11p00YnMHf2W5wsv6jhavKcAmk7LHqXhYqsWEuzx9pGqmDpmUk57fl3gCm6FOgofWfp qrgaC5TLWYDTvabHKuJM2TE SjGIotvt2w1zbSplTgC3XDynMJpBIn2rVsFN6T11L70n4KH6iT5n1LNiciE5L2wqp7BEkVqORFEdialCa1gPQZx/FCuz1vGvrLiApzurX1HmNNSEPsvZQNtoTbMww99gbN9/ZlVOI0G7tiiUOhbWZ5rU07iZUZ6PYrumipE7Fwfty kHgKUyK726GTrg4XdR53GSPw1Ps9ZIvLgGRDKsDKCxorCRPHAM6tECWfaUKIxT2YRvKOdEiLe6Hh0Cd1YDBTestg4/1OS7C CXIJEVa1cuNEf2J40mTmGsYeBl2FLe3rCzl43 En4vgFPdhLnH4bKruPoTrhFot9AD9OU9e PLadwwycJezkPeb3YR/GJOTIJ63t3 0fENaorZ9qXlOHYlAWJbhRwru tZpN03fgVjr vRSzVwsupAo zjvGYDdPnlJb7nEI3pYe MEPg73qY3bFyTVbg7dj5C6981 1P0AUTDO7 1rSdwRC6Ij/iHV3mRjhlGBYq1KMHDuekuzZQK6qPJE5Im3OjeduphF3 85SvwONlBqDcLb3Me/pyl9DXf1lh/rLuvnTFc=";
//        return new Property("textures", texture, signature);
//    }
//
//    public void setSkin(Player player, Property property) {
//
//    }
//
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
//
//    public void reloadPlayerSkin0(ServerPlayer serverPlayer) {
//        final var serverPlayerConnection = serverPlayer.connection;
//        final var serverPlayerGameProfile = serverPlayer.getGameProfile();
//        final var serverPlayerProperties = serverPlayerGameProfile.getProperties();
//
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                serverPlayerConnection.send(new ClientboundPlayerInfoRemovePacket(List.of(serverPlayer.getUUID())));
//
//                serverPlayerProperties.removeAll("textures");
//                serverPlayerProperties.put("textures", getTexturesProperty());
//
//                serverPlayerConnection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(serverPlayer)));
//            }
//        }.runTaskLater(PixplazeCorePlugin.getInstance(), 5);
//    }

//    public void reloadPlayerSkin1(ServerPlayer serverPlayer) {
//        final var serverPlayerConnection = serverPlayer.connection;
//        final var serverPlayerGameProfile = serverPlayer.getGameProfile();
//        final var serverPlayerProperties = serverPlayerGameProfile.getProperties();
//
//        serverPlayerProperties.removeAll("textures");
//        serverPlayerProperties.put("textures", getTexturesProperty());
//
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                try (final var level = serverPlayer.serverLevel()) {
//                    final var spawnInfo = serverPlayer.createCommonSpawnInfo(level);
//                    serverPlayerConnection.send(new ClientboundRespawnPacket(spawnInfo, ClientboundRespawnPacket.KEEP_ATTRIBUTE_MODIFIERS));
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }.runTaskLater(PixplazeCorePlugin.getInstance(), 5);
//    }
//
//    public void reloadPlayerSkin2(ServerPlayer serverPlayer) {
//        final var serverPlayerConnection = serverPlayer.connection;
//        final var serverPlayerGameProfile = serverPlayer.getGameProfile();
//        final var serverPlayerProperties = serverPlayerGameProfile.getProperties();
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                serverPlayerConnection.send(new ClientboundPlayerInfoRemovePacket(List.of(serverPlayer.getUUID())));
//
//                serverPlayerProperties.removeAll("textures");
//                serverPlayerProperties.put("textures", getTexturesProperty());
//
//                serverPlayerConnection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(serverPlayer)));
//            }
//        }.runTaskLater(PixplazeCorePlugin.getInstance(), 5);
//    }

    public long getPlayerPlaytime(OfflinePlayer player) {
        return (long) player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
    }

    private boolean isPlayerOffline(MinecraftPlayerInfo p) {
        return !p.isOnline();
    }
    /// Bukkit knows no nickname of a player it has no fresh name cache or player data for (an expired
    /// usercache.json entry, an op who never joined): such a player cannot be shown or stored by name.
    private static boolean isNamed(OfflinePlayer player) {
        return player.getName() != null && !player.getName().isBlank();
    }
}
