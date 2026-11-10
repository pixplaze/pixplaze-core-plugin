package com.pixplaze.api.dao;

import com.pixplaze.api.ext.data.player.MinecraftPlayerInfo;
import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Function;

/// Player snapshot for the heartbeat (п. 32 of the authorization design): every online player with the
/// nickname, operator and vanish flags and the skin head ([SkinHeads]), sorted by UUID, and the hash of
/// that list. The head is in the hash: a head loaded after the player joined changes the hash, so the next
/// heartbeat carries it; after that the list (and the heads) travel only on changes again.
final class PlayerSnapshots {

    /// Metadata key vanish plugins (EssentialsX, SuperVanish, PremiumVanish) set on hidden players.
    private static final String VANISHED = "vanished";
    private final PixplazeCorePlugin plugin;
    private final SkinHeads skinHeads;

    PlayerSnapshots(PixplazeCorePlugin plugin) {
        this.plugin = plugin;
        this.skinHeads = new SkinHeads(plugin);
    }

    /// Reads Bukkit API: call on the main thread.
    List<MinecraftPlayerInfo> of(Collection<? extends Player> players) {
        return players.stream()
                .map(this::toPresence)
                .sorted(Comparator.comparing(MinecraftPlayerInfo::uuid))
                .toList();
    }

    private <P extends Player> MinecraftPlayerInfo toPresence(P player) {
        return MinecraftPlayerInfo.presence(
                player.getUniqueId(),
                player.getName(),
                player.isOp(),
                isVanished(player),
                skinHeads.head(player)
        );
    }

    /// SHA-256 of the sorted snapshot. Opaque to web-api: it only compares it with the hash it stored.
    static String hash(List<MinecraftPlayerInfo> snapshot) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256");
            for (var player : snapshot) {
                digest.update((player.uuid() + "|" + player.username() + "|" + player.isOperator() + "|" + player.isHidden()
                        + "|" + player.skinHeadBase64() + "\n").getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static boolean isVanished(Player player) {
        return player.getMetadata(VANISHED).stream().anyMatch(MetadataValue::asBoolean);
    }
}
