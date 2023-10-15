package com.pixplaze.exchange;

import io.javalin.Javalin;
import io.javalin.json.JavalinGson;

import java.util.Optional;

public class ExchangeServer {
    private final Javalin javalin;
    private final HandlerMapper handlerMapper;
    private final int port;

    public ExchangeServer(int port) {
        this.port = port;
        this.javalin = initializeJavalin();
        this.handlerMapper = new HandlerMapper("com.pixplaze.api");
        handlerMapper.map(javalin);
        start();
    }

    public void start() {
        javalin.start(port);
    }

    private Javalin initializeJavalin() {
        return Optional.ofNullable(javalin)
                .orElse(Javalin.create(config -> {
                            config.showJavalinBanner = false;
                            config.routing.ignoreTrailingSlashes = true;
                            config.jsonMapper(new JavalinGson());
                }));
    }

    public void stop() {
        Optional.ofNullable(javalin)
                .ifPresent(Javalin::stop);
    }

    @Deprecated
    public <T> void register(Class<T> controllerClass, Object ... args) {
        var mapping = HandlerMapper.lookupControllerClass(controllerClass, args);
        mapping.forEach(this::map);
    }

    private void map(MethodHandlerWrapper wrapper) {
        javalin.addHandler(wrapper.method(), wrapper.path(), wrapper);
    }
}