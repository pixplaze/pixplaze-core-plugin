package com.pixplaze.http.server;

import com.pixplaze.exceptions.HttpServerException;
import com.pixplaze.exceptions.InvalidAddressException;
import com.pixplaze.exceptions.CannotDefineAddressException;
import com.pixplaze.http.HttpController;
import com.pixplaze.http.exceptions.BadMethodException;
import com.pixplaze.plugin.PixplazeCorePlugin;
import com.pixplaze.util.Inet;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

import static com.pixplaze.http.HttpStatus.BAD_METHOD;

/**
 * Простой HTTP-сервер, позволяющий обрабатывать запросы посредством контроллера.
 * В отличие от {@link HttpServer}, PixplazeHttpServer имеет возможность
 * делигировать обработку запросов контроллерам, - наследникам {@link HttpController}.
 *
 * Контроллеры имеют возможность обрабатывать запросы раздельно по методам.
 * Некоторые операции, вроде обработки исключений, закрытии потоков ввода-вывода,
 * формирования статус-кода происходят автоматически.
 *
 * @see com.pixplaze.http.HttpController
 * @see com.sun.net.httpserver.HttpServer
 * @see com.sun.net.httpserver.HttpHandler
 *
 * @since v0.1.0-indev
 */
public final class PixplazeHttpServer {

    public Logger logger = PixplazeCorePlugin.getInstance().getLogger();
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
        } catch (IOException e) {
            throw new HttpServerException(
					"Can not create pixplaze core api server on address: %s:%d"
		            .formatted(address, port), e
            );
        }
    }

    /**
     * Метод, позволяющий привязать объект контроллера к HTTP-серверу.
     * Иными словами, метод монтирует контроллер к HTTP-серверу.
     *
     * @param controller объект контроллера, который должен быть привязан к HTTP-серверу.
     */
    public void mount(HttpController controller) {
        var allMethods = controller.getClass().getMethods();
        var contextMapper = ContextMapper.scanHandlers(allMethods);

        contextMapper.getContextMapping().forEach((context, mapping) -> httpServer.createContext(context, exchange -> {
            var method = exchange.getRequestMethod();
            var params = new QueryParams(exchange.getRequestURI().getQuery());
            controller.beforeEach(exchange);
            try {
                this.observe(controller, context, method, mapping, exchange, params);
            } catch (IllegalArgumentException e) {
                // TODO: Реализлвать исключение
                logger.warning(
                        "Illegal handler arguments! Expected: %s, %s!"
                        .formatted(HttpExchange.class.getSimpleName(), QueryParams.class.getSimpleName())
                );
            } catch (BadMethodException e) {
                var message = e.getMessage().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(BAD_METHOD.getCode(), message.length);
                exchange.getResponseBody().write(message);
                exchange.getResponseBody().flush();
            } catch (Throwable e) {
                logger.warning("Error occurred: %s\tMessage: %s".formatted(e.getClass().getSimpleName(), e.getMessage()));
            }
            exchange.close();
        }));

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

    // TODO: Упростить код, уменьшить количество параметров
    private void observe(HttpController controller,
                         String context,
                         String method,
                         Map<String, Method> mapping,
                         HttpExchange exchange,
                         QueryParams params) throws InvocationTargetException,
                                                    IllegalAccessException,
                                                    BadMethodException {
        var handler = mapping.get(method);
        if (handler != null) {
            handler.invoke(controller, exchange, params);
        } else {
            throw new BadMethodException(method, context);
        }

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
}
