package com.pixplaze.api.info;

public record ServerShortInfo(
        String name,
        Integer maxPlayers,
        String difficulty,
        String mapAddress,
        String coreName,
        String coreVersion
) { }
