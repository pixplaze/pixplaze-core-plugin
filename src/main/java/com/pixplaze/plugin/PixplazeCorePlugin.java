package com.pixplaze.plugin;

import com.pixplaze.exchange.ExchangeServer;
import com.pixplaze.exchange.JavalinExchangeServer;
import com.pixplaze.rcon.ConsoleBuffer;
import io.javalin.Javalin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class PixplazeCorePlugin extends JavaPlugin {

    private static PixplazeCorePlugin instance;
    private ConsoleBuffer consoleBuffer;
    private ExchangeServer<Javalin> exchangeServer;

    public PixplazeCorePlugin() {
        instance = this;
    }

    public static PixplazeCorePlugin getInstance() {
        return instance = Optional.ofNullable(instance)
                .orElseGet(PixplazeCorePlugin::new);
    }

    @Override
    public void onEnable() {
        initConsoleBuffer();
        initHttpServer();
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        Optional.ofNullable(exchangeServer)
                .ifPresent(ExchangeServer::stop);
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
     * Если создать HTTP-сервер по прежнему не удаётся, печатает сообщение об ошибке.
     * </pre>
     */
    private void initHttpServer() {
        var address = getConfig().getString("http-listen-ip");
        var port = getConfig().getInt("http-listen-port");

        exchangeServer = new JavalinExchangeServer(port);
        exchangeServer.start();
    }
}
