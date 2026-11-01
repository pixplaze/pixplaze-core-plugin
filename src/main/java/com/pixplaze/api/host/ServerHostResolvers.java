package com.pixplaze.api.host;

import com.pixplaze.plugin.PixplazeCorePlugin;
import com.pixplaze.plugin.PluginConfig;

/// Фабрика {@link ServerHostResolver}: выбирает реализацию в зависимости от условий окружения.
///
/// Логика выбора:
/// 1. задан {@code public.host} в конфиге -> {@link ConfiguredServerHostResolver}
///    (авторитетно, слушать подключения не нужно);
/// 2. иначе -> {@link ConfiguredServerHostResolver} поверх {@link ConnectionFrequencyHostResolver}
///    (конфиг всё ещё победит, если его зададут позже; иначе — автоопределение по подключениям).
///    Частотный резолвер регистрируется как listener.
///
/// Если при этом детектится прокси, а конфиг пуст — печатает предупреждение: за Velocity/без
/// forwarding автоопределение хост не найдёт, и {@code public.host} придётся задать вручную.
public final class ServerHostResolvers {

    public static final String CONFIG_PATH = PluginConfig.PUBLIC_HOST;

    private ServerHostResolvers() {
    }

    public static ServerHostResolver create(PixplazeCorePlugin plugin) {
        final var configured = new ConfiguredServerHostResolver(plugin, CONFIG_PATH);

        if (configured.resolve().isPresent()) {
            plugin.getLogger().info("Server host source: %s = %s"
                    .formatted(configured.source(), configured.resolve().orElseThrow()));
            return configured;
        }

        if (ProxyDetector.isProxied()) {
            plugin.getLogger().warning(("Server appears to be behind a proxy but '%s' is not set in config.yml. "
                    + "Automatic host detection via handshake is unreliable behind proxies (especially Velocity). "
                    + "Set '%s' explicitly. Falling back to best-effort detection by player connections.")
                    .formatted(CONFIG_PATH, CONFIG_PATH));
        }

        final var frequency = new ConnectionFrequencyHostResolver(plugin);
        plugin.getServer().getPluginManager().registerEvents(frequency, plugin);
        plugin.getLogger().info("Server host source: config('%s') over auto-detection by player connections."
                .formatted(CONFIG_PATH));

        return new CompositeServerHostResolver(configured, frequency);
    }
}
