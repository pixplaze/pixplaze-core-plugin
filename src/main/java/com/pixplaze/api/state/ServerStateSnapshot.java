package com.pixplaze.api.state;

import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/// Снимок состояния сервера, который обновляется из главного потока по таймеру.
///
/// Нужен потому, что Bukkit API не потокобезопасен, а состояние спрашивают обработчики
/// Javalin из потоков Jetty. Вместо того чтобы на каждый HTTP-запрос идти в главный поток
/// через {@code callSyncMethod} и блокировать поток запроса, значения считаются раз в секунду
/// синхронной задачей и складываются в {@code volatile}-поля, которые HTTP-слой просто читает.
///
/// Сейчас здесь только медианный пинг; сюда же стоит переносить остальное состояние,
/// которое сегодня читается из потоков Jetty напрямую.
public final class ServerStateSnapshot {

    /// Период обновления в тиках. Чаще смысла нет: сервер сам пересчитывает пинг игрока
    /// примерно раз в секунду.
    private static final long REFRESH_PERIOD_TICKS = 20L;

    private final PixplazeCorePlugin plugin;

    /// Медианный пинг игроков в миллисекундах. {@code null} — значение ещё ни разу не
    /// считалось (с момента запуска на сервер никто не заходил).
    private volatile Long medianPing;

    private BukkitTask refreshTask;

    public ServerStateSnapshot(PixplazeCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (refreshTask != null) {
            return;
        }

        refreshTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::refresh, REFRESH_PERIOD_TICKS, REFRESH_PERIOD_TICKS);
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    /// Медианный пинг игроков. Медиана, а не среднее: один игрок с плохим каналом не должен
    /// портить показатель за весь сервер. {@code null} только до первого расчёта.
    public Long getMedianPing() {
        return medianPing;
    }

    /// Пересчёт. Выполняется в главном потоке: {@code getOnlinePlayers()} возвращает живое
    /// представление, а {@link Player#getPing()} читает состояние соединения.
    ///
    /// Если игроков нет, значение **не трогаем** — пусть остаётся последнее известное,
    /// иначе при пустом сервере пинг обнулялся бы и читался как идеальная связь.
    private void refresh() {
        final var pings = plugin.getServer().getOnlinePlayers().stream()
                .mapToInt(Player::getPing)
                .sorted()
                .toArray();

        if (pings.length == 0) {
            return;
        }

        medianPing = median(pings);
    }

    /// Медиана отсортированного массива: середина для нечётной длины, среднее двух
    /// серединных значений для чётной.
    static long median(int[] sorted) {
        final var middle = sorted.length / 2;

        return sorted.length % 2 == 1
                ? sorted[middle]
                : Math.round((sorted[middle - 1] + sorted[middle]) / 2.0);
    }
}
