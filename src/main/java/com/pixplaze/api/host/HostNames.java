package com.pixplaze.api.host;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/// Нормализация адреса из Minecraft handshake ({@code PlayerLoginEvent#getHostname()})
/// до чистого публичного домена.
///
/// Покрывает разные топологии:
/// - прямой сервер: "play.example.com:25565" -> "play.example.com";
/// - BungeeCord (legacy forwarding): адрес приходит как
///   "hostПлагина\0clientIp\0uuid\0props" — берём первый токен до NUL
///   (это адрес, который игрок ввёл в клиенте, чтобы попасть на прокси);
/// - Forge/FML: маркеры "\0FML\0" / "\0FML2\0" отсекаются тем же split.
///
/// Отбрасывает всё, что не является публичным доменом: сырые IP (IPv4/IPv6),
/// localhost, одиночные метки без точки (внутренние имена/адрес бэкенда за прокси).
public final class HostNames {

    /// Разделитель полей forwarding/FML — нулевой символ (регулярка \x00).
    private static final String FORWARDING_SEPARATOR = "\\x00";
    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    private HostNames() {
    }

    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }

        // 1. Отсекаем forwarding/FML-маркеры: реальный хост — первый токен до NUL.
        String host = raw.split(FORWARDING_SEPARATOR)[0].trim();

        // 2. Срезаем ':port' (только если хвост после последнего ':' — это цифры).
        final int colon = host.lastIndexOf(':');
        if (colon > 0) {
            final String tail = host.substring(colon + 1);
            if (!tail.isEmpty() && tail.chars().allMatch(Character::isDigit)) {
                host = host.substring(0, colon);
            }
        }

        host = host.trim();
        if (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1); // FQDN trailing dot
        }
        host = host.toLowerCase(Locale.ROOT);

        // 3. Оставляем только похожее на публичный домен.
        if (host.isEmpty() || host.equals("localhost")) {
            return Optional.empty();
        }
        if (host.indexOf(':') >= 0 || host.indexOf('[') >= 0) {
            return Optional.empty(); // IPv6-литерал
        }
        if (IPV4.matcher(host).matches()) {
            return Optional.empty(); // сырой IPv4
        }
        if (host.indexOf('.') < 0) {
            return Optional.empty(); // одиночная метка — внутреннее имя, не публичный домен
        }

        return Optional.of(host);
    }
}
