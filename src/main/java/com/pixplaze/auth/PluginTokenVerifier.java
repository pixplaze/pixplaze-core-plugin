package com.pixplaze.auth;

import com.pixplaze.api.ext.data.auth.Authority;
import com.pixplaze.api.ext.data.auth.AuthorityClaims;
import com.pixplaze.api.ext.data.auth.MinecraftServerTargets;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.ProtectedHeader;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Jwks;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Verification of incoming access tokens: ES256 signature by the key from web-api JWKS ({@code kid}),
 * {@code aud} containing this server ({@code mc:server:{id}}), {@code exp}; roles via {@link AuthorityClaims}.
 *
 * <p>Keys are loaded when the server connects to Pixplaze and again on an unknown {@code kid}, at most once
 * per {@link #RELOAD_INTERVAL}: random {@code kid} values must not flood web-api. While the verifier cannot decide
 * — the server is not registered yet, no keys are loaded (web-api unreachable), or an unknown {@code kid} could
 * not be looked up because of the reload interval — it throws {@link VerifierNotReadyException}: the token may
 * be fine, the client retries later with it instead of exchanging a new one in a loop.
 */
public final class PluginTokenVerifier {

    static final Duration RELOAD_INTERVAL = Duration.ofSeconds(30);
    private static final long FETCH_TIMEOUT_SECONDS = 15;

    private final Optional<? extends KeySource> source;
    private final Logger logger;
    private final GsonJsonDeserializer json = new GsonJsonDeserializer();
    private final Object reloadLock = new Object();

    private volatile Map<String, Key> keys = Map.of();
    private volatile Instant lastReload = Instant.EPOCH;

    /// @param source id of this server and the keys; empty — the server is not connected, every token is rejected
    public PluginTokenVerifier(Optional<? extends KeySource> source, Logger logger) {
        this.source = source;
        this.logger = logger;
    }

    /// @throws VerifierNotReadyException the token cannot be checked yet, retry later
    /// @throws InvalidTokenException     the token cannot be trusted
    public VerifiedToken verify(String token) throws InvalidTokenException {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("No token");
        }
        final var serverId = source.flatMap(KeySource::serverId)
                .orElseThrow(() -> new VerifierNotReadyException("The server is not registered in Pixplaze yet"));
        try {
            final var claims = Jwts.parser()
                    .json(json)
                    .keyLocator(new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(ProtectedHeader header) {
                            return keyOf(header.getKeyId());
                        }
                    })
                    .requireAudience(MinecraftServerTargets.of(serverId))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new VerifiedToken(AuthorityClaims.read(claims), claims.getSubject(), playerName(claims),
                    claims.getExpiration().toInstant());
        } catch (KeysNotReady e) {
            throw new VerifierNotReadyException(e.getMessage(), e);
        } catch (JwtException | IllegalArgumentException | IllegalStateException e) {
            if (e.getCause() instanceof KeysNotReady notReady) {
                throw new VerifierNotReadyException(notReady.getMessage(), e);
            }
            throw new InvalidTokenException(e.getMessage(), e);
        }
    }

    /// The key locator cannot tell whether the {@code kid} is unknown: thrown out of jjwt, turned into
    /// {@link VerifierNotReadyException} by {@link #verify}.
    private static final class KeysNotReady extends RuntimeException {
        KeysNotReady(String message) {
            super(message, null, false, false);
        }
    }

    /// `mc.name` of a plugin access token; `null` if absent or not a string.
    private static String playerName(Claims claims) {
        return claims.get(Authority.Claims.MINECRAFT_CONTEXT) instanceof Map<?, ?> mc
                && mc.get(Authority.Claims.Minecraft.PLAYER_NAME) instanceof String name ? name : null;
    }

    /// Loads the keys now, ignoring the reload interval: the server has just connected to Pixplaze.
    public void reloadKeys() {
        synchronized (reloadLock) {
            load();
        }
    }

    private Key keyOf(String kid) {
        if (kid == null) {
            throw new UnsupportedJwtException("No key id (kid)");
        }
        var key = keys.get(kid);
        if (key != null) {
            return key;
        }
        final var reloaded = reloadIfAllowed();
        key = keys.get(kid);
        if (key != null) {
            return key;
        }
        // Unknown only if fresh keys were loaded and lack it; otherwise the keys may just be stale or missing
        if (keys.isEmpty()) {
            throw new KeysNotReady("Pixplaze signing keys are not loaded yet");
        }
        if (!reloaded) {
            throw new KeysNotReady("Unknown key id (kid) " + kid + ", keys were reloaded less than "
                    + RELOAD_INTERVAL.toSeconds() + " s ago");
        }
        throw new UnsupportedJwtException("Unknown key id (kid): " + kid);
    }

    /// Whether this call loaded the keys (successfully or not); {@code false} — the reload interval has not passed.
    private boolean reloadIfAllowed() {
        synchronized (reloadLock) {
            if (Instant.now().isBefore(lastReload.plus(RELOAD_INTERVAL))) {
                return false;
            }
            load();
            return true;
        }
    }

    private void load() {
        lastReload = Instant.now();
        if (source.isEmpty()) {
            return;
        }
        try {
            final var jwkSet = Jwks.setParser().json(json).build()
                    .parse(source.get().fetchJwks().get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            final var loaded = new HashMap<String, Key>();
            jwkSet.getKeys().forEach(jwk -> loaded.put(jwk.getId(), jwk.toKey()));
            keys = Map.copyOf(loaded);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.severe("Cannot load Pixplaze signing keys, tokens are rejected until they load: " + e.getMessage());
        }
    }
}
