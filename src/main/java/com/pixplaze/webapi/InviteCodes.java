package com.pixplaze.webapi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/// Bid codes in the server MOTD: the admin puts the code from the Pixplaze bid into {@code motd} of
/// {@code server.properties}, as for servers without the plugin. Codes are 8 characters of A–Z and 0–9.
final class InviteCodes {

    private static final Pattern FORMATTING = Pattern.compile("§[0-9a-fk-orx]", Pattern.CASE_INSENSITIVE);
    private static final Pattern CODE = Pattern.compile("(?<![A-Za-z0-9])[A-Z0-9]{8}(?![A-Za-z0-9])");

    private InviteCodes() {
    }

    /// Candidates in the MOTD; a word like SURVIVAL also matches, so ones with a digit go first and web-api
    /// tells which one is a real bid code.
    static List<String> candidates(String motd) {
        final var codes = new ArrayList<String>();
        if (motd == null) {
            return codes;
        }
        final var matcher = CODE.matcher(FORMATTING.matcher(motd).replaceAll(""));
        while (matcher.find()) {
            if (!codes.contains(matcher.group())) {
                codes.add(matcher.group());
            }
        }
        codes.sort(Comparator.comparing(code -> code.chars().noneMatch(Character::isDigit)));
        return codes;
    }
}
