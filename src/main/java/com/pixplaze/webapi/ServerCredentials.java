package com.pixplaze.webapi;

/// Secrets of the registered server, stored apart from `config.yml` (configs get shared).
///
/// @param minecraftServerId Pixplaze server id (claim `mc.sid`); kept after the tokens are lost, for re-authorization
/// @param accessToken       server access token; `null` until issued
/// @param refreshToken      single-use refresh token; `null` — re-authorization is needed
record ServerCredentials(Long minecraftServerId, String accessToken, String refreshToken) {

    boolean hasTokens() {
        return accessToken != null && refreshToken != null;
    }

    ServerCredentials withoutTokens() {
        return new ServerCredentials(minecraftServerId, null, null);
    }
}
