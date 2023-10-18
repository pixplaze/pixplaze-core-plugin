package com.pixplaze.api.info;

import org.json.JSONObject;

public record ServerStatusInfo(
        String onlineStatus,
        Integer players,
        Long uptime
) {
    @Override
    public String toString() {
        return new JSONObject()
                .put("online_status", onlineStatus)
                .put("players", players)
                .put("uptime", uptime)
                .toString();
    }
}
