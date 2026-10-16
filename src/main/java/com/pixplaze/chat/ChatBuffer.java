package com.pixplaze.chat;

import com.pixplaze.api.ext.data.message.MinecraftMessageInfo;
import com.pixplaze.api.ext.data.message.MinecraftMessageInfo.Type;
import com.pixplaze.message.MessageBuffer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.BroadcastMessageEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.UUID;

/// Сообщения, которые видны всем в чате Minecraft: источник — события Bukkit.
///
/// Слушает с приоритетом {@link EventPriority#MONITOR}, то есть после всех плагинов, и пропускает
/// отменённые события: сообщение, которое заглушил другой плагин, в чат не попало. Сообщение
/// с урезанным списком получателей (локальный чат, канал, рассылка по праву) — не для всех,
/// его тоже пропускаем.
///
/// Не ловятся: `/tellraw` и сообщения, которые плагины шлют игрокам по одному через
/// {@code sendMessage}, — у них нет событий. Личные сообщения (`/msg`) не ловятся намеренно.
public class ChatBuffer extends MessageBuffer implements Listener {

    private static final int HISTORY_CAPACITY = 256;

    private final Plugin plugin;
    private boolean attached;

    public ChatBuffer(Plugin plugin) {
        super(HISTORY_CAPACITY);
        this.plugin = plugin;
    }

    @Override
    public void attach() {
        if (attached) {
            return;
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        attached = true;
    }

    @Override
    public void detach() {
        if (attached) {
            HandlerList.unregisterAll(this);
            attached = false;
        }

        super.detach();
    }

    /// Сообщение игрока в оформлении сервера, чтобы web-app мог его раскрасить: автор — отображаемое
    /// имя (с префиксами и цветами плагинов чата), текст — строка целиком, как её видят игроки.
    /// Оформление плагинов, которые рисуют чат через Paper {@code AsyncChatEvent}, здесь не видно.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!isForEveryone(event.getRecipients())) {
            return;
        }

        final var player = event.getPlayer();
        final var displayName = player.getDisplayName();
        add(new MinecraftMessageInfo(
                Type.CHAT,
                displayName,
                player.getUniqueId(),
                formatChat(event.getFormat(), displayName, event.getMessage()),
                System.currentTimeMillis()));
    }

    /// Строка чата по формату сервера ({@code <%1$s> %2$s}); формат с ошибкой — только текст сообщения.
    private String formatChat(String format, String displayName, String message) {
        try {
            return String.format(format, displayName, message);
        } catch (IllegalFormatException e) {
            plugin.getLogger().fine("Invalid chat format '%s': %s".formatted(format, e.getMessage()));
            return message;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        add(Type.JOIN, event.getPlayer(), event.getJoinMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        add(Type.QUIT, event.getPlayer(), event.getQuitMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        add(Type.DEATH, event.getEntity(), event.getDeathMessage());
    }

    /// {@code Bukkit.broadcastMessage} от плагинов. {@code broadcast(message, permission)} шлёт
    /// только обладателям права — не всем, отсекается проверкой получателей.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBroadcast(BroadcastMessageEvent event) {
        if (!isForEveryone(event.getRecipients())) {
            return;
        }

        add(new MinecraftMessageInfo(Type.BROADCAST, null, null, event.getMessage(), System.currentTimeMillis()));
    }

    /// `/say` и `/me` игрока. Событие приходит до проверки прав, поэтому без права команда не
    /// выполнится — и в чат её не кладём.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        // У игрока команда начинается с «/»
        onCommand(event.getPlayer(), event.getMessage().substring(1));
    }

    /// `/say` и `/me` консоли, командного блока и веб-клиентов (они идут через это же событие).
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        onCommand(event.getSender(), event.getCommand());
    }

    private void onCommand(CommandSender sender, String command) {
        final var parts = command.strip().split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            return;
        }

        final var type = switch (label(parts[0])) {
            case "say" -> Type.SAY;
            case "me" -> Type.ME;
            default -> null;
        };
        if (type == null || !sender.hasPermission("minecraft.command." + label(parts[0]))) {
            return;
        }

        // Консоль в чате — сам сервер, у прочих отправителей (командный блок) — их имя
        final var author = sender instanceof ConsoleCommandSender ? null : sender.getName();
        final var authorUuid = sender instanceof Player player ? player.getUniqueId() : null;
        add(new MinecraftMessageInfo(type, author, authorUuid, parts[1], System.currentTimeMillis()));
    }

    /// Метка команды без пространства имён: {@code minecraft:say} → {@code say}.
    private static String label(String command) {
        return command.substring(command.indexOf(':') + 1).toLowerCase(Locale.ROOT);
    }

    /// Сообщение пустое ({@code null} — плагин его убрал) — в чате его нет, пропускаем.
    private void add(Type type, Player player, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        final UUID uuid = player.getUniqueId();
        add(new MinecraftMessageInfo(type, player.getName(), uuid, text, System.currentTimeMillis()));
    }

    /// Получатели — все игроки онлайн.
    private static boolean isForEveryone(Collection<? extends CommandSender> recipients) {
        return recipients.containsAll(Bukkit.getOnlinePlayers());
    }
}
