package com.pixplaze.util;

import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class AddressResolver {
    private static final AtomicBoolean isUntrustedAddressWarningSent = new AtomicBoolean(false);

    private static boolean isUntrustedAddress(InetAddress ip) {
        return ip == null || ip.isLoopbackAddress() || ip.isSiteLocalAddress() || ip.isLinkLocalAddress();
    }

    /// The address is not the player's own — loopback or private network, as behind a proxy without IP forwarding.
    public static boolean isUntrusted(PixplazeCorePlugin plugin, Player player) {
        final InetAddress ip = addressOf(player);
        final var isIpUntrusted = isUntrustedAddress(ip);
        if (isIpUntrusted && isUntrustedAddressWarningSent.compareAndSet(false, true)) {
            plugin.getLogger().warning(
                    "Players join from a local address (" + (ip == null ? "unknown" : ip.getHostAddress()) + "): " +
                            "if the server is behind a proxy, enable IP forwarding, " +
                            "otherwise every player join needs a Pixplaze confirmation"
            );
        }
        return isIpUntrusted;
    }

    public static String ipOf(Player player) {
        final var address = player.getAddress();
        return address == null || address.getAddress() == null ? null : address.getAddress().getHostAddress();
    }

    public static String ipOf(InetAddress inetAddress) {
        return inetAddress == null ? null : inetAddress.getHostAddress();
    }

    public static InetAddress addressOf(Player player) {
        return player.getAddress() == null ? null : player.getAddress().getAddress();
    }
}
