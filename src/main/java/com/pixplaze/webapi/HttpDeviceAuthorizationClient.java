package com.pixplaze.webapi;

import com.pixplaze.api.ext.DeviceAuthorizationClient;
import com.pixplaze.api.ext.OAuthException;
import com.pixplaze.api.ext.PendingAuthorization;
import com.pixplaze.api.ext.data.auth.AuthorizationDetails;
import com.pixplaze.api.ext.data.auth.AuthorizationToken;
import com.pixplaze.api.ext.data.oauth.DeviceAuthorizationResponse;
import com.pixplaze.api.ext.data.oauth.OAuthError;

import java.util.LinkedHashMap;
import java.util.Optional;

/// Device flow of one subject kind over HTTP: `POST /auth/oauth/authorize` and `POST /auth/oauth/token`.
final class HttpDeviceAuthorizationClient<D extends AuthorizationDetails, T extends AuthorizationToken>
        implements DeviceAuthorizationClient<D, T> {

    private static final String DEVICE_CODE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";

    private final WebApiHttp http;
    private final String scope;
    private final Class<T> tokenType;

    HttpDeviceAuthorizationClient(WebApiHttp http, String scope, Class<T> tokenType) {
        this.http = http;
        this.scope = scope;
        this.tokenType = tokenType;
    }

    @Override
    public PendingAuthorization<T> authorize(D details) {
        final var clientId = details.clientId();
        final var form = new LinkedHashMap<String, String>();
        form.put("client_id", clientId);
        form.put("scope", scope);
        form.put("authorization_details", WebApiHttp.json.toJson(details));
        final var response = http.postForm("/auth/oauth/authorize", form, DeviceAuthorizationResponse.class);
        return new Pending(clientId, response);
    }

    @Override
    public T refresh(String refreshToken) {
        final var form = new LinkedHashMap<String, String>();
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", refreshToken);
        return http.postForm("/auth/oauth/token", form, tokenType);
    }

    private final class Pending implements PendingAuthorization<T> {
        private final String clientId;
        private final DeviceAuthorizationResponse response;

        private Pending(String clientId, DeviceAuthorizationResponse response) {
            this.clientId = clientId;
            this.response = response;
        }

        @Override
        public DeviceAuthorizationResponse response() {
            return response;
        }

        @Override
        public Optional<T> poll() {
            final var form = new LinkedHashMap<String, String>();
            form.put("grant_type", DEVICE_CODE_GRANT);
            form.put("client_id", clientId);
            form.put("device_code", response.deviceCode());
            try {
                return Optional.of(http.postForm("/auth/oauth/token", form, tokenType));
            } catch (OAuthException e) {
                if (e.error() == OAuthError.AUTHORIZATION_PENDING) {
                    return Optional.empty();
                }
                throw e;
            }
        }
    }
}
