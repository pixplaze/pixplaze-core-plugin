package com.pixplaze.exchange;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.json.JsonMapper;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/** Эта обёртка позволяет получить доступ к настройкам сериализатора GSON. */
public class JavalinGsonWrapper implements JsonMapper {

    private final Gson gson = new GsonBuilder()
            .create();

    @NotNull
    @Override
    public String toJsonString(@NotNull Object obj, @NotNull Type type) {
        return gson.toJson(obj, type);
    }

    @NotNull
    @Override
    public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
        return gson.fromJson(json, targetType);
    }
}
