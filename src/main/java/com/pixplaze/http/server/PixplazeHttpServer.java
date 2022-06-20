package com.pixplaze.http.server;

import com.google.gson.Gson;
import com.pixplaze.exceptions.HttpServerException;
import com.pixplaze.exceptions.InvalidAddressException;
import com.pixplaze.exceptions.CannotDefineAddressException;
import com.pixplaze.http.HttpController;
import com.pixplaze.http.exceptions.BadMethodException;
import com.pixplaze.http.exceptions.HttpException;
import com.pixplaze.plugin.PixplazeRootsAPI;
import com.pixplaze.util.Inet;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
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
            try {
                this.handle(controller, context, method, mapping, exchange, params);
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
    private void handle(HttpController controller,
                        String context,
                        String method,
                        Map<String, Method> mapping,
                        HttpExchange exchange,
                        QueryParams params) throws BadMethodException
    {
        class ResponseBuilder {
            private final Gson gson = new Gson();
            private final Charset charset = StandardCharsets.UTF_8;

            private Integer code;
            private Throwable error;

            private String message;
            private byte[] body;

            public ResponseBuilder(Integer code, Throwable error, String message, Object body) {
                this.code = code;
                this.error = error;
                this.message = message;
                this.body = gson.toJson(body).getBytes(charset);
            }

            public Integer getCode() {
                return code;
            }

            public ResponseBuilder setCode(Integer code) {
                this.code = code;
                return this;
            }

            public Throwable getError() {
                return error;
            }

            public ResponseBuilder setError(Throwable error) {
                this.error = error;
                return this;
            }

            public ResponseBuilder setError(HttpException error) {
                this.error = error.getCause();
                this.code = error.getStatus().getCode();
                this.message = error.getStatus().getMessage();
                return this;
            }

            public String getMessage() {
                return message;
            }

            public ResponseBuilder setMessage(String message) {
                this.message = message;
                return this;
            }

            public byte[] getBody() {
                return body;
            }

            public ResponseBuilder setBody(byte[] body) {
                this.body = body;
                return this;
            }

            public ResponseBuilder setBody(Object body) {
                this.body = gson.toJson(body).getBytes(charset);
                return this;
            }

            public int getLength() {
                return this.body.length;
            }

            public byte[] toBytes() {
                if ()
            }
        }

        byte[] body;
        try {
            var handler = mapping.get(method);
            if (handler != null) {
                var gson = new Gson();
                var result = handler.invoke(controller, exchange, params);

                body = gson.toJson(result).getBytes(StandardCharsets.UTF_8);

                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } else {
                throw new BadMethodException(method, context);
            }
        } catch (HttpException e) {
            record BadResponse(int code, Throwable error, String message) {}

            var code = e.getStatus();
            var error = e.getCause();
            var message = e.getMessage();

        } catch (Throwable e) {

        }
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
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
