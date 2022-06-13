package com.pixplaze.plugin;

import com.pixplaze.exceptions.HttpServerException;
import com.pixplaze.exceptions.InvalidAddressException;
import com.pixplaze.exceptions.UnableToDefineLocalAddress;
import com.pixplaze.http.RconHttpServer;
import com.pixplaze.http.rcon.ConsoleBuffer;
import com.pixplaze.util.Optional;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.SocketException;

public final class PixplazeRootsAPI extends JavaPlugin {

    private static PixplazeRootsAPI instance;

    private RconHttpServer rconServer;

    private ConsoleBuffer consoleBuffer;

    public PixplazeRootsAPI() throws SocketException {
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
        initRconHttpServer();
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        Optional.runNotNull(getRconHttpServer(), RconHttpServer::stop);
    }

    public ConsoleBuffer getConsoleBuffer() {
        return consoleBuffer;
    }

    private void initConsoleBuffer() {
        consoleBuffer = new ConsoleBuffer();
        consoleBuffer.attachLogger();
    }

    /**
     * Создаёт и инициализирует HTTP-сервер по адресу и порту из {@code config.yml}.
     *
     * В случае, если в конфигурационном файле не указан {@code http-listen-ip},
     * указан неправильно или указан как {@code auto},
     * то пытается автоматически определить ip-адрес.
     *
     * В случае, если автоматически определить адрес не удаётся, сервер запускается
     * на {@code 127.0.0.1}.
     *
     * Если создать HTTP-сервер по прежнуму не удаётся, печатает сообщение об ошибке.
     */
    private void initRconHttpServer() {
        var address = getConfig().getString("http-listen-ip");
        var port = getConfig().getInt("http-listen-port");
        var tryAgain = true;
        while (tryAgain) {
            try {
                rconServer = new RconHttpServer(address, port);
                tryAgain = false;
            } catch (InvalidAddressException e) {
                address = "auto";
                getLogger().warning(e.getMessage());
            } catch (UnableToDefineLocalAddress e) {
                address = "127.0.0.1";
                getLogger().warning(e.getMessage());
            } catch (HttpServerException e) {
                tryAgain = false;
                getLogger().warning(e.getMessage());
            }
        }
        Optional.runNotNull(getRconHttpServer(), rcon -> {
            rcon.start();
            getLogger().warning("Starting PixplazeCore on: %s:%d".formatted(rcon.getAddress(), rcon.getPort()));
        });
    }

    public RconHttpServer getRconHttpServer() {
        return this.rconServer;
    }
}
