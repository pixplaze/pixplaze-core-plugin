package com.pixplaze.http;

import com.pixplaze.plugin.PixplazeRootsAPI;
import com.pixplaze.util.Utils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class RconHttpHandler implements HttpHandler {

    private final Logger logger = PixplazeRootsAPI.getInstance().getLogger();

    private static final int MAX_LINES_COUNT = PixplazeRootsAPI.getInstance().getConsoleBuffer().getSize();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String uriPath = exchange.getRequestURI().getPath().split("/")[2];

        if ("GET".equals(exchange.getRequestMethod())) {
            if ("lines".equalsIgnoreCase(uriPath)) {
                handleLinesRequest(exchange);
            }
        }
        if ("POST".equals(exchange.getRequestMethod())) {
            if ("command".equalsIgnoreCase(uriPath)) {
                handleCommandRequest(exchange);
            }
        }
    }

    private void handleLinesRequest(HttpExchange exchange) throws IOException {
        Map<String, String> params;
        ResponseBodyBuilder rb = new ResponseBodyBuilder();

        try {
            params = parseParams(exchange.getRequestURI().getQuery());
        } catch (Exception e) {
            sendResponse(exchange, 400, rb.setError("ParamsParseError").setMessage("Error occurred while parsing params").getFinal());
            return;
        }

        if (!params.containsKey("access-token")) {
            sendResponse(exchange, 401, rb.setError("InvalidTokenError").setMessage("No token!").getFinal());
            return;
        } else if (!Utils.checkToken(params.get("access-token"))) {
            sendResponse(exchange, 401, rb.setError("InvalidTokenError").setMessage("Access token is invalid").getFinal());
            return;
        }

        int count = 0;
        if (!params.containsKey("count")) {
            count = MAX_LINES_COUNT;
        } else {
            count = Integer.parseInt(params.get("count"));
        }

        if (count == 0) {
            sendResponse(exchange, 401, rb.setError("ZeroCountError").setMessage("Param count cannot be zero").getFinal());
            return;
        }

        List<String> lines = PixplazeRootsAPI.getInstance().getConsoleBuffer().getHistory(count);
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("lines", lines);
        rb.setResponse(jsonResponse).setMessage("Command request success");

        sendResponse(exchange, 200, rb.getFinal());
    }

    private void handleCommandRequest(HttpExchange exchange) throws IOException {
        Map<String, String> params;
        ResponseBodyBuilder rb = new ResponseBodyBuilder();

        try {
            params = parseParams(exchange.getRequestURI().getQuery());
        } catch (Exception e) {
            sendResponse(exchange, 400, rb.setError("ParamsParseError").setMessage("Error occurred while parsing params").getFinal());
            return;
        }

        if (!params.containsKey("access-token")) {
            sendResponse(exchange, 401, rb.setError("InvalidTokenError").setMessage("No token!").getFinal());
            return;
        } else if (!Utils.checkToken(params.get("access-token"))) {
            sendResponse(exchange, 401, rb.setError("InvalidTokenError").setMessage("Access token is invalid").getFinal());
            return;
        }

        String line = "";
        if (!params.containsKey("line")) {
            sendResponse(exchange, 401, rb.setError("InvalidParamError").setMessage("No param line").getFinal());
            return;
        } else {
            line = params.get("line");
        }

        if ("".equals(line)) {
            sendResponse(exchange, 401, rb.setError("InvalidParamError").setMessage("line is empty").getFinal());
            return;
        }

        try {
            dispatchCommand(Bukkit.getConsoleSender(), line);
            sendResponse(exchange, 200, rb.setMessage("Success execution").getFinal());
        } catch (Exception e) {
            sendResponse(exchange, 200, rb.setMessage("Failed command execution").getFinal());
        }
    }

    private void sendResponse(HttpExchange exchange, int code, String responseBody) throws IOException {
        OutputStream outputStream = exchange.getResponseBody();
        exchange.sendResponseHeaders(code, responseBody.length());
        outputStream.write(responseBody.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
    }

    private Map<String, String> parseParams(String query) {
        if (query == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else{
                result.put(entry[0], "");
            }
        }
        return result;
    }

    private void dispatchCommand(CommandSender sender, String command) {
        new BukkitRunnable() {
            @Override
            public void run() {
                logger.warning("Dispatching...");
                if (Bukkit.getServer().dispatchCommand(sender, command)) {
                    logger.warning("NO!");
                }
            }
        };
    }
}
