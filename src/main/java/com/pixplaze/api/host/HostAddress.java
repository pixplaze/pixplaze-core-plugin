package com.pixplaze.api.host;

import java.util.Optional;

/// Public address `host[:port]` as players or browsers reach the server from outside.
///
/// @param host domain or IP, lower case
/// @param port port, or {@code null} — not known from this source
public record HostAddress(String host, Integer port) {

    /// Parses `mc.server.net` or `mc.server.net:50010`; empty for a blank value or a bad port.
    public static Optional<HostAddress> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        final var trimmed = value.trim();
        final var colon = trimmed.lastIndexOf(':');
        if (colon <= 0) {
            return Optional.of(new HostAddress(trimmed.toLowerCase(), null));
        }
        try {
            final var port = Integer.parseInt(trimmed.substring(colon + 1));
            if (port < 1 || port > 65535) {
                return Optional.empty();
            }
            return Optional.of(new HostAddress(trimmed.substring(0, colon).toLowerCase(), port));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /// Port from this address, otherwise {@code fallback}.
    public int portOr(int fallback) {
        return port == null ? fallback : port;
    }

    @Override
    public String toString() {
        return port == null ? host : host + ":" + port;
    }
}
