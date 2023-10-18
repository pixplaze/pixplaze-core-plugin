package com.pixplaze.api.info;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public record PlayerListInfo(
        List<PlayerInfo> playerInfos
) {
    @Override
    public String toString() {
        var jsonList = new ArrayList<JSONObject>();
        playerInfos.forEach(x -> jsonList.add(
                new JSONObject()
                        .put("uuid", x.uuid())
                        .put("username", x.username())
                        .put("status", x.status())
                        .put("playtime", x.playtime())
                ));
        return new JSONArray(jsonList).toString();
    }
}
