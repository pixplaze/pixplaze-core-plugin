package com.pixplaze.api.websocket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.pixplaze.api.ext.data.message.MinecraftMessageInfo;
import com.pixplaze.auth.InvalidTokenException;
import com.pixplaze.auth.PluginTokenVerifier;
import com.pixplaze.auth.VerifiedToken;
import com.pixplaze.exchange.ExchangeController;
import com.pixplaze.exchange.JavalinExchangeServer;
import com.pixplaze.exchange.JavalinGsonWrapper;
import com.pixplaze.message.MessageBuffer;
import com.pixplaze.plugin.PixplazeCorePlugin;
import io.javalin.json.JsonMapper;
import io.javalin.router.JavalinDefaultRouting;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsCloseStatus;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import org.bukkit.Bukkit;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/// Websocket-поток сообщений {@link MessageBuffer}: при подключении клиент получает историю первым
/// кадром (JSON-массив {@link MinecraftMessageInfo}), дальше — каждое новое сообщение отдельным
/// кадром (JSON-объект). Входящие сообщения клиента разбирает наследник в {@link #handleMessage}.
///
/// Отправка неблокирующая: рассылку зовут поток лога (в том числе главный поток сервера) и
/// обработчики событий чата, и ждать сеть медленного клиента им нельзя. Кадр встаёт в очередь
/// Jetty; размер очереди и тайм-аут записи ограничивает Jetty ({@link JavalinExchangeServer}).
/// Неудачная отправка (клиент отключился, не успевает читать) убирает только этого клиента
/// и закрывает его соединение — он переподключится и получит историю заново; наружу ошибка не уходит.
///
/// Авторизация — первым кадром `{"type":"AUTH","token":…}` с токеном доступа к плагину; история и рассылка —
/// только после него. Без кадра за {@link #AUTH_TIMEOUT}, с неверным или истёкшим токеном — закрытие
/// {@value #CLOSE_UNAUTHORIZED}; токен верный, но прав нет ({@link #isAllowed}) — {@value #CLOSE_FORBIDDEN}.
/// Новый кадр `AUTH` заменяет токен без переподключения; не прислали до {@code exp} — закрытие 4401.
public abstract class MessageWebSocketController implements ExchangeController<JavalinExchangeServer> {

    /// Коды закрытия (диапазон 4000–4999 отведён приложениям, RFC 6455).
    static final int CLOSE_UNAUTHORIZED = 4401;
    static final int CLOSE_FORBIDDEN = 4403;
    private static final Duration AUTH_TIMEOUT = Duration.ofSeconds(5);
    private static final String AUTH_TYPE = "AUTH";

    /// Тот же маппер, что у Javalin; JSON собирается один раз на рассылку, а не на каждого клиента.
    private static final JsonMapper JSON = new JavalinGsonWrapper();

    protected final PixplazeCorePlugin plugin = PixplazeCorePlugin.getInstance();
    private final String path;
    private final MessageBuffer buffer;
    /// Открытые подключения: добавляют и удаляют потоки Jetty, рассылают потоки источника буфера.
    /// {@link WsContext} равны по сессии, поэтому контексты подключения, сообщения и закрытия
    /// одного клиента — один элемент. Значение — последний проверенный токен клиента: кто пишет.
    private final Map<WsContext, VerifiedToken> contexts = new ConcurrentHashMap<>();
    /// Тайм-аут кадра AUTH (до авторизации) или срок токена (после): по нему соединение закрывается.
    private final Map<WsContext, ScheduledFuture<?>> deadlines = new ConcurrentHashMap<>();
    private final PluginTokenVerifier verifier;
    private final ScheduledExecutorService scheduler;

    protected MessageWebSocketController(String path, MessageBuffer buffer, PluginTokenVerifier verifier) {
        this.path = path;
        this.buffer = buffer;
        this.verifier = verifier;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            final var thread = new Thread(runnable, "Pixplaze WS " + path + " deadlines");
            thread.setDaemon(true);
            return thread;
        });
        this.buffer.addHandler(this::broadcast);
    }

    /// Входящее сообщение клиента: уже без пробелов по краям, непустое и однострочное.
    /// {@code token} — последний проверенный токен этого клиента.
    protected abstract void handleMessage(WsMessageContext context, VerifiedToken token, String text);

    /// Права проверенного токена на этот поток: роль из токена и состояние сервера.
    protected abstract boolean isAllowed(VerifiedToken token);

    /// Остановить таймеры: вызывается при остановке HTTP-сервера.
    public void stop() {
        scheduler.shutdownNow();
    }

    @Override
    public void register(JavalinDefaultRouting routing) {
        routing.ws(path, config -> {
            config.onConnect(this::onConnect);
            config.onClose(this::onClose);
            config.onMessage(this::onMessage);
        });
    }

    private void onConnect(WsConnectContext context) {
        // Тихий сервер — норма, а Jetty закрывает соединение без трафика по тайм-ауту простоя.
        // Пинг на половине тайм-аута держит живого клиента (браузер отвечает pong сам), мёртвый отваливается.
        // Тайм-аут 0 — простой не ограничен, пинговать незачем.
        final var pingInterval = context.session.getIdleTimeout().dividedBy(2);
        if (pingInterval.toMillis() > 0) {
            context.enableAutomaticPings(pingInterval.toMillis(), TimeUnit.MILLISECONDS);
        }

        // История и рассылка — только после кадра AUTH
        schedule(context, AUTH_TIMEOUT, "AUTH frame expected");
        plugin.getLogger().finest("WS %s connected, awaiting AUTH".formatted(path));
    }

    private void onClose(WsCloseContext context) {
        context.disableAutomaticPings();
        contexts.remove(context);
        cancelDeadline(context);

        // Код закрытия: 1001 + "Connection Idle Timeout" — тайм-аут Jetty, 1000/1005 — закрыл клиент
        plugin.getLogger().finest("WS %s closed: %d %s".formatted(path, context.status(), context.reason()));
    }

    private void onMessage(WsMessageContext context) {
        final var text = context.message().strip();
        if (text.isEmpty()) {
            return;
        }
        final var authToken = authToken(text);
        if (authToken != null) {
            authorize(context, authToken);
            return;
        }
        final var token = contexts.get(context);
        if (token == null) {
            close(context, CLOSE_UNAUTHORIZED, "AUTH frame expected");
            return;
        }
        if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            plugin.getLogger().warning("Rejected multi-line message from web client on " + path);
            return;
        }
        handleMessage(context, token, text);
    }

    /// Первый кадр AUTH открывает поток (история первым кадром ответа), следующие заменяют токен.
    private void authorize(WsContext context, String token) {
        final VerifiedToken verified;
        try {
            verified = verifier.verify(token);
        } catch (InvalidTokenException e) {
            plugin.getLogger().fine(() -> "WS %s AUTH rejected with 4401: %s".formatted(path, e.getMessage()));
            close(context, CLOSE_UNAUTHORIZED, "Invalid token");
            return;
        }
        if (!isAllowed(verified)) {
            plugin.getLogger().fine(() -> "WS %s AUTH rejected with 4403: roles %s from %s, subject %s".formatted(path,
                    verified.authority().roles(), verified.authority().source(), verified.subject()));
            close(context, CLOSE_FORBIDDEN, "Forbidden");
            return;
        }

        schedule(context, Duration.between(Instant.now(), verified.expiresAt()), "Token expired");
        // Повторный AUTH: новый токен заменяет старый
        if (contexts.replace(context, verified) != null) {
            return;
        }
        // История — до добавления в рассылку: первым кадром клиент получает её, а не новое сообщение
        if (send(context, JSON.toJsonString(buffer.getHistory(), List.class))) {
            contexts.put(context, verified);
        }
        plugin.getLogger().finest("WS %s authorized, active connections: %d".formatted(path, contexts.size()));
    }

    /// Токен из кадра `{"type":"AUTH","token":…}`; иначе {@code null} — это обычное сообщение.
    private static String authToken(String text) {
        if (!text.startsWith("{")) {
            return null;
        }
        try {
            final JsonObject frame = JsonParser.parseString(text).getAsJsonObject();
            if (frame.has("type") && AUTH_TYPE.equals(frame.get("type").getAsString()) && frame.has("token")) {
                return frame.get("token").getAsString();
            }
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            // Не JSON-объект — обычное сообщение
        }
        return null;
    }

    private void schedule(WsContext context, Duration delay, String reason) {
        final var deadline = scheduler.schedule(() -> close(context, CLOSE_UNAUTHORIZED, reason),
                Math.max(delay.toMillis(), 0), TimeUnit.MILLISECONDS);
        final var previous = deadlines.put(context, deadline);
        if (previous != null) {
            previous.cancel(false);
        }
    }

    private void cancelDeadline(WsContext context) {
        final var deadline = deadlines.remove(context);
        if (deadline != null) {
            deadline.cancel(false);
        }
    }

    private void close(WsContext context, int code, String reason) {
        if (code == CLOSE_UNAUTHORIZED && !"Invalid token".equals(reason)) {
            plugin.getLogger().fine(() -> "WS %s closed with %d: %s".formatted(path, code, reason));
        }
        contexts.remove(context);
        cancelDeadline(context);
        try {
            context.closeSession(code, reason);
        } catch (RuntimeException ignored) {
            // Уже закрыто
        }
    }

    /// Ответ одному клиенту, в историю не попадает. Клиент уже отключился — ответ некому.
    protected void reply(WsContext context, String text) {
        if (!contexts.containsKey(context)) {
            return;
        }
        final var message = new MinecraftMessageInfo(
                MinecraftMessageInfo.Type.REPLY, null, null, text, System.currentTimeMillis());
        send(context, JSON.toJsonString(message, MinecraftMessageInfo.class));
    }

    /// Задача в главном потоке сервера: команды и рассылка трогают мир и игроков.
    protected void runOnMainThread(Runnable task) {
        // Выключающийся плагин задачу не поставит: планировщик бросит исключение в поток Jetty
        if (!plugin.isEnabled()) {
            return;
        }
        try {
            Bukkit.getScheduler().runTask(plugin, task);
        } catch (IllegalPluginAccessException e) {
            // Плагин выключили между проверкой и постановкой задачи
        }
    }

    /// Как команда, набранная в консоли сервера: сначала {@link ServerCommandEvent} — его видят, отменяют
    /// и переписывают другие плагины (сам {@code dispatchCommand} событие не создаёт), затем запуск.
    /// Команда без ведущего «/». Вызывать только в главном потоке.
    protected static void dispatchAsConsole(String command) {
        final var sender = Bukkit.getConsoleSender();
        final var event = new ServerCommandEvent(sender, command);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            Bukkit.dispatchCommand(sender, event.getCommand());
        }
    }

    private void broadcast(MinecraftMessageInfo message) {
        if (contexts.isEmpty()) {
            return;
        }
        final var json = JSON.toJsonString(message, MinecraftMessageInfo.class);
        contexts.keySet().forEach(context -> send(context, json));
    }

    /// Неблокирующая отправка кадра: Jetty ставит асинхронные кадры в очередь сессии по порядку,
    /// замок не нужен. Возвращает, принят ли кадр; иначе клиент уже убран и отключён.
    private boolean send(WsContext context, String json) {
        try {
            context.session.getRemote().sendString(json, new WriteCallback() {
                @Override
                public void writeFailed(Throwable error) {
                    drop(context, error);
                }
            });
            return true;
        } catch (RuntimeException e) {
            // Сессия закрылась до постановки кадра
            drop(context, e);
            return false;
        }
    }

    /// Убрать клиента из рассылки и закрыть его соединение: отправка не удалась — клиент отключился,
    /// не успевает читать (очередь Jetty полна, {@code WritePendingException}) или истёк тайм-аут записи.
    /// Jetty в последних двух случаях соединение не закрывает, а клиенту без пропущенных кадров
    /// нужна история заново — её он получит при переподключении. Отключение клиента — штатная
    /// ситуация, поэтому без стека и на уровне {@code fine}.
    private void drop(WsContext context, Object reason) {
        if (contexts.remove(context) != null) {
            plugin.getLogger().fine("WS %s client dropped: %s".formatted(path, reason));
        }
        try {
            context.closeSession(WsCloseStatus.TRY_AGAIN_LATER, "Failed to deliver messages");
        } catch (RuntimeException ignored) {
            // Уже закрыто
        }
    }
}
