package com.pixplaze.http.server;

import com.google.gson.Gson;
import com.pixplaze.exceptions.HttpServerException;
import com.pixplaze.exceptions.InvalidAddressException;
import com.pixplaze.exceptions.CannotDefineAddressException;
import com.pixplaze.http.HttpController;
import com.pixplaze.http.HttpStatus;
import com.pixplaze.http.exceptions.BadMethodException;
import com.pixplaze.http.exceptions.HttpException;
import com.pixplaze.plugin.PixplazeRootsAPI;
import com.pixplaze.util.Inet;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.Charset;
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
        var contextMapper = new ContextMapper(allMethods);

        contextMapper.getContextMapping().forEach((context, mapping) -> httpServer.createContext(context, exchange -> {
            var method = exchange.getRequestMethod();
            var params = new QueryParams(exchange.getRequestURI().getQuery());
            controller.beforeEach(exchange);
            this.handle(controller, context, method, mapping, exchange, params);
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
    private void handle(HttpController controller,
                        String context,
                        String method,
                        Map<String, Method> mapping,
                        HttpExchange exchange,
                        QueryParams params) throws BadMethodException
    {
        var rb = new ResponseBuilder();
        try {
            var handler = mapping.get(method);
            if (handler != null) {
                var result = handler.invoke(controller, exchange, params);
                logger.warning(result.toString());
                rb.append(result);
            } else {
                throw new BadMethodException(method, context);
            }
        } catch (HttpException e) {
            rb.append(e);
            logger.warning(e.getMessage());
        } catch (InvocationTargetException e) {
            rb.append(e.getCause());
        } catch (Throwable e) {
            rb.append(e);
            logger.warning(e.fillInStackTrace().getLocalizedMessage());
        }
        makeResponse(exchange, rb);
    }

    private void makeResponse(HttpExchange exchange, ResponseBuilder builder) {
        try {
            exchange.sendResponseHeaders(builder.getCode(), builder.getLength());
            exchange.getResponseBody().write(builder.toBytes());
        } catch (IOException e) {
            throw new RuntimeException(
                    "Can not serve %s %s request"
                    .formatted(exchange.getRequestMethod(), exchange.getRequestURI())
            );
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
