package com.pixplaze.webapi;

import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/// Player files {@code players/<first two UUID chars>/<uuid>.json}: 256 subfolders keep big servers
/// (millions of players) usable by hosting panels, FTP and backups. Written atomically.
final class PlayerRecordStore {

    private final Path root;

    PlayerRecordStore(Path root) {
        this.root = root;
    }

    Optional<PlayerRecord> load(UUID id) {
        final var file = fileOf(id);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(WebApiHttp.json.fromJson(Files.readString(file, StandardCharsets.UTF_8), PlayerRecord.class));
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Cannot read " + file + ": " + e.getMessage(), e);
        }
    }

    void save(PlayerRecord record) {
        AtomicFiles.write(fileOf(record.id()), WebApiHttp.json.toJson(record));
    }

    private Path fileOf(UUID id) {
        final var name = id.toString();
        return root.resolve(name.substring(0, 2)).resolve(name + ".json");
    }
}
