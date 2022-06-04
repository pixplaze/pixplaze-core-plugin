package com.pixplaze.plugin;

import com.pixplaze.PlayerListener;
import com.pixplaze.http.RconHttpServer;
import com.pixplaze.http.ResponseBodyBuilder;
import com.pixplaze.http.rcon.ConsoleBuffer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.util.ArrayList;

public final class PixplazeRootsAPI extends JavaPlugin {

    private static PixplazeRootsAPI instance;

    private ConsoleBuffer consoleBuffer;

    public PixplazeRootsAPI() {
        if (instance == null) {
            instance = this;
        }
    }

    public static PixplazeRootsAPI getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        initConsoleBuffer();
        initRconHttp();
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        if (Bukkit.dispatchCommand(null, "say DISPATCH")) {
            getLogger().warning("DISPATCH!");
        } else {
            getLogger().warning("NO DISPATCH!");
        }
    }

    @Override
    public void onDisable() {

    }

    public ConsoleBuffer getConsoleBuffer() {
        return consoleBuffer;
    }

    private void initConsoleBuffer() {
        consoleBuffer = new ConsoleBuffer();
        consoleBuffer.attachLogger();
    }

    private void initRconHttp() {
        RconHttpServer rconServer = new RconHttpServer(getConfig().getInt("http-rcon-port"));
        rconServer.start();
    }
}
