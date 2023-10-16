package com.pixplaze.api.websocket.controller;

import com.pixplaze.exchange.ExchangeController;
import com.pixplaze.exchange.JavalinExchangeServer;
import com.pixplaze.rcon.ConsoleBuffer;
import io.javalin.websocket.WsContext;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatWebSocketController implements ExchangeController<JavalinExchangeServer> {

    private static final int MAX_BUFFER_SIZE = 1024;
    private static final int MAX_CONSOLE_LINES = 50;

    private final Map<WsContext, String> activeSessions = new ConcurrentHashMap<>();
    private final ConsoleBuffer consoleBuffer = new ConsoleBuffer();

    public ChatWebSocketController() {
        consoleBuffer.attachLogger();
    }

    private void onConnect(WsContext context) {
        context.session.setIdleTimeout(Duration.ofSeconds(1));
        context.send(consoleBuffer.getHistory());
        activeSessions.put(context, context.sessionAttribute("username"));
    }

    private void onDisconnect(WsContext context) {

    }

    private void onMessage(WsContext context) {

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
