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
 * per {@link #RELOAD_INTERVAL}: random {@code kid} values must not flood web-api. While no keys are loaded
 * (web-api unreachable, server not registered) every token is rejected and the error is logged.
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

    /// @throws InvalidTokenException the token cannot be trusted
    public VerifiedToken verify(String token) throws InvalidTokenException {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("No token");
        }
        final var serverId = source.flatMap(KeySource::serverId)
                .orElseThrow(() -> new InvalidTokenException("The server is not registered in Pixplaze"));
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
        } catch (JwtException | IllegalArgumentException | IllegalStateException e) {
            throw new InvalidTokenException(e.getMessage(), e);
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
        var key = kid == null ? null : keys.get(kid);
        if (key == null) {
            reloadIfAllowed();
            key = kid == null ? null : keys.get(kid);
        }
        if (key == null) {
            throw new UnsupportedJwtException("Unknown key id (kid): " + kid);
        }
        return key;
    }

    private void reloadIfAllowed() {
        synchronized (reloadLock) {
            if (Instant.now().isBefore(lastReload.plus(RELOAD_INTERVAL))) {
                return;
            }
            load();
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
