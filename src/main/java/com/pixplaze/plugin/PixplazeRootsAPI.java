package com.pixplaze.plugin;

import com.pixplaze.http.RconHttpServer;
import com.pixplaze.http.rcon.ConsoleBuffer;
import org.bukkit.plugin.java.JavaPlugin;

public final class PixplazeRootsAPI extends JavaPlugin {

    private static PixplazeRootsAPI instance;
    private static RconHttpServer rconServer;

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
        getRconHttpServerInstance().start();
    }

    @Override
    public void onDisable() {
        getRconHttpServerInstance().stop();
    }

    public ConsoleBuffer getConsoleBuffer() {
        return consoleBuffer;
    }

    private void initConsoleBuffer() {
        consoleBuffer = new ConsoleBuffer();
        consoleBuffer.attachLogger();
    }

    private RconHttpServer getRconHttpServerInstance() {
        if (rconServer == null) {
            rconServer = new RconHttpServer(getConfig().getInt("http-rcon-port"));
        }
        return rconServer;
    }
}
