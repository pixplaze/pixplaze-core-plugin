package com.pixplaze.api.websocket;

import com.pixplaze.exchange.ExchangeController;
import com.pixplaze.exchange.JavalinExchangeServer;
import com.pixplaze.plugin.PixplazeCorePlugin;
import com.pixplaze.rcon.ConsoleBuffer;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatWebSocketController implements ExchangeController<JavalinExchangeServer> {

    private static final int MAX_BUFFER_SIZE = 1024;
    private static final int MAX_CONSOLE_LINES = 50;
    private static final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();

//    private final Map<WsContext, String> activeSessions = new ConcurrentHashMap<>();
    private final List<WsContext> contexts = Collections.synchronizedList(new ArrayList<>());
    private final ConsoleBuffer consoleBuffer;

    public ChatWebSocketController(ConsoleBuffer consoleBuffer) {
        this.consoleBuffer = consoleBuffer;
    }

    private void onConnect(WsConnectContext context) {
        context.session.setIdleTimeout(Duration.ofSeconds(1));
        context.send(consoleBuffer.getHistory());
        contexts.add(context);
        initializeUpdateCycle();
    }

    private void onDisconnect(WsCloseContext context) {
        context.session.close();
        contexts.remove(context);
    }

    private void onMessage(WsMessageContext context) {
        var message = context.message();
        var sender = Bukkit.getConsoleSender();
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(sender, message);
            }
        }.runTask(plugin);
    }

    private void initializeUpdateCycle() {
        new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20);
    }

    private void update() {
        contexts.stream()
                .filter(ctx -> ctx.session.isOpen())
                .forEach(session -> session.send(consoleBuffer.getHistory()));
    }

    private void addSession() {

    }

    @Override
    public void register(JavalinExchangeServer server) {
        final var app = server.provide();
        app.ws("/chat", config -> {
            config.onConnect(this::onConnect);
            config.onClose(this::onDisconnect);
            config.onMessage(this::onMessage);
        });
    }
}
