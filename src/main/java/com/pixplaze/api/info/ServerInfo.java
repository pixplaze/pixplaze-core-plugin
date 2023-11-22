package com.pixplaze.api.info;

import java.util.List;

public record ServerInfo(
        String address,
        Integer apiPort,
        Boolean primary,
        String name,
        String thumbnail,
        String coreName,
        String coreVersion,
        String minecraftVersion,
        Integer mapPort,
        Integer maxPlayers,
        String difficulty,
        List<String> plugins
) { }
