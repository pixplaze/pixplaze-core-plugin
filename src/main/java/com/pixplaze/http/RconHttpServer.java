package com.pixplaze.http;

import com.pixplaze.exceptions.HttpServerException;
import com.pixplaze.exceptions.InvalidAddressException;
import com.pixplaze.exceptions.UnableToDefineLocalAddress;
import com.pixplaze.plugin.PixplazeRootsAPI;
import com.pixplaze.util.Inet;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.*;
import java.util.logging.Logger;

public class RconHttpServer {

    public Logger logger = PixplazeRootsAPI.getInstance().getLogger();
    private final HttpServer httpServer;
    private final String address;
    private final int port;

    public RconHttpServer(String address, int port) throws  InvalidAddressException,
                                                            UnableToDefineLocalAddress,
                                                            HttpServerException {
        if (address == null || address.isEmpty() || address.equalsIgnoreCase("auto"))
            address = Inet.getLocalAddress();
        else if (!Inet.isIpV4Valid(address))
            throw new InvalidAddressException("Invalid ipv4 address: %s!".formatted(address));

        this.address = address;
        this.port = port;

        try {
            httpServer = HttpServer.create(new InetSocketAddress(this.address, this.port), 0);
            httpServer.createContext("/rcon", new RconHttpHandler());
        } catch (IOException e) {;
            throw new HttpServerException(
                    "Can not create pixplaze core api server on address: %s:%d".formatted(address, port));
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
