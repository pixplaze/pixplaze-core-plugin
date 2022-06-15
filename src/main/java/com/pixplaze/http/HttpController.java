package com.pixplaze.http;
;
import com.sun.net.httpserver.HttpExchange;

public interface HttpController {
	default void beforeEach(HttpExchange exchange) {}
}
