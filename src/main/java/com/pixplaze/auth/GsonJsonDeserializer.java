package com.pixplaze.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import io.jsonwebtoken.io.DeserializationException;
import io.jsonwebtoken.io.Deserializer;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/// JSON for jjwt on the Gson bundled with Spigot, so the plugin does not ship Jackson. Integers stay
/// `Long` (`exp`, `iat`, `mc.sid`), not `Double`.
final class GsonJsonDeserializer implements Deserializer<Map<String, ?>> {

    private static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
    private static final Type MAP = new TypeToken<Map<String, Object>>() {}.getType();

    @Override
    public Map<String, ?> deserialize(Reader reader) throws DeserializationException {
        try {
            return GSON.fromJson(reader, MAP);
        } catch (RuntimeException e) {
            throw new DeserializationException("Invalid JSON: " + e.getMessage(), e);
        }
    }

    @Override
    @Deprecated
    public Map<String, ?> deserialize(byte[] bytes) throws DeserializationException {
        return deserialize(new StringReader(new String(bytes, StandardCharsets.UTF_8)));
    }
}
