package com.pixplaze.exchange;

import com.pixplaze.api.rest.PlayerController;
import com.pixplaze.api.rest.ServerController;
import com.pixplaze.api.websocket.ChatWebSocketController;
import com.pixplaze.plugin.PixplazeCorePlugin;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.config.RouterConfig;

import static io.javalin.apibuilder.ApiBuilder.path;

public class JavalinExchangeServer implements ExchangeServer<Javalin> {

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
            config.router.mount(new ChatWebSocketController(PixplazeCorePlugin.getInstance().getConsoleBuffer())::register);
            config.jsonMapper(new JavalinGsonWrapper());
        });
    }

    @Override
    public void start() {
        javalin.start(port);
    }

    @Override
    public void stop() {
        javalin.stop();
        new Thread(this::awaitForClose).start();
    }

    @Override
    public Javalin provide() {
        return javalin;
    }

    /**
     * Ожидает закрытия процесса Javalin чтобы не блокировать jar-файл.
     */
    private void awaitForClose() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
