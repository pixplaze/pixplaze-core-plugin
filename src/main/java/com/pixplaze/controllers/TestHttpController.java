package com.pixplaze.controllers;

import com.pixplaze.http.HttpController;
import com.pixplaze.http.annotations.GetHandler;
import com.pixplaze.http.server.QueryParams;
import com.pixplaze.http.server.ResponseBuilder;
import com.sun.net.httpserver.HttpExchange;

import java.util.UUID;

public class TestHttpController implements HttpController {
    record Player(UUID uuid, String username, int rating) {}

    @GetHandler("/player")
    public ResponseBuilder handlePlayer(HttpExchange exchange, QueryParams params) {
        var response = new ResponseBuilder();
        var player = new Player(UUID.randomUUID(), "Emberati", 10);

        response.append(player);

        return response;
    }
}
