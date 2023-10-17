package com.pixplaze.api.dto;


import org.json.JSONObject;

public record ServerShortDTO(
        String name,
        int maxPlayers,
        String difficulty,
        String mapAddress
) {
    @Override
    public String toString() {
        return new JSONObject()
                .put("name", name)
                .put("max_players", maxPlayers)
                .put("difficulty", difficulty)
                .put("map_address", mapAddress)
                .toString();
    }
}
