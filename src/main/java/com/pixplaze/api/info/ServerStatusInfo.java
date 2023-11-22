package com.pixplaze.api.info;

import com.google.gson.annotations.SerializedName;
import org.json.JSONObject;

public record ServerStatusInfo(
        String onlineStatus,
        Integer players,
        Long uptime
) { }
