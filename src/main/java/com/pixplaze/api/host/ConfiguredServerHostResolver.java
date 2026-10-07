package com.pixplaze.api.host;

import com.pixplaze.plugin.PixplazeCorePlugin;

import java.util.Optional;

/// Хост из {@code config.yml} — авторитетный, ручной источник.
///
/// Работает для любой топологии (в т.ч. за Velocity/прокси без forwarding, где
/// автоопределение невозможно). Значение читается «вживую», поэтому подхватывается
/// после {@code /reload}/перезагрузки конфига без пересоздания резолвера.
public final class ConfiguredServerHostResolver implements ServerHostResolver {

    private final PixplazeCorePlugin plugin;
    private final String configPath;

    public ConfiguredServerHostResolver(PixplazeCorePlugin plugin, String configPath) {
        this.plugin = plugin;
        this.configPath = configPath;
    }

    @Override
    public Optional<String> resolve() {
        return Optional.ofNullable(plugin.getConfig().getString(configPath))
                .map(String::trim)
                .filter(host -> !host.isBlank());
    }

    @Override
    public String source() {
        return "config('" + configPath + "')";
    }
}
