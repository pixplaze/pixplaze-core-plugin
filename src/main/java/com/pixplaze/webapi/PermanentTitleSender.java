package com.pixplaze.webapi;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

public class PermanentTitleSender {

    private final JavaPlugin plugin;
    // Храним активные таймеры для каждого игрока по его UUID
    private final HashMap<UUID, BukkitRunnable> timers = new HashMap<>();
    private final Consumer<Player> action;

    public PermanentTitleSender(JavaPlugin plugin, Consumer<Player> action) {
        this.plugin = plugin;
        this.action = action;
    }

    /**
     * Запуск постоянного напоминания для игрока
     */
    public void start(Player player) {
        UUID playerUUID = player.getUniqueId();

        // На всякий случай проверяем, нет ли уже запущенного таймера для этого игрока
        if (timers.containsKey(playerUUID)) {
            return;
        }

        // Создаем новый повторяющийся таск
        BukkitRunnable timer = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    timers.remove(playerUUID);
                    return;
                }

                action.accept(player);
            }
        };

        timers.put(playerUUID, timer);

        timer.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Метод для отключения таймера (вызывать при успешном /login или /register)
     */
    public void stop(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (timers.containsKey(playerUUID)) {
            timers.get(playerUUID).cancel();
            timers.remove(playerUUID);

            player.resetTitle();
        }
    }
}
