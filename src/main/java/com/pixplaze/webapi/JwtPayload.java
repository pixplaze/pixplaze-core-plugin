package com.pixplaze.webapi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pixplaze.api.ext.data.auth.Authority;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/// Unverified read of our own access token payload: the plugin received the token from web-api over
/// TLS and needs only its `exp` and `mc.sid`. Verification of incoming tokens is a separate concern.
final class JwtPayload {

    private final JsonObject claims;

    private JwtPayload(JsonObject claims) {
        this.claims = claims;
    }

    static JwtPayload of(String jwt) {
        final var parts = jwt.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Not a JWT");
        }
        final var json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return new JwtPayload(JsonParser.parseString(json).getAsJsonObject());
    }

    /// Expiration, epoch seconds.
    long expiresAt() {
        return claims.get("exp").getAsLong();
    }

    /// Pixplaze server id from `mc.sid`.
    Long minecraftServerId() {
        final var mc = claims.getAsJsonObject(Authority.Claims.MINECRAFT_CONTEXT);
        return mc == null || !mc.has(Authority.Claims.Minecraft.SERVER_ID)
                ? null
                : mc.get(Authority.Claims.Minecraft.SERVER_ID).getAsLong();
    }
}
