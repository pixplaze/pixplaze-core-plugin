package com.pixplaze.api.websocket;

import com.pixplaze.api.ext.data.message.MinecraftMessageInfo;
import com.pixplaze.chat.ChatBuffer;
import io.javalin.websocket.WsMessageContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.stream.Collectors;

/// Чат ({@code /chat}): видимые всем сообщения чата и команды, доступные обычному игроку.
///
/// Пока в websocket нет авторизации, веб-клиент пишет от имени консоли: текст уходит в чат
/// с префиксом {@value #WEB_PREFIX}, а {@code /msg} и {@code /me} выполняются консолью.
/// Ответы команд, которые видит только отправитель, приходят ему одному ({@code REPLY}).
public class ChatWebSocketController extends MessageWebSocketController {

    /// Предел длины сообщения чата в Minecraft.
    private static final int MAX_LENGTH = 256;
    private static final String WEB_PREFIX = "[Web] ";
    private static final String HELP = "Commands: /msg <player> <message> (/tell, /w), /me <action>, /list, /help";

    private final ChatBuffer chatBuffer;

    public ChatWebSocketController(ChatBuffer chatBuffer) {
        super("/chat", chatBuffer);
        this.chatBuffer = chatBuffer;
    }

    @Override
    protected void handleMessage(WsMessageContext context, String text) {
        if (text.length() > MAX_LENGTH) {
            reply(context, "Message is too long: %d characters, at most %d".formatted(text.length(), MAX_LENGTH));
            return;
        }
        if (!text.startsWith("/")) {
            runOnMainThread(() -> sendWebMessage(text));
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

    /// Сообщение веб-клиента всем игрокам и в лог, от имени консоли. Не через
    /// {@code Bukkit.broadcastMessage}: его поймал бы {@link ChatBuffer} как {@code BROADCAST},
    /// а это сообщение — {@code WEB}. Вызывать только в главном потоке.
    private void sendWebMessage(String text) {
        final var line = WEB_PREFIX + text;
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(line));
        Bukkit.getConsoleSender().sendMessage(line);
        chatBuffer.add(new MinecraftMessageInfo(
                MinecraftMessageInfo.Type.WEB, null, null, text, System.currentTimeMillis()));
    }

    /// Ответ на {@code /list}. Вызывать только в главном потоке.
    private static String onlinePlayers() {
        final var players = Bukkit.getOnlinePlayers();
        final var names = players.stream()
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
        return "Online %d/%d: %s".formatted(players.size(), Bukkit.getMaxPlayers(), names);
    }

    /// Метка команды без пространства имён: {@code minecraft:msg Steve hi} → {@code msg}.
    private static String label(String command) {
        final var name = command.split("\\s+", 2)[0];
        return name.substring(name.indexOf(':') + 1).toLowerCase(Locale.ROOT);
    }
}
