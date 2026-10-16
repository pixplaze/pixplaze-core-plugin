package com.pixplaze.api.websocket;

import com.pixplaze.rcon.ConsoleBuffer;
import io.javalin.websocket.WsMessageContext;

/// Консоль администратора ({@code /console}): лог сервера и выполнение любых консольных команд.
public class ConsoleWebSocketController extends MessageWebSocketController {

    public ConsoleWebSocketController(ConsoleBuffer consoleBuffer) {
        super("/console", consoleBuffer);
    }

    /// Сообщение — команда консоли; ведущий «/» необязателен ({@code dispatchCommand} ждёт команду без него).
    @Override
    protected void handleMessage(WsMessageContext context, String text) {
        final var command = text.startsWith("/") ? text.substring(1).strip() : text;
        if (command.isEmpty()) {
            return;
        }
        runOnMainThread(() -> dispatchAsConsole(command));
    }
}
