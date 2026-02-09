package com.pixplaze.api.dao;

import com.pixplaze.api.dao.plugin.MinecraftPluginDao;
import com.pixplaze.api.exception.PixplazeApiException;
import com.pixplaze.api.ext.MinecraftServerApi;
import com.pixplaze.api.ext.data.player.MinecraftPlayerInfo;
import com.pixplaze.api.ext.data.plugin.MinecraftPluginInfo;
import com.pixplaze.api.ext.data.server.MinecraftServerCoreInfo;
import com.pixplaze.api.ext.data.server.MinecraftServerInfo;
import com.pixplaze.api.ext.data.server.MinecraftServerStateInfo;
import com.pixplaze.api.nms.MinecraftServer;
import com.pixplaze.api.reflection.Reflection;
import com.pixplaze.plugin.PixplazeCorePlugin;
import com.pixplaze.util.ServerFiles;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class MinecraftServerDao implements MinecraftServerApi {

    public static final String SERVER_ICON_NAME = "server-icon.png";

    private final Logger logger = PixplazeCorePlugin.getInstance().getLogger();
    private final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();
    private final Server server = plugin.getServer();
    private final Reflection minecraftServer = Reflection.provide(MinecraftServer::produce);
    private final MinecraftServerCoreDao minecraftServerCoreDao = new MinecraftServerCoreDao();
    private final MinecraftPlayerDao minecraftPlayerDao = new MinecraftPlayerDao();
    private final MinecraftPluginDao minecraftPluginDao = new MinecraftPluginDao();

    @Override
    public MinecraftServerInfo getServerInfo(boolean retrieveThumbnail) {
        final var address = getServerAddress();
        final var port = server.getPort();
        final var name = server.getMotd();
        final var license = server.getOnlineMode();
        final var difficulty = getServerDifficulty();
        final var hardcore = server.isHardcore();
        final var whitelist = server.hasWhitelist();
        final var version = getServerVersion();
        final var maxPlayers = server.getMaxPlayers();
        final var thumbnail = getServerThumbnail(retrieveThumbnail);
        final var core = minecraftServerCoreDao.getMinecraftCoreInfo();
        final var state = getServerState();
        final var plugins = getEnabledPluginNames();

//        testReflection();

        return new MinecraftServerInfo(
                address,
                port,
                name,
                license,
                difficulty,
                hardcore,
                whitelist,
                version,
                maxPlayers,
                thumbnail,
                core,
                state,
                plugins
        );
    }

    private void testNms() {
        final var serverPlayer = net.minecraft.server.MinecraftServer.getServer().getPlayerList().getPlayerByName("Emberati");
    }

    private void testReflection() {
        try {
            final var minecraftServer = Reflection.provide(MinecraftServer::produce);
            final var playerList = Reflection.wrap(minecraftServer.call("getPlayerList"));
            final var player = Reflection.wrap(playerList.call("getPlayerByName", "Emberati"));
            final var profile = Reflection.wrap(player.call("getGameProfile"));
            final var properties = Reflection.wrap(profile.call("getProperties"));
            final var propertiesMap = Reflection.wrap(properties.debug().methods().named("get").get(0).invoke(properties.getReflectedObject(), "textures"));
            final var property = Reflection.provide(
                    "com.mojang.authlib.properties.Property",
                    "textures",
                    "ewogICJ0aW1lc3RhbXAiIDogMTc0Nzc4NTc4MjI3NSwKICAicHJvZmlsZUlkIiA6ICJjMDFmM2QwYTU4Y2E0YWVhYTFiOTVjZjhmMTcyYjY2NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJFbWJlcmF0aSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lNWFhMjA0ODRkMTM2ZTJhZGVkZDUxMjk1ZGJiN2JmNDAwNWMwY2VmYWY0OGIxOTVkMjZmNzczMDhjNDEwNDA3IgogICAgfSwKICAgICJDQVBFIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jZDlkODJhYjE3ZmQ5MjAyMmRiZDRhODZjZGU0YzM4MmE3NTQwZTExN2ZhZTdiOWEyODUzNjU4NTA1YTgwNjI1IgogICAgfQogIH0KfQ==",
                    "nojeWCkdmZnpLbLBAoU11p00YnMHf2W5wsv6jhavKcAmk7LHqXhYqsWEuzx9pGqmDpmUk57fl3gCm6FOgofWfp qrgaC5TLWYDTvabHKuJM2TE SjGIotvt2w1zbSplTgC3XDynMJpBIn2rVsFN6T11L70n4KH6iT5n1LNiciE5L2wqp7BEkVqORFEdialCa1gPQZx/FCuz1vGvrLiApzurX1HmNNSEPsvZQNtoTbMww99gbN9/ZlVOI0G7tiiUOhbWZ5rU07iZUZ6PYrumipE7Fwfty kHgKUyK726GTrg4XdR53GSPw1Ps9ZIvLgGRDKsDKCxorCRPHAM6tECWfaUKIxT2YRvKOdEiLe6Hh0Cd1YDBTestg4/1OS7C CXIJEVa1cuNEf2J40mTmGsYeBl2FLe3rCzl43 En4vgFPdhLnH4bKruPoTrhFot9AD9OU9e PLadwwycJezkPeb3YR/GJOTIJ63t3 0fENaorZ9qXlOHYlAWJbhRwru tZpN03fgVjr vRSzVwsupAo zjvGYDdPnlJb7nEI3pYe MEPg73qY3bFyTVbg7dj5C6981 1P0AUTDO7 1rSdwRC6Ij/iHV3mRjhlGBYq1KMHDuekuzZQK6qPJE5Im3OjeduphF3 85SvwONlBqDcLb3Me/pyl9DXf1lh/rLuvnTFc="
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MinecraftServerStateInfo getServerState() {
        final var online = getCurrentOnline();
        final var tps = getCurrentTps();
        final var uptime = getCurrentUptime();
        final var enabled = true;

        return new MinecraftServerStateInfo(
                online,
                tps,
                uptime,
                enabled
        );
    }

    @Override
    public List<MinecraftPluginInfo> getInstalledPlugins() {
        return minecraftPluginDao.getInstalledPlugins();
    }

    @Override
    public List<MinecraftPluginInfo> getEnabledPlugins() {
        return minecraftPluginDao.getEnabledPlugins();
    }

    @Override
    public Set<MinecraftPlayerInfo> getOnlinePlayers() {
        return minecraftPlayerDao.getOnlinePlayers();
    }

    @Override
    public Set<MinecraftPlayerInfo> getOfflinePlayers() {
        return minecraftPlayerDao.getOfflinePlayers();
    }

    @Override
    public Set<MinecraftPlayerInfo> getBannedPlayers() {
        return minecraftPlayerDao.getBannedPlayers();
    }

    @Override
    public Set<MinecraftPlayerInfo> getWhitelistedPlayers() {
        return minecraftPlayerDao.getWhitelistedPlayers();
    }

    @Override
    public Set<MinecraftPlayerInfo> getOpPlayers() {
        return minecraftPlayerDao.getOpPlayers();
    }

    @Override
    public Set<MinecraftPlayerInfo> getAllPlayers() {
        return minecraftPlayerDao.getAllPlayers();
    }

    public Integer getCurrentOnline() {
        return server.getOnlinePlayers().size();
    }

    public Double getCurrentTps() {
        int tps = minecraftServer.get("TPS");
        float currentSmoothedTickTime = minecraftServer.call("getCurrentSmoothedTickTime");
        float currentTps = Duration.ofSeconds(1).toMillis() / currentSmoothedTickTime;

        if (currentTps > 20) {
            currentTps = tps;
        }

        return (double) Math.round(currentTps * 100.0) / 100.0;
    }

    public Long getCurrentUptime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    private Path getServerIconPath() {
        return Paths.get(ServerFiles.getServerDirectoryPath().toString(), SERVER_ICON_NAME);
    }

    /**
     * Loads server icon image in Base64 format
     * @return server icon image in Base64 string
     */
    private @Nullable String getServerIconBase64() {
        final var serverIconPath = getServerIconPath();
        try {
            return ServerFiles.imageToBase64(serverIconPath.toFile(), "png");
        } catch (NullPointerException | FileNotFoundException e) {
            logger.log(Level.SEVERE, "Could not load server icon file from %s, cause %s".formatted(serverIconPath, e.getMessage()));
            return null;
        } catch (IOException e) {
            throw new PixplazeApiException("Could not load server icon file from %s".formatted(serverIconPath));
        }
    }

    public @Nullable String getServerThumbnail(boolean retrieveThumbnail) {
        return retrieveThumbnail ? getServerIconBase64() : null;
    }

    /**
     * Retrieves server difficulty.
     * WARNING: IT WILL CORRECTLY WORK ONLY FOR SERVERS WITH ONE
     * PRIMARY WORLD, MULTI-WORLD SERVER DIFFICULTY IS NOT SUPPORTED
     * @return server primary difficulty
     */
    private String getServerDifficulty() {
        return server.getWorlds().get(0).getDifficulty().name();
    }

    private String getServerAddress() {
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public String getServerVersion() {
        final var coreVersion = server.getVersion();
        final var pattern = Pattern.compile("\\(MC: ([\\d.]+)\\)");
        final var matcher = pattern.matcher(coreVersion);
        final var found = matcher.find();

        if (!found) {
            return coreVersion;
        }

        return matcher.group(1);
    }

    public MinecraftServerCoreInfo getServerCore() {
        return minecraftServerCoreDao.getMinecraftCoreInfo();
    }

    public List<String> getEnabledPluginNames() {
        return getEnabledPlugins().stream()
                .map(MinecraftPluginInfo::name)
                .toList();
    }
}
