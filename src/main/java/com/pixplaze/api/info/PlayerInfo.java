package com.pixplaze.api.info;

import netscape.javascript.JSObject;
import org.json.JSONObject;

import java.util.UUID;

public record PlayerInfo(
        UUID uuid,
        String username,
        String status,
        Long playtime
) {
    @Override
    public String toString() {
        return new JSONObject()
                .put("uuid", uuid)
                .put("username", username)
                .put("status", status)
                .put("playtime", playtime)
                .toString();
    }
}
