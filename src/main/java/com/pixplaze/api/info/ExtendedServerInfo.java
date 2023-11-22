package com.pixplaze.api.info;

import java.util.List;

/**
 * Данный класс является временным и служит только для замены части данных с бэкенда.
 * Является обёрткой для ServerInfo.
 */
public record ExtendedServerInfo(
        String name,
        List<String> tags,
        Integer rating,
        String description,
        ServerInfo server
) {}
