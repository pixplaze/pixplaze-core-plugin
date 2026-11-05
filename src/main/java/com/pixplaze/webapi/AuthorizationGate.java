package com.pixplaze.webapi;

import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.scheduler.BukkitTask;

import java.net.InetAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mandatory Pixplaze sign-in (`authorization.enabled`, п. 33–37 of the authorization design): a player without
 * a valid token is frozen — no movement, blocks, interaction, inventory, damage either way, drops, pickups,
 * vehicles, teleports, chat or commands except {@code /pixplaze login} — and kicked after the timeout. With
 * {@code authorization.auto-login} the sign-in starts by itself on join (an operator on a server that is not
 * connected yet starts the server connection), otherwise the player types the command.
 *
 * <p>Everyone is frozen on join until the token check answers. A non-licensed server also checks the IP; when
 * the player's IP is not their own (a proxy without IP forwarding) every session needs a confirmation. A second
 * join with the nickname of an authorized player is refused, so nobody can kick the real one.
 *
 * <p>web-api unreachable: a licensed server lets everyone in; a non-licensed one keeps frozen those who need a
 * confirmation, players with stored tokens and the same IP play.
 */
public final class AuthorizationGate implements Listener, PlayerAuthorization.SignInEvents {

    private static final String LOGIN_COMMAND = "/pixplaze";
    private static final Set<PlayerTeleportEvent.TeleportCause> PLAYER_TELEPORTS = Set.of(
            PlayerTeleportEvent.TeleportCause.ENDER_PEARL,
            PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT,
            PlayerTeleportEvent.TeleportCause.NETHER_PORTAL,
            PlayerTeleportEvent.TeleportCause.END_PORTAL,
            PlayerTeleportEvent.TeleportCause.END_GATEWAY);
    private static final long TICKS_PER_SECOND = 20;

    private final PixplazeCorePlugin plugin;
    private final PixplazeConnection connection;
    private final PlayerAuthorization authorization;
    private final boolean licensed;
    private final Duration timeout;
    private final boolean autoLogin;

    private final Set<UUID> frozen = ConcurrentHashMap.newKeySet();
    /// Nicknames of authorized players online, lower case → UUID: read on the async pre-login thread
    private final Map<String, UUID> authorizedNames = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> kicks = new ConcurrentHashMap<>();
    private boolean untrustedIpWarned;

    public AuthorizationGate(
            PixplazeCorePlugin plugin,
            PixplazeConnection connection,
            boolean licensed,
            Duration timeout,
            boolean autoLogin
    ) {
        this.plugin = plugin;
        this.connection = connection;
        this.authorization = connection.players();
        this.licensed = licensed;
        this.timeout = timeout;
        this.autoLogin = autoLogin;
        authorization.setEvents(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED
                && authorizedNames.containsKey(event.getName().toLowerCase(Locale.ROOT))) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "This player is already on the server");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        frozen.add(player.getUniqueId());
        authorization.onJoin(player, isAuthorized -> {
            if (!player.isOnline()) {
                return;
            }
            if (isAuthorized) {
                release(player);
            } else {
                requireLogin(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        frozen.remove(player.getUniqueId());
        cancelKick(player.getUniqueId());
        authorizedNames.remove(player.getName().toLowerCase(Locale.ROOT), player.getUniqueId());
        authorization.onQuit(player);
    }

    @Override
    public void onSignedIn(UUID id) {
        Optional.ofNullable(plugin.getServer().getPlayer(id)).ifPresent(this::release);
    }

    @Override
    public void onUnavailable(UUID id) {
        final var player = plugin.getServer().getPlayer(id);
        if (player == null) {
            return;
        }
        if (licensed) {
            // The licensed server restricts nobody while Pixplaze is down
            release(player);
        } else {
            player.sendMessage(ChatColor.YELLOW + "Pixplaze is unavailable: you cannot play until the sign-in is confirmed. Try /pixplaze login later.");
        }
    }

    private void requireLogin(Player player) {
        if (autoLogin) {
            player.sendMessage(ChatColor.YELLOW + "Sign in to Pixplaze to play on this server.");
            final var id = player.getUniqueId();
            connection.startLogin(player, message -> {
                if (plugin.isEnabled()) {
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> Optional.ofNullable(plugin.getServer().getPlayer(id)).ifPresent(online -> online.sendMessage(message)));
                }
            });
        } else {
            player.sendMessage(ChatColor.YELLOW + "Sign in to Pixplaze to play on this server: /pixplaze login");
        }
        if (!timeout.isZero()) {
            final var id = player.getUniqueId();
            kicks.put(id, plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                kicks.remove(id);
                final var online = plugin.getServer().getPlayer(id);
                if (online != null && frozen.contains(id)) {
                    online.kickPlayer("Sign in to Pixplaze and join again");
                }
            }, timeout.toSeconds() * TICKS_PER_SECOND));
        }
    }

    private void release(Player player) {
        cancelKick(player.getUniqueId());
        authorizedNames.put(player.getName().toLowerCase(Locale.ROOT), player.getUniqueId());
        if (frozen.remove(player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Signed in to Pixplaze, have a good game.");
        }
    }

    private void cancelKick(UUID id) {
        Optional.ofNullable(kicks.remove(id)).ifPresent(BukkitTask::cancel);
    }

    private boolean isFrozen(Object entity) {
        return entity instanceof Player player && frozen.contains(player.getUniqueId());
    }

    private void cancelIfFrozen(Object entity, Cancellable event) {
        if (isFrozen(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        final Location from = event.getFrom();
        final Location to = event.getTo();
        if (to != null && isFrozen(event.getPlayer())
                && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
            // Looking around is allowed, moving is not
            final var still = from.clone();
            still.setYaw(to.getYaw());
            still.setPitch(to.getPitch());
            event.setTo(still);
        }
    }

    /// Teleports the player causes (pearls, chorus, portals). Plugin teleports stay — e.g. to spawn on join;
    /// commands are blocked anyway.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (PLAYER_TELEPORTS.contains(event.getCause())) {
            cancelIfFrozen(event.getPlayer(), event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        cancelIfFrozen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        cancelIfFrozen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        cancelIfFrozen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        cancelIfFrozen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        cancelIfFrozen(event.getWhoClicked(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        cancelIfFrozen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        cancelIfFrozen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        cancelIfFrozen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        cancelIfFrozen(event.getEntity(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent event) {
        cancelIfFrozen(event.getEntity(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamaging(EntityDamageByEntityEvent event) {
        cancelIfFrozen(event.getDamager(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVehicle(VehicleEnterEvent event) {
        cancelIfFrozen(event.getEntered(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.YELLOW + "Sign in to Pixplaze first: /pixplaze login");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!isFrozen(event.getPlayer())) {
            return;
        }
        final var command = event.getMessage().split(" ", 2)[0].toLowerCase(Locale.ROOT);
        if (!command.equals(LOGIN_COMMAND)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.YELLOW + "Sign in to Pixplaze first: /pixplaze login");
        }
    }
}
