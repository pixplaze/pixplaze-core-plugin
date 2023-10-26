package com.pixplaze.api.info;


import com.google.gson.annotations.SerializedName;
import org.json.JSONObject;

public record ServerShortInfo(
        String name,
        Integer maxPlayers,
        String difficulty,
        String mapAddress
) { }
