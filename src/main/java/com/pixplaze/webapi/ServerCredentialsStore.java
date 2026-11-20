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
    private volatile ServerCredentials serverCredentials;

    ServerCredentialsStore(Path file) {
        this.file = file;
    }

    Optional<ServerCredentials> load() {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            final var serverCredentials = WebApiHttp.json.fromJson(Files.readString(file, StandardCharsets.UTF_8), ServerCredentials.class);
            this.serverCredentials = serverCredentials;
            return Optional.ofNullable(serverCredentials);
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Cannot read " + file + ": " + e.getMessage(), e);
        }
    }

    void save(ServerCredentials serverCredentials) {
        this.serverCredentials = serverCredentials;
        AtomicFiles.write(file, WebApiHttp.json.toJson(serverCredentials));
    }

    boolean isRegistered() {
        return this.serverCredentials != null && serverCredentials.minecraftServerId() != null;
    }

    boolean isAuthorized() {
        return isRegistered() && serverCredentials.hasTokens();
    }
}
