package com.pixplaze.api.websocket;

import com.pixplaze.auth.PluginAccess;
import com.pixplaze.auth.PluginTokenVerifier;
import com.pixplaze.auth.VerifiedToken;
import com.pixplaze.message.ConsoleBuffer;
import com.pixplaze.plugin.PixplazeCorePlugin;
import io.javalin.websocket.WsMessageContext;

/// Консоль администратора ({@code /console}): лог сервера и выполнение любых консольных команд.
/// Только оператор: роль {@code RMOP} в токене и {@code isOp()} игрока на сервере.
public class ConsoleWebSocketController extends MessageWebSocketController {

    public ConsoleWebSocketController(PixplazeCorePlugin plugin, ConsoleBuffer consoleBuffer, PluginTokenVerifier verifier) {
        super(plugin,"/console", consoleBuffer, verifier);
    }

    @Override
    protected boolean isAllowed(VerifiedToken token) {
        return PluginAccess.canUseConsole(token);
    }

    /// Сообщение — команда консоли; ведущий «/» необязателен ({@code dispatchCommand} ждёт команду без него).
    @Override
    protected void handleMessage(WsMessageContext context, VerifiedToken token, String text) {
        final var command = text.startsWith("/") ? text.substring(1).strip() : text;
        if (command.isEmpty()) {
            return;
        }
        runOnMainThread(() -> dispatchAsConsole(command));
    }
}
