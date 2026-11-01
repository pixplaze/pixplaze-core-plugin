package com.pixplaze.plugin;

import com.pixplaze.api.host.ServerHostResolver;
import com.pixplaze.api.host.ServerHostResolvers;
import com.pixplaze.api.state.ServerStateSnapshot;
import com.pixplaze.auth.PluginTokenVerifier;
import com.pixplaze.chat.ChatBuffer;
import com.pixplaze.exchange.ExchangeServer;
import com.pixplaze.exchange.JavalinExchangeServer;
import com.pixplaze.rcon.ConsoleBuffer;
import com.pixplaze.webapi.AuthorizationGate;
import com.pixplaze.webapi.AuthorizationSettings;
import com.pixplaze.webapi.PixplazeCommand;
import com.pixplaze.webapi.PixplazeConnection;
import com.pixplaze.webapi.PlayerAuthorizationListener;
import io.javalin.Javalin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;

public final class PixplazeCorePlugin extends JavaPlugin {

    private static PixplazeCorePlugin instance;
    private ConsoleBuffer consoleBuffer;
    private ChatBuffer chatBuffer;
    private ExchangeServer<Javalin> exchangeServer;
    private ServerHostResolver serverHostResolver;
    private ServerStateSnapshot serverStateSnapshot;
    private PixplazeConnection pixplazeConnection;
    private PluginTokenVerifier tokenVerifier;

    /// Экземпляр, созданный сервером: заполняется в {@link #onEnable()} и обнуляется
    /// в {@link #onDisable()}. Создавать {@link JavaPlugin} вручную нельзя — его конструктор
    /// требует загрузки через PluginClassLoader сервера, поэтому при обращении к выключенному
    /// плагину честнее упасть, чем молча подсунуть неработоспособный объект.
    public static PixplazeCorePlugin getInstance() {
        return Objects.requireNonNull(instance, "PixplazeCorePlugin is disabled");
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        PluginConfig.upgrade(this);
        initMessageBuffers();
        // До HTTP-сервера: контроллеры и DAO создаются при монтировании маршрутов
        // и рассчитывают, что снимок уже есть.
        initServerStateSnapshot();
        // До HTTP-сервера: его маршруты проверяют токены по ключам и id сервера из подключения
        initPixplazeConnection();
        initHttpServer();
        initServerHostResolver();
        // После резолвера хоста: регистрации нужен публичный адрес сервера
        Optional.ofNullable(pixplazeConnection).ifPresent(this::startPixplazeConnection);
    }

    /// Подключение, вход игроков ({@code /pixplaze login}) и проверка их токенов при входе на сервер.
    /// С {@code authorization.enabled} (на нелицензионном сервере — всегда) игрок без токена заморожен.
    private void startPixplazeConnection(PixplazeConnection connection) {
        final var players = connection.players();
        final var settings = AuthorizationSettings.load(this);
        getServer().getPluginManager().registerEvents(settings.enabled()
                ? new AuthorizationGate(this, connection, settings.licensed(), settings.timeout(), settings.autoLogin())
                : new PlayerAuthorizationListener(players), this);
        final var command = Objects.requireNonNull(getCommand("pixplaze"), "Command 'pixplaze' is missing in plugin.yml");
        final var executor = new PixplazeCommand(this, connection);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        connection.start();
    }

    /// Порядок важен: сначала отцепляемся от log4j и событий Bukkit (они живут дольше плагина),
    /// затем останавливаем HTTP-сервер и задачу обновления снимка, затем отпускаем
    /// статическую ссылку на плагин. Всё вместе гарантирует, что после выгрузки на классы
    /// плагина никто не ссылается, и обращений к закрытому jar не будет.
    @Override
    public void onDisable() {
        Optional.ofNullable(pixplazeConnection)
                .ifPresent(PixplazeConnection::stop);

        Optional.ofNullable(consoleBuffer)
                .ifPresent(ConsoleBuffer::detach);

        Optional.ofNullable(chatBuffer)
                .ifPresent(ChatBuffer::detach);

        Optional.ofNullable(exchangeServer)
                .ifPresent(ExchangeServer::stop);

        Optional.ofNullable(serverStateSnapshot)
                .ifPresent(ServerStateSnapshot::stop);

        instance = null;
    }

    public ConsoleBuffer getConsoleBuffer() {
        return consoleBuffer;
    }

    public ChatBuffer getChatBuffer() {
        return chatBuffer;
    }

    /// Резолвер публичного хоста сервера. Реализация выбрана {@link ServerHostResolvers}
    /// в {@link #onEnable()} по условиям окружения (конфиг/прокси/подключения).
    public ServerHostResolver getServerHostResolver() {
        return serverHostResolver;
    }

    /// Подключение к Pixplaze (регистрация сервера, токены, heartbeat) и проверка входящих токенов.
    /// Без {@code pixplaze.gateway} в конфиге подключения нет, и все токены отклоняются.
    private void initPixplazeConnection() {
        pixplazeConnection = PixplazeConnection.create(this).orElse(null);
        tokenVerifier = new PluginTokenVerifier(Optional.ofNullable(pixplazeConnection), getLogger());
        Optional.ofNullable(pixplazeConnection).ifPresent(connection -> connection.onConnected(tokenVerifier::reloadKeys));
    }

    private void initServerHostResolver() {
        serverHostResolver = ServerHostResolvers.create(this);
    }

    /// Снимок состояния сервера, обновляемый из главного потока. HTTP-слой читает значения
    /// отсюда, а не дёргает Bukkit API из потоков Jetty.
    public ServerStateSnapshot getServerStateSnapshot() {
        return serverStateSnapshot;
    }

    private void initServerStateSnapshot() {
        serverStateSnapshot = new ServerStateSnapshot(this);
        serverStateSnapshot.start();
    }

    /// Лог сервера и видимый всем чат — источники websocket-потоков {@code /console} и {@code /chat}.
    private void initMessageBuffers() {
        consoleBuffer = new ConsoleBuffer();
        consoleBuffer.attach();

        chatBuffer = new ChatBuffer(this);
        chatBuffer.attach();
    }

    /// HTTP-сервер API плагина на {@code http.port}; {@code http.ip} — интерфейс ({@code auto} или пусто — все).
    private void initHttpServer() {
        final var ip = getConfig().getString(PluginConfig.HTTP_IP, "auto");
        final var port = getConfig().getInt(PluginConfig.HTTP_PORT, 25566);

        exchangeServer = new JavalinExchangeServer(ip == null || ip.isBlank() || "auto".equalsIgnoreCase(ip) ? null : ip.trim(), port, tokenVerifier);
        exchangeServer.start();
    }
}
