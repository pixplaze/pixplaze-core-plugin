package com.pixplaze.auth;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/// Where {@link PluginTokenVerifier} takes this server's id and web-api signing keys from.
public interface KeySource {

    /// Pixplaze id of this server: tokens must be addressed to it; empty — not registered yet.
    Optional<Long> serverId();

    /// JWKS of web-api, raw JSON.
    CompletableFuture<String> fetchJwks();
}
