package com.pixplaze.auth;

import com.pixplaze.api.ext.data.auth.Authority;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/// Access token that passed the signature, audience and expiration checks.
///
/// @param authority  roles, source and audience from the token
/// @param subject    `sub`: UUID of the player for a plugin access token; something else for a system token
/// @param playerName `mc.name`: name of the `sub` player; `null` — not in the token (system token, profile
///                   without a player here, or a token web-api issued before the claim appeared)
/// @param expiresAt  `exp`
public record VerifiedToken(Authority authority, String subject, String playerName, Instant expiresAt) {

    /// Token issued to a web user (plugin access token): source AAD.
    public boolean isUser() {
        return authority.from(Authority.Source.APPLICATION_AUTHORIZED_DEVICE);
    }

    /// Token web-api issued to itself (`RSYS`, source SAD).
    public boolean isSystem() {
        return authority.from(Authority.Source.SYSTEM_AUTHORIZED_DEVICE) && authority.is(Authority.Role.SYSTEM);
    }

    /// Player the user acts as on this server; empty — the profile has no player here (owner without one).
    public Optional<UUID> playerUuid() {
        try {
            return Optional.ofNullable(subject).map(UUID::fromString);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
