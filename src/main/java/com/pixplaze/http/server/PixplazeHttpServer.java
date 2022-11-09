package com.pixplaze.http.server;

import com.pixplaze.exceptions.HttpServerException;
import com.pixplaze.exceptions.InvalidAddressException;
import com.pixplaze.exceptions.CannotDefineAddressException;
import com.pixplaze.http.HttpController;
import com.pixplaze.http.server.validation.HandlerValidationStrategy;
import com.pixplaze.http.server.validation.ReturnAnyStrategy;
import com.pixplaze.plugin.PixplazeCorePlugin;
import com.pixplaze.util.Inet;
//import com.pixplaze.util.Optional;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.*;
import java.util.logging.Logger;

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
     * <br><br>
     * Если указать параметр {@code validationStrategy}, - метод проверит корректность
     * переданного {@code controller} в соответствии c указанной {@code validationStrategy}.
     * Если не передавать {@code validationStrategy}, будет использована ReturnAnyStrategy
     * (по умолчанию).
     *
     * @param controller объект контроллера, который должен быть привязан к HTTP-серверу.
     */
    public void mount(HttpController controller, HandlerValidationStrategy validationStrategy) {
        var contextMapper = new ContextMapper(controller, validationStrategy);

        contextMapper.getContextMapping().forEach((context, mapping) -> httpServer.createContext(
                context, new PixplazeHttpHandler(this.logger, context, mapping, controller))
        );

        this.logger.warning("HTTP contexts mapping:\n" + contextMapper);
    }

    /**
     * @see PixplazeHttpServer#mount(HttpController, HandlerValidationStrategy)
     * @param controller объект контроллера, который должен быть привязан к HTTP-серверу.
     */
    public void mount(HttpController controller) {
        this.mount(controller, new ReturnAnyStrategy());
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
