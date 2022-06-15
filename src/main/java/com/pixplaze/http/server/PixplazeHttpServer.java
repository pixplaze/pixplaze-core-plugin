package com.pixplaze.http.server;

import com.pixplaze.annotations.RequestHandler;
import com.pixplaze.exceptions.HttpServerException;
import com.pixplaze.exceptions.InvalidAddressException;
import com.pixplaze.exceptions.CannotDefineAddressException;
import com.pixplaze.http.HttpController;
import com.pixplaze.http.Methods;
import com.pixplaze.http.server.ContextMapper;
import com.pixplaze.plugin.PixplazeRootsAPI;
import com.pixplaze.util.Inet;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.*;
import java.util.logging.Logger;

public final class PixplazeHttpServer {

    public Logger logger = PixplazeRootsAPI.getInstance().getLogger();
    private final HttpServer httpServer;
    private final String address;
    private final int port;

    public PixplazeHttpServer(final int port) throws CannotDefineAddressException, HttpServerException {
        this("auto", port);
    }

    public PixplazeHttpServer(String address, final int port) throws
		    InvalidAddressException,
		    CannotDefineAddressException,
		    HttpServerException
    {
        if (address == null || address.isEmpty() || address.equalsIgnoreCase("auto"))
            address = Inet.getLocalAddress();
        else if (!Inet.isIpV4Valid(address))
            throw new InvalidAddressException("Invalid ipv4 address: %s!".formatted(address));

        this.address = address;
        this.port = port;

        try {
            httpServer = HttpServer.create(new InetSocketAddress(this.address, this.port), 0);
        } catch (IOException e) {;
            throw new HttpServerException(
					"Can not create pixplaze core api server on address: %s:%d"
		            .formatted(address, port), e
            );
        }
    }


    public final void mount(HttpController controller) {
        mapHandlers(controller);
    }

    private void mapHandlers(HttpController controller) {
        var allMethods = controller.getClass().getMethods();
        var contextMapper = new ContextMapper();

        for (var method: allMethods) {
            if (method.isAnnotationPresent(RequestHandler.class)) {
                var annotation = method.getAnnotation(RequestHandler.class);
                contextMapper.mapContext(annotation.path(), annotation.method(), method);
            }
        }

        contextMapper.getContextMapping().forEach((context, mapping) -> {
            httpServer.createContext(context, exchange -> {
                controller.beforeEach(exchange);
                try {
                    switch (exchange.getRequestMethod()) {
                        case "GET" -> {
                            var getHandler = mapping.get(Methods.GET);
                            if (getHandler != null) getHandler.invoke(controller, exchange);
                        }
                        case "POST" -> {
                            var postHandler = mapping.get(Methods.POST);
                            if (postHandler != null) postHandler.invoke(controller, exchange);
                        }
                        case "PUT" -> {
                            var putHandler = mapping.get(Methods.PUT);
                            if (putHandler != null) putHandler.invoke(controller, exchange);
                        }
                        case "DELETE" -> {
                            var deleteHandler = mapping.get(Methods.DELETE);
                            if (deleteHandler != null) deleteHandler.invoke(controller, exchange);
                        }
                    }
                } catch (Throwable e) {
                    logger.warning(e.getMessage());
                }
                exchange.close();
            });
        });

        var contexts = contextMapper.getContextMapping();
        var contextsCount = contexts.size();
        logger.warning("Count of contexts: %s".formatted(contextsCount));
        contexts.forEach((path, mapping) -> {
            logger.warning("Context path: %s".formatted(path));
            mapping.forEach((restMethods, method) -> {
                logger.warning("\tContext method: %s".formatted(restMethods));
                logger.warning("\tContext handler: %s".formatted(method.getName()));
            });
            logger.warning("");
        });
    }

    public void start() {
        httpServer.start();
    }

    public void stop(final int delay) {
        httpServer.stop(delay);
    }

    public void stop() {
        this.stop(0);
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public HttpServer getParent() {
        return this.httpServer;
    }
}
