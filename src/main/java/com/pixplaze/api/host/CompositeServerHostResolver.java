package com.pixplaze.api.host;

import java.util.List;
import java.util.Optional;

/// Цепочка резолверов: перебирает делегатов по порядку и возвращает первый непустой хост.
///
/// Позволяет комбинировать источники по приоритету — например, конфиг (авторитетно)
/// поверх автоопределения по подключениям.
public final class CompositeServerHostResolver implements ServerHostResolver {

    private final List<ServerHostResolver> delegates;

    public CompositeServerHostResolver(ServerHostResolver... delegates) {
        this.delegates = List.of(delegates);
    }

    @Override
    public Optional<HostAddress> resolve() {
        for (final var delegate : delegates) {
            final var host = delegate.resolve();
            if (host.isPresent()) {
                return host;
            }
        }
        return Optional.empty();
    }

    @Override
    public String source() {
        return delegates.stream().map(ServerHostResolver::source).toList().toString();
    }
}
