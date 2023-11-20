package com.pixplaze.api.info;

import java.util.List;

public record ServerInfo(
        String name,
        Integer maxPlayers,
        String difficulty,
        String mapAddress,
        String coreName,
        String coreVersion,
        List<String> plugins
) { }
