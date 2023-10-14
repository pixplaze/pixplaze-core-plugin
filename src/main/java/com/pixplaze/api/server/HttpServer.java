package com.pixplaze.api.server;

import io.javalin.Javalin;
import java.util.Optional;

public class HttpServer {
    private Javalin javalin;
    private HandlerMapper handlerMapper;
    private final int port;

    public HttpServer(int port) {
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
                .orElse(Javalin.create());
    }

    public void stop() {
        Optional.ofNullable(javalin)
                .ifPresent(Javalin::stop);
    }

    @Deprecated
    public <T> void register(Class<T> controllerClass, Object ... args) {
//        handlerMapper.mapAnnotatedHandlersByClass(controllerClass, args);
    }
}