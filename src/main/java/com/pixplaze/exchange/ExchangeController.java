package com.pixplaze.exchange;

public interface ExchangeController<T extends ExchangeServer<?>> {
    void register(T instance);
}
