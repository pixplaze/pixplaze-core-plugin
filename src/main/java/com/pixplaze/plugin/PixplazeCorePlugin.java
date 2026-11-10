package com.pixplaze.plugin;

import com.pixplaze.api.host.ServerHostResolver;
import com.pixplaze.api.host.ServerHostResolvers;
import com.pixplaze.api.state.ServerStateSnapshot;
import com.pixplaze.auth.PluginTokenVerifier;
import com.pixplaze.config.PixplazeConfig;
import com.pixplaze.exchange.ExchangeServer;
import com.pixplaze.exchange.JavalinExchangeServer;
import com.pixplaze.message.ChatBuffer;
import com.pixplaze.message.ConsoleBuffer;
import com.pixplaze.webapi.*;
import io.javalin.Javalin;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;

public final class PixplazeCorePlugin extends JavaPlugin {

    private static PixplazeCorePlugin instance;
    private static PixplazeConfig pixplazeConfig;
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
        // 1. Пытаемся взять стандартное статическое поле
        if (instance != null) {
            return instance;
        }

        // 2. Если оно null (баг ClassLoader), ищем плагин через менеджер сервисов Bukkit
        var provider = Bukkit.getServicesManager().getRegistration(PixplazeCorePlugin.class);
        if (provider != null) {
            instance = provider.getProvider();
            return instance;
        }

        // 3. Крайний случай: ищем плагин напрямую по его текстовому имени в системе
        var plugin = Bukkit.getPluginManager().getPlugin("pixplaze-core"); // Укажите точное имя из plugin.yml
        if (plugin instanceof PixplazeCorePlugin) {
            instance = (PixplazeCorePlugin) plugin;
            return instance;
        }

        throw new IllegalStateException("PixplazeCorePlugin is not loaded or disabled!");
    }

    public PixplazeConfig getPixplazeConfig() {
        return pixplazeConfig;
    }

    public PixplazeCorePlugin() {
        super();
        instance = this;
    }

    @Override
    public void onEnable() {
        instance = this;
        pixplazeConfig = PixplazeConfig.load(this);
        saveDefaultConfig();
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

    /// Порядок важен: сначала отцепляемся от log4j и событий Bukkit (они живут дольше плагина),
    /// затем останавливаем HTTP-сервер и задачу обновления снимка, затем отпускаем
    /// статическую ссылку на плагин. Всё вместе гарантирует, что после выгрузки на классы
    /// плагина никто не ссылается, и обращений к закрытому jar не будет.
    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        Optional.ofNullable(pixplazeConnection)
                .ifPresent(PixplazeConnection::stop);

//        Optional.ofNullable(consoleBuffer)
//                .ifPresent(ConsoleBuffer::detach);
//
//        Optional.ofNullable(chatBuffer)
//                .ifPresent(ChatBuffer::detach);

        Optional.ofNullable(exchangeServer)
                .ifPresent(ExchangeServer::stop);

        Optional.ofNullable(serverStateSnapshot)
                .ifPresent(ServerStateSnapshot::stop);

        instance = null;
        exchangeServer = null;
    }

    /// Подключение, вход игроков ({@code /pixplaze login}) и проверка их токенов при входе на сервер.
    /// С {@code authorization.enabled} (на нелицензионном сервере — всегда) игрок без токена заморожен.
    private void startPixplazeConnection(PixplazeConnection connection) {
        getServer().getPluginManager().registerEvents(createAuthorizationResolver(connection), this);

        final var command = Objects.requireNonNull(getCommand("pixplaze"), "Command 'pixplaze' is missing in plugin.yml");
        final var executor = new PixplazeCommand(this, connection);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        connection.start();
    }

    private Listener createAuthorizationResolver(PixplazeConnection connection) {
        final var players = connection.players();
        final var authorizationConfig = getPixplazeConfig().authorization();

        if (authorizationConfig.enabled()) {
            return new AuthorizationGate(
                    this,
                    connection,
                    authorizationConfig.licensed(),
                    authorizationConfig.timeout(),
                    authorizationConfig.autoLogin()
            );
        }

        return new PlayerAuthorizationListener(players);
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

    private void initHttpServer() {
        final var httpConfig = getPixplazeConfig().http();
        final var ip = httpConfig.ip();
        final var port = httpConfig.port();

        this.exchangeServer = new JavalinExchangeServer(
                this,
                ip,
                port,
                tokenVerifier
        );

        this.exchangeServer.start();
    }

    /// HTTP-сервер API плагина на {@code http.port}; {@code http.ip} — интерфейс ({@code auto} или пусто — все).
    private void initHttpServerRuntime() {
        final var httpConfig = getPixplazeConfig().http();
        final var ip = httpConfig.ip();
        final var port = httpConfig.port();

        ExchangeServer<?> existingServer = (ExchangeServer<?>) System.getProperties().get("pixplaze.exchange.server");

        if (existingServer != null) { // уже загружен
            this.getLogger().info("Pixplaze API connecting to existed HTTP server..");
            this.exchangeServer = (JavalinExchangeServer) existingServer;
        } else { // загружается первый раз
            this.getLogger().info("Pixplaze API creating a new HTTP server!");
            this.exchangeServer = new JavalinExchangeServer(
                    this,
                    ip,
                    port,
                    tokenVerifier
            );

            this.exchangeServer.start();

            System.getProperties().put("pixplaze.exchange.server", this.exchangeServer);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (this.exchangeServer != null) {
                    this.exchangeServer.stop();
                }
            }));
        }
    }

    private void initHttpServerBukkitService() {
        final var httpConfig = getPixplazeConfig().http();
        final var ip = httpConfig.ip();
        final var port = httpConfig.port();

        final var serviceManager = Bukkit.getServicesManager();
        final var serviceRegistration = serviceManager.getRegistration(ExchangeServer.class);

        if (serviceRegistration != null) { // уже загружен
            this.getLogger().info("Pixplaze API connecting to existed HTTP server..");
            this.exchangeServer = serviceRegistration.getProvider();
        } else { // загружается первый раз
            this.getLogger().info("Pixplaze API creating a new HTTP server!");
            this.exchangeServer = new JavalinExchangeServer(
                    this,
                    ip,
                    port,
                    tokenVerifier
            );

            this.exchangeServer.start();

            serviceManager.register(
                    ExchangeServer.class,
                    this.exchangeServer,
                    this,
                    org.bukkit.plugin.ServicePriority.Highest
            );

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (this.exchangeServer != null) {
                    this.exchangeServer.stop();
                }
            }));
        }
    }
}
