package com.pixplaze.api.dao;

import com.pixplaze.plugin.PixplazeCorePlugin;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/// Skin heads of online players as the server shows them: the texture from the player profile (the
/// Mojang skin, or the one a skin plugin put into the profile), 8×8 face with the hat layer, Base64 PNG.
///
/// A texture is downloaded once per plugin run and cached by its URL. [#head] never blocks: an unknown
/// texture starts a background download and the head is `null` until it is ready; the next heartbeat
/// snapshot then carries it (the head is part of the snapshot hash, see [PlayerSnapshots]).
final class SkinHeads {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    /// A failed texture is not retried before this: the snapshot is built every minute.
    private static final Duration RETRY_AFTER = Duration.ofMinutes(10);
    /// Skin textures are 64×64 PNG of a few KB; anything bigger is not a skin.
    private static final int MAX_BYTES = 64 * 1024;
    private static final int FACE = 8;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    /// Texture URL → head; a missing key — not requested yet.
    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
    /// `getPlayerProfile` appeared in 1.18.1: on an older server heads are off.
    private static volatile boolean supported = true;
    private final PixplazeCorePlugin plugin;

    SkinHeads(PixplazeCorePlugin plugin) {
        this.plugin = plugin;
    }

    /// Head of the player's current skin; `null` — no skin texture (default skin), or not loaded yet.
    /// Reads Bukkit API: call on the main thread.
    String head(Player player) {
        if (!supported) {
            return null;
        }
        final String url;
        try {
            final var skin = player.getPlayerProfile().getTextures().getSkin();
            if (skin == null) {
                return null;
            }
            url = skin.toString();
        } catch (LinkageError e) {
            supported = false;
            plugin.getLogger().warning(
                    "Skin heads are disabled: the server has no player profile API (Spigot 1.18.1+ needed)");
            return null;
        }

        final var entry = CACHE.get(url);
        if (entry != null && (entry.head() != null || entry.isLoading() || Instant.now().isBefore(entry.retryAt()))) {
            return entry.head();
        }
        CACHE.put(url, Entry.LOADING);
        load(url);
        return null;
    }

    private void load(String url) {
        final HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build();
        } catch (IllegalArgumentException e) {
            failed(url, e);
            return;
        }
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("HTTP " + response.statusCode());
                    }
                    return crop(readLimited(response.body()));
                })
                .whenComplete((head, error) -> {
                    if (error != null) {
                        failed(url, error);
                    } else {
                        CACHE.put(url, new Entry(head, null));
                    }
                });
    }

    private void failed(String url, Throwable error) {
        CACHE.put(url, new Entry(null, Instant.now().plus(RETRY_AFTER)));
        plugin.getLogger().log(Level.FINE,
                "Failed to load skin texture %s: %s".formatted(url, error.getMessage()));
    }

    private static byte[] readLimited(InputStream body) {
        try (body) {
            final var bytes = body.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                throw new IllegalStateException("Texture is larger than %d bytes".formatted(MAX_BYTES));
            }
            return bytes;
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /// Face (8, 8) with the hat layer (40, 8) on top, as an 8×8 PNG in Base64. Legacy 64×32 skins have
    /// the hat layer at the same place.
    static String crop(byte[] texture) {
        try {
            final var skin = ImageIO.read(new ByteArrayInputStream(texture));
            if (skin == null || skin.getWidth() < 64 || skin.getHeight() < 16) {
                throw new IllegalStateException("Not a skin texture");
            }
            final var head = new BufferedImage(FACE, FACE, BufferedImage.TYPE_INT_ARGB);
            final var graphics = head.createGraphics();
            try {
                graphics.drawImage(skin.getSubimage(8, 8, FACE, FACE), 0, 0, null);
                graphics.drawImage(skin.getSubimage(40, 8, FACE, FACE), 0, 0, null);
            } finally {
                graphics.dispose();
            }
            final var png = new ByteArrayOutputStream();
            ImageIO.write(head, "png", png);
            return Base64.getEncoder().encodeToString(png.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /// Cached texture: `head` — loaded; `retryAt` — failed, retry after it; both `null` — loading.
    private record Entry(String head, Instant retryAt) {
        static final Entry LOADING = new Entry(null, null);

        boolean isLoading() {
            return head == null && retryAt == null;
        }
    }
}
