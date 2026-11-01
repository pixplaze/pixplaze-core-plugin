package com.pixplaze.webapi;

import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/// File with {@link ServerCredentials}, written atomically ({@link AtomicFiles}).
final class ServerCredentialsStore {

    private final Path file;

    ServerCredentialsStore(Path file) {
        this.file = file;
    }

    Optional<ServerCredentials> load() {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(WebApiHttp.json.fromJson(Files.readString(file, StandardCharsets.UTF_8), ServerCredentials.class));
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Cannot read " + file + ": " + e.getMessage(), e);
        }
    }

    void save(ServerCredentials credentials) {
        AtomicFiles.write(file, WebApiHttp.json.toJson(credentials));
    }
}
