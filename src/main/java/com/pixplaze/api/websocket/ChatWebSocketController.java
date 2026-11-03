package com.pixplaze.api.websocket;

import com.pixplaze.api.dao.MinecraftPlayerDao;
import com.pixplaze.api.dao.MinecraftServerDao;
import com.pixplaze.api.ext.data.message.MinecraftMessageInfo;
import com.pixplaze.auth.PluginAccess;
import com.pixplaze.auth.PluginTokenVerifier;
import com.pixplaze.auth.VerifiedToken;
import com.pixplaze.message.ChatBuffer;
import io.javalin.websocket.WsMessageContext;
import org.bukkit.Bukkit;

import java.util.Locale;

/// Чат ({@code /chat}): видимые всем сообщения чата и команды, доступные обычному игроку.
///
/// Доступ — участнику сервера ({@code RMPL}), не забаненному и, если whitelist включён, состоящему в нём.
/// Сообщение веб-клиента уходит в чат с префиксом {@value #WEB_PREFIX} и именем игрока из токена
/// ({@code [Web] <Steve> привет}); {@code /msg} и {@code /me} пока выполняются консолью.
/// Ответы команд, которые видит только отправитель, приходят ему одному ({@code REPLY}).
public class ChatWebSocketController extends MessageWebSocketController {

    /// Предел длины сообщения чата в Minecraft.
    private static final int MAX_LENGTH = 256;
    private static final String WEB_PREFIX = "[Web] ";
    private static final String HELP = "Commands: /msg <player> <message> (/tell, /w), /me <action>, /list, /help";

    private final ChatBuffer chatBuffer;
    private final MinecraftServerDao minecraftServerDao = new MinecraftServerDao();
    private final MinecraftPlayerDao minecraftPlayerDao = new MinecraftPlayerDao();

    public ChatWebSocketController(ChatBuffer chatBuffer, PluginTokenVerifier verifier) {
        super("/chat", chatBuffer, verifier);
        this.chatBuffer = chatBuffer;
    }

    @Override
    protected boolean isAllowed(VerifiedToken token) {
        return PluginAccess.canUseChat(token);
    }

    @Override
    protected void handleMessage(WsMessageContext context, VerifiedToken token, String text) {
        if (text.length() > MAX_LENGTH) {
            reply(context, "Message is too long: %d characters, at most %d".formatted(text.length(), MAX_LENGTH));
            return;
        }
        if (!text.startsWith("/")) {
            runOnMainThread(() -> sendWebMessage(token, text));
            return;
        }

        final var command = text.substring(1).strip();
        final var label = label(command);
        switch (label) {
            // Личное сообщение в общий чат не попадает: ChatBuffer его не ловит
            case "msg", "tell", "w", "me" -> runOnMainThread(() -> dispatchAsConsole(command));
            case "list" -> runOnMainThread(() -> reply(context, onlinePlayers()));
            case "help" -> reply(context, HELP);
            default -> reply(context, "Unknown command: /%s. Type /help".formatted(label));
        }
    }

    /// Сообщение веб-клиента всем игрокам и в лог от имени игрока из токена. Не через
    /// {@code Bukkit.broadcastMessage}: его поймал бы {@link ChatBuffer} как {@code BROADCAST},
    /// а это сообщение — {@code WEB}. Вызывать только в главном потоке.
    private void sendWebMessage(VerifiedToken token, String text) {
        final var author = token.playerName() != null ? token.playerName() : token.subject();
        final var line = "%s<%s> %s".formatted(WEB_PREFIX, author, text);
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(line));
        Bukkit.getConsoleSender().sendMessage(line);
        chatBuffer.add(new MinecraftMessageInfo(MinecraftMessageInfo.Type.WEB, author,
                token.playerUuid().orElse(null), text, System.currentTimeMillis()));
    }

    /// Ответ на {@code /list}. Вызывать только в главном потоке.
    private String onlinePlayers() {
        final var names = minecraftPlayerDao.getOnlinePlayerNames();
        return "Online %d/%d: %s".formatted(names.size(), minecraftServerDao.getMaxPlayers(), String.join(", ", names));
    }

    /// Метка команды без пространства имён: {@code minecraft:msg Steve hi} → {@code msg}.
    private static String label(String command) {
        final var name = command.split("\\s+", 2)[0];
        return name.substring(name.indexOf(':') + 1).toLowerCase(Locale.ROOT);
    }
}
