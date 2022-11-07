package com.pixplaze.plugin;

import com.pixplaze.exceptions.HttpServerException;
import com.pixplaze.exceptions.InvalidAddressException;
import com.pixplaze.exceptions.CannotDefineAddressException;
import com.pixplaze.http.RconHttpController;
import com.pixplaze.http.server.PixplazeHttpServer;
import com.pixplaze.http.rcon.ConsoleBuffer;
import com.pixplaze.util.Optional;
import org.bukkit.plugin.java.JavaPlugin;

public final class PixplazeCorePlugin extends JavaPlugin {

    private static PixplazeCorePlugin instance;

    private ConsoleBuffer consoleBuffer;
    private PixplazeHttpServer rconServer;

    public PixplazeCorePlugin() {
        if (instance == null) {
            instance = this;
        }
    }

    public static PixplazeCorePlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        initConsoleBuffer();
        initRconHttpServer();
        saveDefaultConfig();
        rconServer.mount(new RconHttpController(this, getLogger()));

        var messages = getServer().getMessenger().getOutgoingChannels();
        for (var message: messages) {
            getLogger().warning(message);
        }
    }

    @Override
    public void onDisable() {
        Optional.runNotNull(getRconHttpServer(), PixplazeHttpServer::stop);
    }

    public ConsoleBuffer getConsoleBuffer() {
        return consoleBuffer;
    }

    private void initConsoleBuffer() {
        consoleBuffer = new ConsoleBuffer();
        consoleBuffer.attachLogger();
    }

    /**<pre>
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
     * </pre>
     */
    private void initRconHttpServer() {
        var address = getConfig().getString("http-listen-ip");
        var port = getConfig().getInt("http-listen-port");
        var tryAgain = true;
        while (tryAgain) {
            try {
                rconServer = new PixplazeHttpServer(address, port);
                tryAgain = false;
            } catch (InvalidAddressException e) {
                address = "auto";
                getLogger().warning(e.getMessage());
            } catch (CannotDefineAddressException e) {
                address = "127.0.0.1";
                getLogger().warning(e.getMessage());
            } catch (HttpServerException e) {
                tryAgain = false;
                getLogger().warning(e.getMessage());
            }
        }
        Optional.runNotNull(getRconHttpServer(), rcon -> {
            rcon.start();
            getLogger().info("Starting PixplazeCore on: %s:%d".formatted(rcon.getAddress(), rcon.getPort()));
        });
    }

    public PixplazeHttpServer getRconHttpServer() {
        return this.rconServer;
    }
}
