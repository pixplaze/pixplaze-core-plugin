package com.pixplaze.api.host;

import java.util.Optional;

/// Стратегия определения публичного хоста, по которому игроки подключаются к серверу.
///
/// Сам Minecraft-сервер свой внешний домен не знает, поэтому источники разные
/// (конфиг, частота адресов из handshake и т.п.). Конкретная реализация выбирается
/// фабрикой {@link ServerHostResolvers} в зависимости от условий окружения.
public interface ServerHostResolver {

    /// @return публичный адрес (домен и, если источник его знает, внешний порт), либо {@link Optional#empty()},
    ///         если данный источник адрес определить не смог.
    Optional<HostAddress> resolve();

    /// Человекочитаемое имя источника — для логов/диагностики.
    default String source() {
        return getClass().getSimpleName();
    }
}
