package com.pixplaze.api.host;

import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Автоопределение хоста по частоте адресов, которые игроки использовали в handshake.
///
/// Слушает {@link PlayerLoginEvent}, нормализует {@code getHostname()} через
/// {@link HostNames} и копит счётчики; {@link #resolve()} возвращает самый частый домен
/// (mode). Покрывает прямые серверы и BungeeCord (legacy forwarding). За Velocity/без
/// forwarding в handshake публичного домена нет — тогда валидных значений не набирается
/// и {@link #resolve()} вернёт {@link Optional#empty()} (нужен {@code server-host} в конфиге).
///
/// Счётчики персистятся в {@code <dataFolder>/host-stats.yml}, чтобы переживать рестарты.
public final class ConnectionFrequencyHostResolver implements ServerHostResolver, Listener {

    private static final String STATS_FILE = "host-stats.yml";
    private static final String KEY_HOSTS = "hostnames";
    private static final String KEY_COUNTS = "hostcounts";

    private final PixplazeCorePlugin plugin;
    private final Map<String, Long> counts = new ConcurrentHashMap<>();

    public ConnectionFrequencyHostResolver(PixplazeCorePlugin plugin) {
        this.plugin = plugin;
        load();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLogin(PlayerLoginEvent event) {
        HostNames.normalize(event.getHostname()).ifPresent(host -> {
            counts.merge(host, 1L, Long::sum);
            saveAsync();
        });
    }

    @Override
    public Optional<String> resolve() {
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    @Override
    public String source() {
        return "connection-frequency";
    }

    private void saveAsync() {
        if (plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
        } else {
            save();
        }
    }

    private synchronized void save() {
        final var yaml = new YamlConfiguration();
        final var hosts = new ArrayList<String>(counts.size());
        final var values = new ArrayList<Long>(counts.size());
        counts.forEach((host, count) -> {
            hosts.add(host);
            values.add(count);
        });
        // Две параллельные строки-списка, чтобы точки в доменах не превращались в YAML-вложенность.
        yaml.set(KEY_HOSTS, hosts);
        yaml.set(KEY_COUNTS, values);

        final var file = new File(plugin.getDataFolder(), STATS_FILE);
        try {
            final var parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save host statistics to " + file + ": " + e.getMessage());
        }
    }

    private void load() {
        final var file = new File(plugin.getDataFolder(), STATS_FILE);
        if (!file.exists()) {
            return;
        }
        final var yaml = YamlConfiguration.loadConfiguration(file);
        final List<String> hosts = yaml.getStringList(KEY_HOSTS);
        final List<Long> values = yaml.getLongList(KEY_COUNTS);
        for (int i = 0; i < hosts.size() && i < values.size(); i++) {
            counts.put(hosts.get(i), values.get(i));
        }
    }
}
