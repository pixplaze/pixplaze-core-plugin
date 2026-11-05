package com.pixplaze.webapi;

import com.pixplaze.api.dao.MinecraftServerDao;
import com.pixplaze.api.ext.OAuthException;
import com.pixplaze.api.ext.PendingAuthorization;
import com.pixplaze.api.ext.data.auth.MinecraftServerAuthorizationDetails;
import com.pixplaze.api.ext.data.auth.VerifiableAuthorizationTokenInfo;
import com.pixplaze.api.ext.data.oauth.OAuthError;
import com.pixplaze.api.ext.data.server.MinecraftServerHeartbeatInfo;
import com.pixplaze.api.ext.data.server.MinecraftServerHeartbeatResponse;
import com.pixplaze.api.ext.data.server.MinecraftServerInfo;
import com.pixplaze.auth.KeySource;
import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connection of the server to Pixplaze: registration through the device flow, the server token pair
 * and the heartbeat.
 *
 * <p>All network calls and all access to the credentials run on one thread ({@link #executor}): the
 * refresh token is single-use, so two refreshes at once would revoke the whole chain. The new pair is
 * written to disk before the new access token is used. Bukkit API is read on the main thread only.
 *
 * <p>The player snapshot travels by hash: the list goes only when its hash differs from the last delivered
 * one (and always first after start); if web-api lost its copy, it asks for the list and the heartbeat is
 * repeated with it at once.
 *
 * <p>Without stored tokens the plugin registers: the first time with the bid code the admin put into the server
 * MOTD — Pixplaze finds the bid by the code and takes the game address from it, so the plugin need not know its
 * public address; by the stored server id (re-authorization, any operator confirms) after the tokens are lost.
 * A failed device flow is not restarted automatically: every restart shows a new code to a human.
 */
public final class PixplazeConnection implements KeySource {

    private static final long REFRESH_MARGIN_SECONDS = 60;
    private static final long TICKS_PER_SECOND = 20;
    private static final long HEARTBEAT_PERIOD_TICKS = 60 * TICKS_PER_SECOND;
    private static final long FIRST_TICK_DELAY_TICKS = 5 * TICKS_PER_SECOND;

    private final PixplazeCorePlugin plugin;
    private final Logger logger;
    private final HttpPixplazeWebApi api;
    private final ServerCredentialsStore store;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        final var thread = new Thread(runnable, "Pixplaze web-api");
        thread.setDaemon(true);
        return thread;
    });

    private final PlayerAuthorization players;
    private MinecraftServerDao dao;
    private BukkitTask heartbeatTask;

    /// Called on the network thread when the server has tokens: after start and after authorization.
    private volatile Runnable onConnected = () -> {};

    // Written on the executor thread only; volatile — {@link #serverId()} reads it from Jetty threads
    private volatile ServerCredentials credentials;
    private volatile CompletableFuture<VerifiableAuthorizationTokenInfo> pendingAuthorization;
    /// A refusal or an unconfirmed link: no attempts until `/pixplaze connect` (or a restart)
    private boolean gaveUp;
    /// credentials.json cannot be read: `/pixplaze connect` must not overwrite it
    private boolean credentialsBroken;
    /// Link of the server confirmation while it waits; operators who ask to connect get it again
    private volatile ServerLink serverLink;

    private record ServerLink(String url, String userCode) {
    }
    /// MOTD whose codes were already tried: a new attempt only after the admin changes it
    private String triedMotd;
    /// Hash of the last player list web-api accepted (2xx); {@code null} after start — the list goes in full.
    private String deliveredPlayersHash;

    private PixplazeConnection(PixplazeCorePlugin plugin, String webApiUrl) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.api = new HttpPixplazeWebApi(webApiUrl);
        this.store = new ServerCredentialsStore(plugin.getDataFolder().toPath().resolve("credentials.json"));
        this.players = new PlayerAuthorization(plugin, api, executor, this::serverId);
    }

    /// Sign-in of players: shares the network thread, so player and server refreshes never overlap.
    public PlayerAuthorization players() {
        return players;
    }

    /// Connection by `pixplaze.gateway` from the config; empty — the URL is not set, the integration is off.
    public static Optional<PixplazeConnection> create(PixplazeCorePlugin plugin) {
        final var url = plugin.getPixplazeConfig().gateway().path();
        if (url.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new PixplazeConnection(plugin, url.trim()));
    }

    /// Loads the credentials and starts the heartbeat (every minute, first tick shortly after start).
    public void start() {
        executor.execute(this::loadCredentials);
        heartbeatTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::heartbeatTask, FIRST_TICK_DELAY_TICKS, HEARTBEAT_PERIOD_TICKS);
    }

    /// Pixplaze id of this server; empty until it is registered.
    @Override
    public Optional<Long> serverId() {
        return Optional.ofNullable(credentials).map(ServerCredentials::minecraftServerId);
    }

    /// Fetches web-api JWKS on the network thread, with a fresh server access token.
    @Override
    public CompletableFuture<String> fetchJwks() {
        return CompletableFuture.supplyAsync(() -> {
            if (credentials == null || !credentials.hasTokens()) {
                throw new IllegalStateException("The server has no Pixplaze tokens yet");
            }
            try {
                return api.fetchJwks(accessToken());
            } catch (WebApiException e) {
                if (e.isUnauthorized() && refreshTokens()) {
                    return api.fetchJwks(credentials.accessToken());
                }
                throw e;
            }
        }, executor);
    }

    /// The server has tokens: it is registered and the plugin can talk to web-api.
    public boolean isConnected() {
        final var current = credentials;
        return current != null && current.hasTokens();
    }

    /// `/pixplaze connect`, or an operator's `/pixplaze login` on a server without tokens: a new attempt after a
    /// refusal or an unconfirmed link, without restarting the server. {@code reply} gets the outcome on the network
    /// thread. The link to confirm and later outcomes go to the console and to operators online
    /// ({@link #report}); {@code requester} — the player who asked (null for the console) gets a waiting link again.
    public void connect(Consumer<String> reply, UUID requester) {
        executor.execute(() -> {
            final var link = serverLink;
            if (isConnected()) {
                reply.accept("The server is already connected to Pixplaze.");
            } else if (credentialsBroken) {
                reply.accept("credentials.json cannot be read: fix or delete it and restart the server.");
            } else if (pendingAuthorization != null && !pendingAuthorization.isDone()) {
                reply.accept("The server connection waits for confirmation.");
                if (link != null && requester != null) {
                    onMainThread(() -> Optional.ofNullable(plugin.getServer().getPlayer(requester))
                            .ifPresent(player -> ConfirmationLinks.send(player, "Confirm the server in Pixplaze", link.url(), link.userCode())));
                }
            } else {
                gaveUp = false;
                triedMotd = null;
                reply.accept("Connecting the server to Pixplaze...");
                ensureAuthorization();
            }
        });
    }

    /// Outcome of the server connection: to the console and to operators online — they can act on it in game.
    private void report(Level level, String message) {
        logger.log(level, message);
        final var color = level.intValue() >= Level.WARNING.intValue() ? ChatColor.YELLOW : ChatColor.GRAY;
        toOperators(player -> player.sendMessage(color + "[Pixplaze] " + message));
    }

    private void toOperators(Consumer<Player> action) {
        onMainThread(() -> plugin.getServer().getOnlinePlayers().stream().filter(Player::isOp).forEach(action));
    }

    private void onMainThread(Runnable task) {
        if (plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    /// Sign-in of a player ({@code /pixplaze login}, auto-login on join). An operator on a server that is not
    /// connected yet starts the server connection instead: nobody can sign in before it. {@code reply} — as in
    /// {@link #connect}.
    public void startLogin(Player player, Consumer<String> reply) {
        if (player.isOp() && !isConnected()) {
            connect(reply, player.getUniqueId());
            return;
        }
        players.startLogin(player);
    }

    /// Callback for the moment the server has tokens (keys of the token verifier are loaded then).
    public void onConnected(Runnable callback) {
        this.onConnected = callback;
    }

    /// Stops the heartbeat, the pending device flow and the network thread.
    public void stop() {
        Optional.ofNullable(heartbeatTask).ifPresent(BukkitTask::cancel);
        Optional.ofNullable(pendingAuthorization).ifPresent(future -> future.cancel(true));
        players.stop();
        executor.shutdownNow();
    }

    /// Main thread: reads the heartbeat body from Bukkit API and hands it to the network thread.
    private void heartbeatTask() {
        final var heartbeat = dao().getHeartbeatInfo();
        executor.execute(() -> onTick(heartbeat));
    }

    private void onTick(MinecraftServerHeartbeatInfo heartbeat) {
        if (credentials == null || !credentials.hasTokens()) {
            ensureAuthorization();
            return;
        }
        try {
            sendHeartbeat(heartbeat);
        } catch (WebApiException e) {
            logger.warning("Heartbeat failed: " + e.getMessage());
        }
    }

    /// Heartbeat with the list only when the snapshot changed; web-api lost its copy — again with the list.
    private void sendHeartbeat(MinecraftServerHeartbeatInfo snapshot) {
        final var withList = !snapshot.playersHash().equals(deliveredPlayersHash);
        var response = send(withList ? snapshot : withoutList(snapshot));
        if (response != null && !withList && Boolean.TRUE.equals(response.isPlayersRequired())) {
            response = send(snapshot);
            if (response != null) {
                deliveredPlayersHash = snapshot.playersHash();
            }
        } else if (response != null && withList) {
            deliveredPlayersHash = snapshot.playersHash();
        }
    }

    private MinecraftServerHeartbeatResponse send(MinecraftServerHeartbeatInfo heartbeat) {
        try {
            return api.heartbeat(accessToken(), heartbeat);
        } catch (WebApiException e) {
            if (!e.isUnauthorized()) {
                throw e;
            }
            // One refresh and one retry, not a refresh on every tick
            return refreshTokens() ? api.heartbeat(credentials.accessToken(), heartbeat) : null;
        }
    }

    private static MinecraftServerHeartbeatInfo withoutList(MinecraftServerHeartbeatInfo snapshot) {
        return MinecraftServerHeartbeatInfo.of(snapshot.isLicense(), snapshot.state(), snapshot.playersHash());
    }

    /// Access token, refreshed shortly before its `exp`.
    private String accessToken() {
        final var expiresAt = JwtPayload.of(credentials.accessToken()).expiresAt();
        if (expiresAt - Instant.now().getEpochSecond() < REFRESH_MARGIN_SECONDS) {
            refreshTokens();
        }
        return credentials.accessToken();
    }

    /// Rotates the token pair. {@code false} — web-api rejected the refresh token (revoked, expired,
    /// server banned): the tokens are dropped and the next tick starts re-authorization.
    private boolean refreshTokens() {
        try {
            final var tokens = api.authorization().server().refresh(credentials.refreshToken());
            // The refresh token is single-use: the new pair is on disk before anything uses it
            saveCredentials(new ServerCredentials(credentials.minecraftServerId(), tokens.accessToken(), tokens.refreshToken()));
            return true;
        } catch (OAuthException e) {
            logger.warning("Refresh token rejected by Pixplaze (" + e.getMessage() + "): the server must be authorized again");
            saveCredentials(credentials.withoutTokens());
            return false;
        }
    }

    /// Starts the device flow if it is not running and has not failed: registration by `invite-code`
    /// or re-authorization by the stored server id.
    private void ensureAuthorization() {
        if (gaveUp || (pendingAuthorization != null && !pendingAuthorization.isDone())) {
            return;
        }

        final var serverInfo = callOnMainThread(() -> dao().getServerInfo(true));
        if (serverInfo == null) {
            return;
        }

        final var serverId = credentials == null ? null : credentials.minecraftServerId();
        final var codes = serverId == null ? InviteCodes.candidates(serverInfo.motd()) : List.<String>of();
        if (serverId == null && (codes.isEmpty() || Objects.equals(triedMotd, serverInfo.motd()))) {
            if (!Objects.equals(triedMotd, serverInfo.motd())) {
                triedMotd = serverInfo.motd();
                report(Level.WARNING, "No Pixplaze bid code in the server MOTD: put the code of your bid into motd in"
                        + " server.properties and restart the server (the code can be removed after registration), or run"
                        + " /pixplaze connect after changing the MOTD by a plugin");
            }
            return;
        }

        // Re-authorization replaces stored addresses: only complete ones are sent. Registration may send an API
        // entry without the address — Pixplaze fills it from the bid.
        final var hosts = serverInfo.hosts().stream()
                .filter(host -> serverId == null || (host.address() != null && !host.address().isBlank()))
                .toList();
        final var registrationInfo = MinecraftServerInfo.builder(serverInfo).hosts(hosts).build();
        final var attempts = serverId == null
                ? codes.stream().map(code -> new MinecraftServerAuthorizationDetails(code, registrationInfo)).toList()
                : List.of(new MinecraftServerAuthorizationDetails(null, MinecraftServerInfo.builder()
                        .id(serverId)
                        .hosts(hosts.isEmpty() ? null : hosts)
                        .isLicense(serverInfo.isLicense())
                        .build()));

        try {
            final var pending = startAuthorization(attempts);
            // The codes of this MOTD got an answer: the next attempt waits for another MOTD (network errors retry)
            triedMotd = serverInfo.motd();
            if (pending == null) {
                report(Level.WARNING, "None of " + codes + " in the server MOTD is a code of an open Pixplaze bid:"
                        + " check the code in the web app, fix the MOTD and restart the server");
                return;
            }
            final var response = pending.response();
            final var link = new ServerLink(ConfirmationLinks.clickable(response.verificationUriComplete()), response.userCode());
            serverLink = link;
            logger.info("Confirm the server in Pixplaze: " + link.url()
                    + " (code " + link.userCode() + ", valid " + response.expiresIn() + " s)");
            toOperators(player -> ConfirmationLinks.send(player, "Confirm the server in Pixplaze", link.url(), link.userCode()));
            pendingAuthorization = pending.await(executor);
            pendingAuthorization.whenComplete((tokens, error) -> {
                serverLink = null;
                onAuthorized(serverId, tokens, error);
            });
        } catch (OAuthException e) {
            if (e.error() == OAuthError.SERVER_ERROR) {
                report(Level.WARNING, "Pixplaze failed to start the server authorization, retrying in a minute: " + e.getMessage());
                return;
            }
            gaveUp = true;
            report(Level.SEVERE, "Pixplaze rejected the server authorization request: " + e.getMessage()
                    + ". Fix it and run /pixplaze connect");
        } catch (WebApiException e) {
            logger.warning("Cannot start the server authorization: " + e.getMessage());
        }
    }

    /// Starts the device flow with the first details web-api accepts: the MOTD may hold words that only look like
    /// a code. {@code null} — every candidate was rejected as a wrong code.
    private PendingAuthorization<VerifiableAuthorizationTokenInfo> startAuthorization(
            List<MinecraftServerAuthorizationDetails> attempts) {
        for (final var details : attempts) {
            try {
                return api.authorization().server().authorize(details);
            } catch (OAuthException e) {
                // A wrong code is access_denied; anything else (or re-authorization) is not about the code — stop
                if (e.error() != OAuthError.ACCESS_DENIED || details.inviteCode() == null) {
                    throw e;
                }
            }
        }
        return null;
    }

    private void onAuthorized(Long knownServerId, VerifiableAuthorizationTokenInfo tokens, Throwable error) {
        if (error == null) {
            final var serverId = Optional.ofNullable(JwtPayload.of(tokens.accessToken()).minecraftServerId()).orElse(knownServerId);
            saveCredentials(new ServerCredentials(serverId, tokens.accessToken(), tokens.refreshToken()));
            report(Level.INFO, "The server is connected to Pixplaze (id " + serverId + ")");
            notifyConnected();
            return;
        }

        final var cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        if (cause instanceof CancellationException) {
            return;
        }
        if (cause instanceof OAuthException oauth && oauth.error() == OAuthError.ACCESS_DENIED) {
            gaveUp = true;
            report(Level.SEVERE, "Pixplaze denied the server authorization. The first registration is confirmed by the owner"
                    + " of the bid, re-authorization by the owner or an operator; run /pixplaze connect to get a new link");
        } else if (cause instanceof OAuthException oauth && oauth.error() == OAuthError.EXPIRED_TOKEN) {
            gaveUp = true;
            report(Level.WARNING, "The authorization link expired before it was confirmed: run /pixplaze connect to get a new one");
        } else {
            // A failure of web-api or the network, not a decision: the next tick tries again with the same MOTD codes
            triedMotd = null;
            logger.log(Level.WARNING, "Server authorization failed: " + cause.getMessage(), cause);
            toOperators(player -> player.sendMessage(ChatColor.YELLOW + "[Pixplaze] Server authorization failed, retrying in a minute"));
        }
    }

    private void loadCredentials() {
        try {
            credentials = store.load().orElse(null);
            if (credentials != null && credentials.hasTokens()) {
                notifyConnected();
            }
        } catch (IllegalStateException e) {
            gaveUp = true;
            credentialsBroken = true;
            logger.severe(e.getMessage() + ": fix or delete the file and restart the server");
        }
    }

    /// Off the network thread: the callback may wait for {@link #fetchJwks()}, which runs on it.
    private void notifyConnected() {
        CompletableFuture.runAsync(onConnected);
    }

    private void saveCredentials(ServerCredentials next) {
        store.save(next);
        credentials = next;
    }

    private MinecraftServerDao dao() {
        if (dao == null) {
            dao = new MinecraftServerDao();
        }
        return dao;
    }

    private <T> T callOnMainThread(Callable<T> call) {
        try {
            return plugin.getServer().getScheduler().callSyncMethod(plugin, call).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            logger.log(Level.WARNING, "Cannot read the server state: " + e.getCause().getMessage(), e.getCause());
            return null;
        }
    }
}
