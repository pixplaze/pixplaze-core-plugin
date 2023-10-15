package com.pixplaze.exchange;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public record MethodHandlerWrapper(Object controller,
                                   String path,
                                   HandlerType method,
                                   Method handler
) implements Handler {

    @Override
    public void handle(@NotNull Context context) throws Exception {
        handler().invoke(controller(), context);
    }
}
