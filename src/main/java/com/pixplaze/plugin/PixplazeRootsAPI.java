package com.pixplaze.plugin;

import com.pixplaze.http.RconHttpServer;
import com.pixplaze.http.rcon.ConsoleBuffer;
import org.bukkit.plugin.java.JavaPlugin;

public final class PixplazeRootsAPI extends JavaPlugin{

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
