package com.pixplaze.http;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class RconHttpServer {

    private HttpServer httpServer;

    public RconHttpServer(int port) {
        try {
            httpServer = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            httpServer.createContext("/rcon", new RconHttpHandler());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void start() {
        httpServer.start();
    }
}
