package com.pixplaze.webapi;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.pixplaze.api.ext.OAuthException;
import com.pixplaze.api.ext.data.oauth.OAuthErrorResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/// Transport of the web-api client: one {@link HttpClient}, form and JSON requests, response parsing.
///
/// OAuth endpoints answer in snake_case, so {@link #oauthJson} is used only for them; request bodies,
/// `authorization_details` and other responses are camelCase ({@link #json}).
final class WebApiHttp {

    static final Gson json = new Gson();
    static final Gson oauthJson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final String baseUrl;

    WebApiHttp(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /// POST `application/x-www-form-urlencoded` to an OAuth endpoint. A 4xx response with an OAuth
    /// error body becomes {@link OAuthException}.
    <T> T postForm(String path, Map<String, String> form, Class<T> responseType) {
        final var body = form.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        final var request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final var response = send(request);
        if (response.statusCode() / 100 == 2) {
            return oauthJson.fromJson(response.body(), responseType);
        }
        throw oauthError(response);
    }

    /// POST JSON with a Bearer token, JSON response.
    <T> T postJson(String path, String accessToken, Object body, Class<T> responseType) {
        final var request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(json.toJson(body)))
                .build();

        final var response = send(request);
        if (response.statusCode() / 100 != 2) {
            throw new WebApiException(response.statusCode(), response.body());
        }
        return response.body() == null || response.body().isBlank() ? null : json.fromJson(response.body(), responseType);
    }

    /// GET with a Bearer token, raw response body.
    String getRaw(String path, String accessToken) {
        final var request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        final var response = send(request);
        if (response.statusCode() / 100 != 2) {
            throw new WebApiException(response.statusCode(), response.body());
        }
        return response.body();
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new WebApiException("web-api is unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WebApiException("Request to web-api was interrupted", e);
        }
    }

    private static RuntimeException oauthError(HttpResponse<String> response) {
        try {
            final var error = oauthJson.fromJson(response.body(), OAuthErrorResponse.class);
            if (error != null && error.error() != null) {
                return new OAuthException(error);
            }
        } catch (JsonParseException ignored) {
            // Not an OAuth error body
        }
        return new WebApiException(response.statusCode(), response.body());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
