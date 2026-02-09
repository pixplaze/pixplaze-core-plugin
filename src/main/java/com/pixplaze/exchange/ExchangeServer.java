package com.pixplaze.exchange;

public interface ExchangeServer<S> {
    void start();

    void stop();

    S provide();
}
