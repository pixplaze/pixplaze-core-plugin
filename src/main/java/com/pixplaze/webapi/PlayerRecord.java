package com.pixplaze.webapi;

import java.util.UUID;

/// File of a player (п. 38 of the authorization design), created for every player who joins, with or
/// without tokens. Absent tokens are not written.
///
/// @param id           player UUID, as in the web-api database
/// @param username     last known nickname
/// @param confirmedIp  IP the player confirmed the Pixplaze sign-in from
/// @param accessToken  player access token (`aud` — web-api gateway): for actions in web-api on the player's behalf
/// @param refreshToken single-use refresh token
record PlayerRecord(UUID id, String username, String confirmedIp, String accessToken, String refreshToken) {

    static PlayerRecord of(UUID id, String username) {
        return new PlayerRecord(id, username, null, null, null);
    }

    boolean hasTokens() {
        return accessToken != null && refreshToken != null;
    }

    PlayerRecord withUsername(String next) {
        return new PlayerRecord(id, next, confirmedIp, accessToken, refreshToken);
    }

    PlayerRecord withTokens(String access, String refresh) {
        return new PlayerRecord(id, username, confirmedIp, access, refresh);
    }

    PlayerRecord signedIn(String ip, String access, String refresh) {
        return new PlayerRecord(id, username, ip, access, refresh);
    }

    PlayerRecord withoutTokens() {
        return new PlayerRecord(id, username, confirmedIp, null, null);
    }
}
