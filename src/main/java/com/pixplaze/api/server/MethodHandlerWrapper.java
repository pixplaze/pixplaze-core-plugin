package com.pixplaze.api.server;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Objects;

public final class MethodHandlerWrapper implements Handler {
    private final Object controller;
    private final String path;
    private final HandlerType method;
    private final Method handler;

    public MethodHandlerWrapper(Object controller, String path, HandlerType method, Method handler) {
        this.controller = controller;
        this.path = path;
        this.method = method;
        this.handler = handler;
    }

    @Override
    public void handle(@NotNull Context context) throws Exception {
        handler().invoke(controller(), context);
    }

    public Object controller() {
        return controller;
    }

    public String path() {
        return path;
    }

    public HandlerType method() {
        return method;
    }

    public Method handler() {
        return handler;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MethodHandlerWrapper) obj;
        return Objects.equals(this.controller, that.controller) &&
                Objects.equals(this.path, that.path) &&
                Objects.equals(this.method, that.method) &&
                Objects.equals(this.handler, that.handler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(controller, path, method, handler);
    }

    @Override
    public String toString() {
        return "MethodHandler[" +
                "controller=" + controller + ", " +
                "path=" + path + ", " +
                "method=" + method + ", " +
                "handler=" + handler + ']';
    }
}
