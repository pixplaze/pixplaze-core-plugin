package com.pixplaze.http;

import com.pixplaze.plugin.PixplazeRootsAPI;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Logger;

public class RconHttpServer {

    private HttpServer httpServer;
    public Logger logger = PixplazeRootsAPI.getInstance().getLogger();

    public final String hostname;

    public RconHttpServer(int port) {
        String address = "";

        try {
            address = getLocalAddress();
        } catch (RuntimeException | SocketException e) {
            logger.warning("Unable to define host address!");
            logger.warning(e.getMessage());
            address = "localhost";
        }

        hostname = address;

        try {
            logger.warning("Starting PixplazeCore on: " + hostname);

            httpServer = HttpServer.create(new InetSocketAddress(hostname, port), 0);
            httpServer.createContext("/rcon", new RconHttpHandler());
        } catch (IOException e) {;
            logger.warning(e.getMessage());
        }

    }

    public String verboseInterfaces() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        StringBuilder stringBuilder = new StringBuilder();
        while (interfaces.hasMoreElements()) {
            Enumeration<InetAddress> addresses = interfaces.nextElement().getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                stringBuilder
                        .append(address.getHostAddress())
                        .append("SiteLocalAddress: ")
                        .append(address.isSiteLocalAddress())
                        .append("\n");
            }
        }
        return stringBuilder.toString();
    }

    // TODO: Протестировать работу метода на RedHat Linux
    public static String getLocalAddress() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            Enumeration<InetAddress> addresses = interfaces.nextElement().getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address.isSiteLocalAddress())
                    return address.getHostAddress();
            }
        }
        // TODO: Сделать проприетарное исключение
        throw new RuntimeException("Unable to define local address!");
    }

    public void start() {
        httpServer.start();
    }
}
