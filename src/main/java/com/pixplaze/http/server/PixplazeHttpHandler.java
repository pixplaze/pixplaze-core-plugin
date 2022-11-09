package com.pixplaze.http.server;

import com.pixplaze.exceptions.QueryParseException;
import com.pixplaze.http.HttpController;
import com.pixplaze.http.HttpStatus;
import com.pixplaze.http.exceptions.BadMethodException;
import com.pixplaze.http.exceptions.HttpException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class PixplazeHttpHandler implements HttpHandler {
    private final Logger logger;
    private final String context;
    private final Map<String, Method> mapping;
    private final HttpController controller;

    public PixplazeHttpHandler(Logger logger, String context, Map<String, Method> mapping, HttpController controller) {
        this.logger = logger;
        this.context = context;
        this.mapping = mapping;
        this.controller = controller;
    }

    @Override
    public void handle(HttpExchange exchange) {
        var rb = new ResponseBuilder();
        try {
            QueryParams params;
            var requestMethod = exchange.getRequestMethod();
            var requestHandler = Optional.ofNullable(mapping.get(requestMethod))
                    .orElseThrow(() -> new BadMethodException(requestMethod, context));

            try {
                params = new QueryParams(exchange.getRequestURI().getQuery());
            } catch (QueryParseException e) {
                throw new HttpException(HttpStatus.BAD_REQUEST, e);
            }

            controller.beforeEach(exchange);

            var result = requestHandler.invoke(controller, exchange, params);
            Optional.ofNullable(result).ifPresent(rb::append);

            exchange.sendResponseHeaders(rb.getCode(), rb.getLength());
            exchange.getResponseBody().write(rb.toBytes());
        } catch (HttpException e) {
            logger.warning("HttpException");
            rb.append(e);
            logger.warning(e.getMessage());
        } catch (InvocationTargetException e) {
            logger.warning("InvocationTargetException");
            rb.append(e.getCause());
            logger.warning(e.getCause().getMessage());
        } catch (Throwable e) {
            logger.warning("Throwable");
            rb.append(e);
            logger.warning(e.getMessage());
        }

        try {
            exchange.sendResponseHeaders(rb.getCode(), rb.getLength());
            exchange.getResponseBody().write(rb.toBytes());
        } catch (Throwable f) {
            logger.warning(f.getMessage());
            throw new RuntimeException(
                    "Can not serve %s %s request"
                    .formatted(exchange.getRequestMethod(), exchange.getRequestURI())
            );
        }
        exchange.close();
    }
}
