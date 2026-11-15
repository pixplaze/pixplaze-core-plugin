package com.pixplaze.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.AbstractSerializer;
import io.jsonwebtoken.security.Jwks;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// "Cannot check yet" ({@link VerifierNotReadyException}: 503 / close 1013, the client waits) must not be confused with
/// "bad token" (401 / close 4401, the client exchanges a new one): mixing them up made web clients exchange tokens in a loop.
class PluginTokenVerifierTest {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Date.class, (JsonSerializer<Date>) (date, type, context) ->
                    new JsonPrimitive(date.getTime() / 1000))
            .create();
    private static final Logger LOGGER = Logger.getLogger(PluginTokenVerifierTest.class.getName());

    private final KeyPair keyPair = Jwts.SIG.ES256.keyPair().build();

    @Test
    void serverNotRegisteredIsNotReady() {
        final var verifier = new PluginTokenVerifier(Optional.of(new FakeSource(null, () -> jwks("k1"))), LOGGER);

        assertThrows(VerifierNotReadyException.class, () -> verifier.verify(token("k1", 1)));
    }

    @Test
    void keysFailingToLoadIsNotReady() {
        final var verifier = new PluginTokenVerifier(Optional.of(new FakeSource(1L, () -> {
            throw new IllegalStateException("web-api is down");
        })), LOGGER);

        assertThrows(VerifierNotReadyException.class, () -> verifier.verify(token("k1", 1)));
    }

    @Test
    void unknownKidAfterFreshReloadIsInvalid() {
        final var verifier = new PluginTokenVerifier(Optional.of(new FakeSource(1L, () -> jwks("k1"))), LOGGER);

        final var error = assertThrows(InvalidTokenException.class, () -> verifier.verify(token("other", 1)));
        assertFalse(error instanceof VerifierNotReadyException, "Fresh keys lack the kid: the token is bad");
    }

    @Test
    void unknownKidWithinReloadIntervalIsNotReady() throws Exception {
        final var fetches = new AtomicInteger();
        final var verifier = new PluginTokenVerifier(Optional.of(new FakeSource(1L, () -> {
            fetches.incrementAndGet();
            return jwks("k1");
        })), LOGGER);
        verifier.reloadKeys();

        // Keys were just loaded: the new kid may be a rotation web-api did after that
        assertThrows(VerifierNotReadyException.class, () -> verifier.verify(token("k2", 1)));
        assertEquals(1, fetches.get(), "Random kids must not trigger a reload within the interval");
    }

    @Test
    void wrongAudienceIsInvalid() {
        final var verifier = new PluginTokenVerifier(Optional.of(new FakeSource(1L, () -> jwks("k1"))), LOGGER);

        final var error = assertThrows(InvalidTokenException.class, () -> verifier.verify(token("k1", 2)));
        assertFalse(error instanceof VerifierNotReadyException);
    }

    @Test
    void noTokenIsInvalid() {
        final var verifier = new PluginTokenVerifier(Optional.empty(), LOGGER);

        final var error = assertThrows(InvalidTokenException.class, () -> verifier.verify(" "));
        assertInstanceOf(InvalidTokenException.class, error);
        assertFalse(error instanceof VerifierNotReadyException);
    }

    private String token(String kid, long serverId) {
        return Jwts.builder()
                .json(new GsonSerializer())
                .header().keyId(kid).and()
                .subject("00000000-0000-0000-0000-000000000001")
                .audience().add("mc:server:" + serverId).and()
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(keyPair.getPrivate(), Jwts.SIG.ES256)
                .compact();
    }

    private String jwks(String kid) {
        final var jwk = Jwks.builder().key((java.security.interfaces.ECPublicKey) keyPair.getPublic()).id(kid).build();
        return GSON.toJson(Map.of("keys", List.of(Map.copyOf(jwk))));
    }

    private record FakeSource(Long id, java.util.function.Supplier<String> jwks) implements KeySource {
        @Override
        public Optional<Long> serverId() {
            return Optional.ofNullable(id);
        }

        @Override
        public CompletableFuture<String> fetchJwks() {
            return CompletableFuture.supplyAsync(jwks);
        }
    }

    private static final class GsonSerializer extends AbstractSerializer<Map<String, ?>> {
        @Override
        protected void doSerialize(Map<String, ?> value, OutputStream out) throws Exception {
            try (var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                GSON.toJson(value, writer);
            }
        }
    }
}
