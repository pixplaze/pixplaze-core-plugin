package com.pixplaze.webapi;

import com.pixplaze.api.ext.OAuthException;
import com.pixplaze.api.ext.data.auth.AuthorizationTokenInfo;
import com.pixplaze.api.ext.data.auth.MinecraftPlayerAuthorizationDetails;
import com.pixplaze.api.ext.data.oauth.OAuthError;
import com.pixplaze.plugin.PixplazeCorePlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
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

    /// Result of the join check, delivered on the main thread.
    public enum JoinCheck {
        /// The player may play: alive token, refreshed token, or stored tokens while web-api is unreachable.
        AUTHORIZED,
        /// No tokens, revoked tokens or a new IP on a non-licensed server: a confirmation is needed.
        LOGIN_REQUIRED
    }

    /// Outcome of a device flow started by {@link #startLogin}, delivered on the main thread.
    public interface SignInEvents {
        void onSignedIn(UUID player);

        /// web-api is unreachable: the flow could not start.
        void onUnavailable(UUID player);
    }

    private static final long REFRESH_MARGIN_SECONDS = 60;

    private final PixplazeCorePlugin plugin;
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
    }

    /// Receiver of device flow outcomes (the authorization gate freezes and releases players by them).
    public void setEvents(SignInEvents events) {
        this.events = events;
    }

    /// Main thread: the player joined — the file is created or updated, the token checked.
    ///
    /// @param checkIp       non-licensed server: the IP must match the confirmed one
    /// @param untrustedIp   the IP is not the player's own (proxy without IP forwarding): every session needs a confirmation
    /// @param result        receives the outcome on the main thread
    public void onJoin(Player player, boolean checkIp, boolean untrustedIp, Consumer<JoinCheck> result) {
        final var id = player.getUniqueId();
        final var name = player.getName();
        final var ip = ipOf(player);
        executor.execute(() -> {
            final var check = checkOnJoin(id, name, checkIp ? ip : null, untrustedIp);
            runOnMainThread(() -> result.accept(check));
        });
    }

    /// Main thread: the player left — the device flow is no longer needed.
    public void onQuit(Player player) {
        Optional.ofNullable(pending.remove(player.getUniqueId())).ifPresent(future -> future.cancel(true));
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
                .ipAddress(ipOf(player))
                .isOperator(player.isOp())
                .build();
        // Placeholder until the flow starts: a second command while web-api answers must not start another one
        pending.put(player.getUniqueId(), new CompletableFuture<>());
        executor.execute(() -> authorize(details));
    }

    /// Cancels all device flows (plugin disable).
    void stop() {
        pending.values().forEach(future -> future.cancel(true));
        pending.clear();
    }

    private void authorize(MinecraftPlayerAuthorizationDetails details) {
        final var id = details.uuid();
        try {
            final var flow = api.authorization().player().authorize(details);
            final var response = flow.response();
            sendLink(id, response.verificationUriComplete(), response.userCode());
            final var future = flow.await(executor);
            if (pending.replace(id, future) == null) {
                future.cancel(true); // Left while the flow was starting
                return;
            }
            future.whenComplete((tokens, error) -> onAuthorized(details, tokens, error));
        } catch (OAuthException | WebApiException e) {
            pending.remove(id);
            logger.warning("Cannot start Pixplaze sign-in of " + details.username() + ": " + e.getMessage());
            message(id, ChatColor.RED + "Pixplaze is unavailable, try again later.");
            runOnMainThread(() -> events.onUnavailable(id));
        }
    }

    private void onAuthorized(MinecraftPlayerAuthorizationDetails details, AuthorizationTokenInfo tokens, Throwable error) {
        final var id = details.uuid();
        pending.remove(id);
        if (error == null) {
            final var record = loadOrCreate(id, details.username());
            save(record.signedIn(details.ipAddress(), tokens.accessToken(), tokens.refreshToken()));
            logger.info(details.username() + " signed in to Pixplaze");
            message(id, ChatColor.GREEN + "You are signed in to Pixplaze.");
            runOnMainThread(() -> events.onSignedIn(id));
            return;
        }

        final var cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        if (cause instanceof CancellationException) {
            return;
        }
        if (cause instanceof OAuthException oauth && oauth.error() == OAuthError.ACCESS_DENIED) {
            message(id, ChatColor.RED + "Sign-in denied. If this player is linked to another Pixplaze profile, only that profile can confirm it.");
        } else if (cause instanceof OAuthException oauth && oauth.error() == OAuthError.EXPIRED_TOKEN) {
            message(id, ChatColor.YELLOW + "The sign-in link expired: run /pixplaze login again.");
        } else {
            logger.warning("Pixplaze sign-in of " + details.username() + " failed: " + cause.getMessage());
            message(id, ChatColor.RED + "Sign-in failed, try again later.");
        }
    }

    /// @param ip IP to match against the confirmed one; {@code null} — not checked (licensed server)
    private JoinCheck checkOnJoin(UUID id, String name, String ip, boolean untrustedIp) {
        var record = loadOrCreate(id, name);
        if (!name.equals(record.username())) {
            record = record.withUsername(name);
        }
        if (!record.hasTokens()) {
            save(record);
            return JoinCheck.LOGIN_REQUIRED;
        }
        if (ip != null && (untrustedIp || !ip.equals(record.confirmedIp()))) {
            save(record);
            return JoinCheck.LOGIN_REQUIRED;
        }
        if (isAlive(record.accessToken())) {
            save(record);
            return JoinCheck.AUTHORIZED;
        }
        try {
            final var tokens = api.authorization().player().refresh(record.refreshToken());
            // Single-use refresh token: the new pair is on disk before anything uses it
            save(record.withTokens(tokens.accessToken(), tokens.refreshToken()));
            return JoinCheck.AUTHORIZED;
        } catch (OAuthException e) {
            save(record.withoutTokens());
            logger.info("Pixplaze access of " + name + " is revoked (" + e.getMessage() + ")");
            return JoinCheck.LOGIN_REQUIRED;
        } catch (WebApiException e) {
            // web-api unreachable: the stored pair (and the same IP) is enough, the next join tries again
            save(record);
            logger.warning("Cannot refresh Pixplaze tokens of " + name + ": " + e.getMessage());
            return JoinCheck.AUTHORIZED;
        }
    }

    private static String ipOf(Player player) {
        final var address = player.getAddress();
        return address == null || address.getAddress() == null ? null : address.getAddress().getHostAddress();
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

    private static boolean isAlive(String accessToken) {
        try {
            return JwtPayload.of(accessToken).expiresAt() - Instant.now().getEpochSecond() > REFRESH_MARGIN_SECONDS;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void sendLink(UUID id, String url, String userCode) {
        runOnMainThread(() -> Optional.ofNullable(plugin.getServer().getPlayer(id))
                .ifPresent(player -> ConfirmationLinks.send(player, "Confirm the sign-in in Pixplaze", url, userCode)));
    }

    private void message(UUID id, String text) {
        runOnMainThread(() -> Optional.ofNullable(plugin.getServer().getPlayer(id)).ifPresent(player -> player.sendMessage(text)));
    }

    private void runOnMainThread(Runnable task) {
        if (plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }
}
