package com.pixplaze.api.info;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public record PlayerListInfo(
        List<PlayerInfo> playerInfos
) { }
