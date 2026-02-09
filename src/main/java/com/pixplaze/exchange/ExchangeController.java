package com.pixplaze.exchange;

import io.javalin.router.JavalinDefaultRouting;

public interface ExchangeController<T extends ExchangeServer<?>> {
    void register(JavalinDefaultRouting server);
}
