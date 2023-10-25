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
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ChatWebSocketController implements ExchangeController<JavalinExchangeServer> {

    private static final int MAX_BUFFER_SIZE = 1024;
    private static final int MAX_CONSOLE_LINES = 50;
    private static int activeConnections = 0;
    private static final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();
    private static final Logger logger = plugin.getLogger();
    private final Map<WsContext, BukkitTask> contexts = new ConcurrentHashMap<>();
    private final ConsoleBuffer consoleBuffer;

    public ChatWebSocketController(ConsoleBuffer consoleBuffer) {
        this.consoleBuffer = consoleBuffer;
    }

    private void onConnect(WsConnectContext context) {
        activeConnections++;
        plugin.getLogger().warning("Active connections: [%2s]".formatted(activeConnections));
//        context.send(consoleBuffer.getHistory());
        contexts.put(context, new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        }.runTaskTimer(plugin, 0L, 20 * 10));
        PixplazeCorePlugin.getInstance().getLogger().warning("WS CONNECT");
    }

    private void onDisconnect(WsCloseContext context) {
        activeConnections--;
        context.session.close();

        // Canceling BukkitTask then remove session!!!
        contexts.get(context).cancel();
        contexts.remove(context);

        PixplazeCorePlugin.getInstance().getLogger().warning("WS DISCONNECT");
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

    private void update() {
        contexts.keySet().stream()
                .filter(ctx -> ctx.session.isOpen())
                .forEach(session -> session.send(consoleBuffer.getHistory()));
        contexts.forEach((key, value) -> logger.warning(
                "[%s]: %s".formatted(
                        key.getSessionId(),
                        key.session.isOpen() ? "opened" : "closed")
        ));
        logger.warning("Active sessions: %d".formatted(contexts.size()));
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
