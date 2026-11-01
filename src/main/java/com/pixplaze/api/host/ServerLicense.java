package com.pixplaze.api.host;

import org.bukkit.Server;

/// Whether the server checks accounts with Mojang itself (п. 33 of the authorization design). Behind a proxy
/// (BungeeCord, Velocity) the backend always runs `online-mode=false` and cannot tell whether the proxy checks
/// accounts, so such a server counts as non-licensed — the safe side.
public final class ServerLicense {

    private ServerLicense() {
    }

    public static boolean isLicensed(Server server) {
        return server.getOnlineMode() && !ProxyDetector.isProxied();
    }
}
