package com.pixplaze.webapi;

import com.pixplaze.api.ext.DeviceAuthorization;
import com.pixplaze.api.ext.DeviceAuthorizationClient;
import com.pixplaze.api.ext.PixplazeWebApi;
import com.pixplaze.api.ext.data.auth.AuthorizationTokenInfo;
import com.pixplaze.api.ext.data.auth.MinecraftPlayerAuthorizationDetails;
import com.pixplaze.api.ext.data.auth.MinecraftServerAuthorizationDetails;
import com.pixplaze.api.ext.data.auth.VerifiableAuthorizationTokenInfo;
import com.pixplaze.api.ext.data.server.MinecraftServerHeartbeatInfo;
import com.pixplaze.api.ext.data.server.MinecraftServerHeartbeatResponse;

/// {@link PixplazeWebApi} over HTTP. Every call is blocking: never call it from the server main thread.
public final class HttpPixplazeWebApi implements PixplazeWebApi {

    private final WebApiHttp http;
    private final DeviceAuthorization authorization;

    public HttpPixplazeWebApi(String baseUrl) {
        this.http = new WebApiHttp(baseUrl);
        final var server = new HttpDeviceAuthorizationClient<MinecraftServerAuthorizationDetails, VerifiableAuthorizationTokenInfo>(
                http, "MAD:MINECRAFT_SERVER", VerifiableAuthorizationTokenInfo.class);
        final var player = new HttpDeviceAuthorizationClient<MinecraftPlayerAuthorizationDetails, AuthorizationTokenInfo>(
                http, "MAD:MINECRAFT_PLAYER", AuthorizationTokenInfo.class);
        this.authorization = new DeviceAuthorization() {
            @Override
            public DeviceAuthorizationClient<MinecraftServerAuthorizationDetails, VerifiableAuthorizationTokenInfo> server() {
                return server;
            }

            @Override
            public DeviceAuthorizationClient<MinecraftPlayerAuthorizationDetails, AuthorizationTokenInfo> player() {
                return player;
            }
        };
    }

    @Override
    public DeviceAuthorization authorization() {
        return authorization;
    }

    /// JWKS of web-api (`GET /auth/oauth/keys`): public keys to verify incoming tokens, raw JSON.
    ///
    /// @param accessToken server access token
    public String fetchJwks(String accessToken) {
        return http.getRaw("/auth/oauth/keys", accessToken);
    }

    /// @throws WebApiException with {@link WebApiException#isUnauthorized()} if the access token is rejected
    @Override
    public MinecraftServerHeartbeatResponse heartbeat(String accessToken, MinecraftServerHeartbeatInfo heartbeat) {
        return http.postJson("/servers/heartbeat", accessToken, heartbeat, MinecraftServerHeartbeatResponse.class);
    }
}
