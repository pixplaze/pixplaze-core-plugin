package com.pixplaze.exchange;

import com.pixplaze.api.rest.PlayerController;
import com.pixplaze.api.rest.ServerController;
import com.pixplaze.api.websocket.ChatWebSocketController;
import com.pixplaze.plugin.PixplazeCorePlugin;
import io.javalin.Javalin;

public class JavalinExchangeServer implements ExchangeServer<Javalin> {

    private final Javalin javalin;
    private final int port;

    public JavalinExchangeServer(int port) {
        this.javalin = initializeJavalin();
        this.port = port;
        register();
    }

    private Javalin initializeJavalin() {
        return Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.routing.ignoreTrailingSlashes = true;
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
    }

    @Override
    public Javalin provide() {
        return javalin;
    }

    @Override
    public void register() {
        new PlayerController().register(this);
        new ServerController().register(this);
        new ChatWebSocketController(PixplazeCorePlugin.getInstance().getConsoleBuffer()).register(this);
    }
}
