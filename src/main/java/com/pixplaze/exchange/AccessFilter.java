package com.pixplaze.exchange;

import com.pixplaze.auth.InvalidTokenException;
import com.pixplaze.auth.PluginAccess;
import com.pixplaze.auth.PluginTokenVerifier;
import com.pixplaze.auth.VerifiedToken;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HandlerType;
import io.javalin.http.UnauthorizedResponse;

import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Every REST route of the plugin requires a token, there are no public ones (п. 28 of the authorization
 * design): `Authorization: Bearer` with a plugin access token or the web-api system token. No or bad
 * token — 401; a valid token without the right — 403. WebSocket routes authorize by their first frame.
 */
public final class AccessFilter {

    private static final String BEARER = "Bearer ";
    private static final Set<String> WEBSOCKET_PATHS = Set.of("/console", "/chat");

    private final PluginTokenVerifier verifier;
    private final Logger logger;

    public AccessFilter(PluginTokenVerifier verifier, Logger logger) {
        this.verifier = verifier;
        this.logger = logger;
    }

    public void handle(Context context) {
        if (context.method() == HandlerType.OPTIONS || WEBSOCKET_PATHS.contains(context.path())) {
            return;
        }

        final var header = context.header("Authorization");
        final VerifiedToken token;
        try {
            token = verifier.verify(header != null && header.startsWith(BEARER) ? header.substring(BEARER.length()) : null);
        } catch (InvalidTokenException e) {
            logger.fine(() -> "REST %s %s rejected with 401: %s".formatted(context.method(), context.path(), e.getMessage()));
            throw new UnauthorizedResponse();
        }

        if (!ruleFor(context.path()).test(token)) {
            logger.fine(() -> "REST %s %s rejected with 403: roles %s from %s, subject %s".formatted(context.method(), context.path(),
                    token.authority().roles(), token.authority().source(), token.subject()));
            throw new ForbiddenResponse();
        }
    }

    /// Players and their data (IP, bans, whitelist) — operators; everything else — server members.
    private static Predicate<VerifiedToken> ruleFor(String path) {
        if (path.equals("/players") || path.startsWith("/players/") || path.startsWith("/server/players")) {
            return PluginAccess::canReadPlayers;
        }
        return PluginAccess::canReadServer;
    }
}
