package com.pixplaze.exchange;

import com.pixplaze.api.rest.PlayerController;
import com.pixplaze.api.rest.ServerController;
import com.pixplaze.api.websocket.ChatWebSocketController;
import com.pixplaze.api.websocket.ConsoleWebSocketController;
import com.pixplaze.auth.PluginTokenVerifier;
import com.pixplaze.plugin.PixplazeCorePlugin;
import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;

import java.time.Duration;
import java.util.function.Consumer;

public class JavalinExchangeServer implements ExchangeServer<Javalin> {

    /// Сколько кадров может ждать отправки одному websocket-клиенту. Больше — клиент не успевает
    /// читать: Jetty отклоняет кадр, а контроллер закрывает соединение.
    private static final int WS_MAX_OUTGOING_FRAMES = 1000;
    /// Сколько ждать записи кадра клиенту, прежде чем считать отправку неудачной.
    private static final Duration WS_WRITE_TIMEOUT = Duration.ofSeconds(30);

    private final Javalin javalin;
    private final String host;
    private final int port;
    private final ConsoleWebSocketController console;
    private final ChatWebSocketController chat;
    private final PixplazeCorePlugin plugin;
    private boolean isStarted = false;

    /// @param host     интерфейс, на котором слушать; {@code null} — все
    /// @param verifier проверка входящих токенов: все REST-маршруты и WebSocket требуют токен
    public JavalinExchangeServer(PixplazeCorePlugin plugin, String host, int port, PluginTokenVerifier verifier) {
        this.host = host;
        this.port = port;
        this.plugin = plugin;
        this.console = new ConsoleWebSocketController(plugin, plugin.getConsoleBuffer(), verifier);
        this.chat = new ChatWebSocketController(plugin, plugin.getChatBuffer(), verifier);
        this.javalin = initializeJavalin(new AccessFilter(verifier, plugin.getLogger()));
    }

    private Javalin initializeJavalin(AccessFilter accessFilter) {
        return Javalin.create(config -> {
            config.startup.showJavalinBanner = false;
//            config.startup.startupWatcherEnabled = false;
            config.startup.showOldJavalinVersionWarning= false;
            config.jetty.modifyServer(server -> {
                server.setStopTimeout(100L);
            });
            config.jetty.modifyServletContextHandler(handler -> {
                handler.setClassLoader(plugin.getClass().getClassLoader());
            });
            config.router.ignoreTrailingSlashes = true;
            config.routes.apiBuilder(() -> {
                // Регистрация глобального фильтра "before"
                ApiBuilder.before(accessFilter::handle);

                // Регистрация контроллеров через ссылки на методы
                new PlayerController(plugin).register();
                new ServerController(plugin).register();
                console.register();
                chat.register();
            });
            config.jsonMapper(new JavalinGsonWrapper());

            config.jetty.modifyWebSocketServletFactory(jettyWsConfig -> {
                jettyWsConfig.setMaxOutgoingFrames(WS_MAX_OUTGOING_FRAMES);

                // Задаем таймаут записи (в Javalin 7 используется java.time.Duration)
//                jettyWsConfig.setWriteTimeout(Duration.ofMillis(WS_WRITE_TIMEOUT));

            });
        });
    }

    @Override
    public void start() {
        if (isStarted) return;
        try {
            Thread.sleep(500);
            if (host == null) {
                javalin.start(port);
            } else {
                javalin.start(host, port);
            }
            isStarted = true;
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains(" zip ")) {
                plugin.getLogger().warning("Accessing unloaded jar (zip file closed)!");
            }
        } catch (InterruptedException e) {
            plugin.getLogger().warning("Could not wait for Pixplaze API to be started!");
        }
    }

    /// {@code javalin.stop()} синхронный: внутри он зовёт {@code Server.stop()} Jetty,
    /// который дожидается остановки своих потоков и закрытия соединений. Отдельное
    /// ожидание не нужно — прежний вариант (фоновый поток со {@code Thread.sleep(1000)})
    /// ничего не ждал и от ошибки `zip file closed` не спасал.
    @Override
    public void stop() {
        if (javalin == null) return;

        console.stop();
        chat.stop();
        javalin.stop();

        plugin.getLogger().warning("Waiting for API server to stop...");

        try {
            isStarted = false;
            javalin.jettyServer().threadPool().join();
        } catch (InterruptedException e) {
            plugin.getLogger().warning(e.getMessage());
        }
    }

    @Override
    public Javalin provide() {
        return javalin;
    }

    @Override
    public void mount(Consumer<Javalin> routesConfig) {
        // Если сервер уже работал (это перезапуск плагина), очищаем старые маршруты

    }
}
