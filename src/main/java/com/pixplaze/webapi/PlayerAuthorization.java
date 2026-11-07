package com.pixplaze.webapi;

import com.pixplaze.api.ext.OAuthException;
import com.pixplaze.api.ext.data.auth.AuthorizationTokenInfo;
import com.pixplaze.api.ext.data.auth.MinecraftPlayerAuthorizationDetails;
import com.pixplaze.api.ext.data.oauth.OAuthError;
import com.pixplaze.config.PixplazeConfig;
import com.pixplaze.plugin.PixplazeCorePlugin;
import com.pixplaze.util.AddressResolver;
import com.pixplaze.util.ServerUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Sign-in of players to Pixplaze (п. 34, 38–42 of the authorization design): the device flow with a link
 * in chat, the player file with the token pair, and the token check on join.
 *
 * <p>Runs on the network thread of {@link PixplazeConnection}: refreshes of one player never overlap, and the
 * new pair is written before it is used. Bukkit API (player data, messages) is touched on the main thread.
 *
 * <p>On join an alive access token is enough; an expired one is refreshed. A rejected refresh (unlinked,
 * banned or deleted profile) drops the tokens from the file — that is how revocation reaches the plugin.
 * On a non-licensed server the nickname proves nothing, so the IP is checked too: another IP than the one
 * the sign-in was confirmed from requires a new confirmation (refresh would not help — the token lies on
 * this server and refreshes for anyone who joins with the nickname).
 */
public final class PlayerAuthorization {

    /// Outcome of a device flow started by {@link #startLogin}, delivered on the main thread.
    public interface SignInEvents {
        void onSignedIn(UUID player);

        /// web-api is unreachable: the flow could not start.
        void onUnavailable(UUID player);
    }

    private static final long REFRESH_MARGIN_SECONDS = 60;

    private final PixplazeCorePlugin plugin;
    private final PixplazeConfig config;
    private final Logger logger;
    private final HttpPixplazeWebApi api;
    private final ExecutorService executor;
    private final Supplier<Optional<Long>> serverId;
    private final PlayerRecordStore store;
    /// Device flows in progress: one per player
    private final Map<UUID, CompletableFuture<AuthorizationTokenInfo>> pending = new ConcurrentHashMap<>();
    private volatile SignInEvents events = new SignInEvents() {
        @Override
        public void onSignedIn(UUID player) {
        }

        @Override
        public void onUnavailable(UUID player) {
        }
    };

    PlayerAuthorization(PixplazeCorePlugin plugin, HttpPixplazeWebApi api, ExecutorService executor, Supplier<Optional<Long>> serverId) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.api = api;
        this.executor = executor;
        this.serverId = serverId;
        this.store = new PlayerRecordStore(plugin.getDataFolder().toPath().resolve("players"));
        this.config = plugin.getPixplazeConfig();
    }

    /// Receiver of device flow outcomes (the authorization gate freezes and releases players by them).
    public void setEvents(SignInEvents events) {
        this.events = events;
    }

    /// Main thread: the player joined — the file is created or updated, the token checked.
    ///
    /// @param untrustedIp   the IP is not the player's own (proxy without IP forwarding): every session needs a confirmation
    /// @param result        receives the outcome on the main thread
    public void onJoin(Player player, Consumer<Boolean> result) {
        executeAsync(() -> {
            final var check = checkOnJoin(player);
            executeSync(() -> result.accept(check));
        });
    }

    /// Main thread: the player left — the device flow is no longer needed.
    public void onQuit(Player player) {
        Optional.ofNullable(pending.remove(player.getUniqueId()))
                .ifPresent(future -> future.cancel(true));
    }

    /// Main thread: `/pixplaze login` — starts the device flow and sends the link to chat.
    public void startLogin(Player player) {
        if (pending.containsKey(player.getUniqueId())) {
            message(player.getUniqueId(), ChatColor.YELLOW + "Sign-in is already in progress: open the link sent earlier.");
            return;
        }
        final var server = serverId.get();
        if (server.isEmpty()) {
            message(player.getUniqueId(), ChatColor.RED + "This server is not connected to Pixplaze yet: an operator can connect it with /pixplaze connect.");
            return;
        }
        final var details = MinecraftPlayerAuthorizationDetails.builder()
                .minecraftServerId(server.get())
                .uuid(player.getUniqueId())
                .username(player.getName())
                .ipAddress(AddressResolver.ipOf(player))
                .isOperator(player.isOp())
                .build();
        // Placeholder until the flow starts: a second command while web-api answers must not start another one
        pending.put(player.getUniqueId(), new CompletableFuture<>());
        executeAsync(() -> authorize(details));
    }

    /// Cancels all device flows (plugin disable).
    void stop() {
        pending.values().forEach(future -> future.cancel(true));
        pending.clear();
    }

    private void authorize(MinecraftPlayerAuthorizationDetails details) {
        final var playerUuid = details.uuid();
        try {
            final var deviceFlow = api.authorization().player().authorize(details);
            final var deviceResponse = deviceFlow.response();
            sendLink(playerUuid, deviceResponse.verificationUriComplete(), deviceResponse.userCode());
            final var future = deviceFlow.await(executor);
            if (pending.replace(playerUuid, future) == null) {
                future.cancel(true); // Left while the deviceFlow was starting
                return;
            }
            future.whenComplete((tokens, error) -> onAuthorized(details, tokens, error));
        } catch (OAuthException | WebApiException e) {
            pending.remove(playerUuid);
            logger.warning("Cannot start Pixplaze sign-in of " + details.username() + ": " + e.getMessage());
            message(playerUuid, ChatColor.RED + "Pixplaze is unavailable, try again later.");
            executeSync(() -> events.onUnavailable(playerUuid));
        }
    }

    private void onAuthorized(MinecraftPlayerAuthorizationDetails details, AuthorizationTokenInfo tokens, Throwable error) {
        final var playerUuid = details.uuid();
        pending.remove(playerUuid);
        if (error == null) {
            final var record = loadOrCreate(playerUuid, details.username());
            save(record.signedIn(details.ipAddress(), tokens.accessToken(), tokens.refreshToken()));
            logger.info(details.username() + " signed in to Pixplaze");
            message(playerUuid, ChatColor.GREEN + "You are signed in to Pixplaze.");
            executeSync(() -> events.onSignedIn(playerUuid));
            return;
        }

        final var cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        if (cause instanceof CancellationException) {
            return;
        }
        if (cause instanceof OAuthException oauth && oauth.error() == OAuthError.ACCESS_DENIED) {
            message(playerUuid, ChatColor.RED + "Sign-in denied. If this player is linked to another Pixplaze profile, only that profile can confirm it.");
        } else if (cause instanceof OAuthException oauth && oauth.error() == OAuthError.EXPIRED_TOKEN) {
            message(playerUuid, ChatColor.YELLOW + "The sign-in link expired: run /pixplaze login again.");
        } else {
            logger.warning("Pixplaze sign-in of " + details.username() + " failed: " + cause.getMessage());
            message(playerUuid, ChatColor.RED + "Sign-in failed, try again later.");
        }
    }

    private boolean checkOnJoin(Player player) {
        final var playerUuid = player.getUniqueId();
        final var playerName = player.getName();
        final var playerIp = AddressResolver.ipOf(player);
        final var isUntrustedIp = AddressResolver.isUntrusted(player);
        final var shouldCheckIp = config.authorization().enabled() && !ServerUtils.isLicensed(plugin.getServer());

        var session = loadOrCreate(playerUuid, playerName);

        if (!playerName.equals(session.username())) {
            session = session.withUsername(playerName);
        }
        if (!session.hasTokens()) {
            save(session);
            logger.info("Player %s needs a first authorization".formatted(playerName));
            return false;
        }
        if (shouldCheckIp && (isUntrustedIp || isPlayerAddressChanged(playerIp, session))) {
            save(session);
            logger.info(String.format(
                    "Player %s needs re-authorization: " +
                    "joined from untrusted ip (%s)",
                    playerName,
                    playerIp
            ));
            return false;
        }

        if (isSessionAlive(session.accessToken())) {
            save(session);
            final var token = JwtPayload.of(session.accessToken());
            logger.info(String.format(
                    "Player %s is authorized from a live session: expires at %s",
                    playerName,
                    Instant.ofEpochSecond(token.expiresAt()).atZone(ZoneId.systemDefault())
            ));
            return true;
        }
        try {
            final var tokens = api.authorization().player().refresh(session.refreshToken());
            // Single-use refresh token: the new pair is on disk before anything uses it
            save(session.withTokens(tokens.accessToken(), tokens.refreshToken()));
            final var newToken = JwtPayload.of(tokens.accessToken());
            logger.info(String.format(
                    "Player %s is authorized from a refreshed session: expires at %s",
                    playerName,
                    Instant.ofEpochSecond(newToken.expiresAt()).atZone(ZoneId.systemDefault())
            ));
            return true;
        } catch (OAuthException e) {
            save(session.withoutTokens());
            logger.info("Pixplaze access of " + playerName + " is revoked (" + e.getMessage() + ")");
            return false;
        } catch (WebApiException e) {
            // web-api unreachable: the stored pair (and the same IP) is enough, the next join tries again
            save(session);
            logger.warning("Cannot refresh Pixplaze tokens of " + playerName + ": " + e.getMessage());
            return true;
        }
    }

    private PlayerRecord loadOrCreate(UUID id, String name) {
        try {
            return store.load(id).orElseGet(() -> PlayerRecord.of(id, name));
        } catch (IllegalStateException e) {
            logger.warning(e.getMessage() + ": the player file is recreated");
            return PlayerRecord.of(id, name);
        }
    }

    private void save(PlayerRecord record) {
        try {
            store.save(record);
        } catch (RuntimeException e) {
            logger.severe("Cannot save the file of player " + record.username() + ": " + e.getMessage());
        }
    }

    private static boolean isSessionAlive(String accessToken) {
        try {
            return JwtPayload.of(accessToken).expiresAt() - Instant.now().getEpochSecond() > REFRESH_MARGIN_SECONDS;
        } catch (RuntimeException e) {
            return false;
        }
    }
    
    private boolean isPlayerAddressChanged(String ip, PlayerRecord session) {
        return ip != null && !ip.equals(session.confirmedIp());
    }

    private void sendLink(UUID id, String url, String userCode) {
        executeSync(() -> Optional.ofNullable(plugin.getServer().getPlayer(id))
                .ifPresent(player -> {
                    ConfirmationLinks.send(player, "Confirm the sign-in in Pixplaze", url, userCode);
                    player.sendTitle("Unauthorized", "Check the link in chat", 10, 80, 10);
                }));
    }

    private void message(UUID id, String text) {
        executeSync(() -> Optional.ofNullable(plugin.getServer().getPlayer(id))
                .ifPresent(player -> player.sendMessage(text)));
    }

    /// Execute on async thread
    private void executeAsync(Runnable task) {
        executor.execute(task);
    }

    /// Execute on main server thread
    private void executeSync(Runnable task) {
        if (plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }
}
