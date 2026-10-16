package com.pixplaze.exchange;

import com.pixplaze.api.rest.PlayerController;
import com.pixplaze.api.rest.ServerController;
import com.pixplaze.api.websocket.ChatWebSocketController;
import com.pixplaze.api.websocket.ConsoleWebSocketController;
import com.pixplaze.plugin.PixplazeCorePlugin;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.config.RouterConfig;
import org.eclipse.jetty.websocket.core.Configuration;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

import java.time.Duration;

import static io.javalin.apibuilder.ApiBuilder.path;

public class JavalinExchangeServer implements ExchangeServer<Javalin> {

    /// Сколько кадров может ждать отправки одному websocket-клиенту. Больше — клиент не успевает
    /// читать: Jetty отклоняет кадр, а контроллер закрывает соединение.
    private static final int WS_MAX_OUTGOING_FRAMES = 1000;
    /// Сколько ждать записи кадра клиенту, прежде чем считать отправку неудачной.
    private static final Duration WS_WRITE_TIMEOUT = Duration.ofSeconds(30);

    private final Javalin javalin;
    private final int port;

    public JavalinExchangeServer(int port) {
        this.javalin = initializeJavalin();
        this.port = port;
    }

    private Javalin initializeJavalin() {
        return Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.router.ignoreTrailingSlashes = true;
            config.router.mount(new PlayerController()::register);
            config.router.mount(new ServerController()::register);
            config.router.mount(new ConsoleWebSocketController(PixplazeCorePlugin.getInstance().getConsoleBuffer())::register);
            config.router.mount(new ChatWebSocketController(PixplazeCorePlugin.getInstance().getChatBuffer())::register);
            config.jsonMapper(new JavalinGsonWrapper());
            config.jetty.modifyWebSocketServletFactory(JavalinExchangeServer::limitOutgoing);
        });
    }

    /// Очередь и тайм-аут исходящих websocket-кадров — силами Jetty. В Jetty 11 этих настроек нет
    /// в публичном интерфейсе фабрики, но её реализация — {@link Configuration} ядра websocket.
    /// Другая версия Jetty может устроить её иначе — тогда без ограничений, с предупреждением.
    private static void limitOutgoing(JettyWebSocketServletFactory factory) {
        if (factory instanceof Configuration configuration) {
            configuration.setMaxOutgoingFrames(WS_MAX_OUTGOING_FRAMES);
            configuration.setWriteTimeout(WS_WRITE_TIMEOUT);
        } else {
            PixplazeCorePlugin.getInstance().getLogger().warning(
                    "WebSocket outgoing frame limit is not supported by this Jetty version: " + factory.getClass().getName());
        }
    }

    @Override
    public void start() {
        javalin.start(port);
    }

    /// {@code javalin.stop()} синхронный: внутри он зовёт {@code Server.stop()} Jetty,
    /// который дожидается остановки своих потоков и закрытия соединений. Отдельное
    /// ожидание не нужно — прежний вариант (фоновый поток со {@code Thread.sleep(1000)})
    /// ничего не ждал и от ошибки `zip file closed` не спасал.
    @Override
    public void stop() {
        javalin.stop();
    }

    @Override
    public Javalin provide() {
        return javalin;
    }
}
