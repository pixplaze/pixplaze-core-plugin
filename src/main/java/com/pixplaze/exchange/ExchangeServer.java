package com.pixplaze.exchange;

import java.util.function.Consumer;

public interface ExchangeServer<S> {
    void start();

    void stop();

    S provide();

    void mount(Consumer<S> routesConfig);
}
