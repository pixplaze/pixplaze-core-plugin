package com.pixplaze.api;

import com.pixplaze.api.annotations.DeleteHandler;
import com.pixplaze.api.annotations.GetHandler;
import com.pixplaze.api.annotations.PostHandler;
import com.pixplaze.api.annotations.PutHandler;
import com.pixplaze.api.exceptions.InvalidControllerException;
import io.javalin.Javalin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public class HttpServer {
    private Javalin javalin;
    private final int port;

    public HttpServer(int port) {
        this.port = port;
        this.javalin = initializeJavalin();
    }

    public void start() {
        javalin.start(port);
    }

    private Javalin initializeJavalin() {
        return Optional.ofNullable(javalin)
                .orElse(Javalin.create());
    }

    public void stop() {
        Optional.ofNullable(javalin).ifPresent(Javalin::close);
    }

    public <T, R> void register(Function<T, R> constructor, T args) {
        register(constructor.apply(args));
    }

    public <T> void register(Class<T> controllerClass, Object ... args) {
        Constructor<?> controllerConstructor;
        Class<?>[] controllerConstructorParameters = Arrays.stream(args)
                .map(Object::getClass)
                .toArray(Class[]::new);
        Object controller;

        try {
            controllerConstructor = controllerClass.getDeclaredConstructor(controllerConstructorParameters);
            controller = controllerConstructor.newInstance(args);
        } catch (NoSuchMethodException e) {
            throw new InvalidControllerException(controllerClass, controllerConstructorParameters);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new InvalidControllerException(e);
        }

        register(controller);
    }

    public <T, R> void register(T controller) {
        Method[] handlerMethods = controller.getClass().getDeclaredMethods();

        for (var method : handlerMethods) {
            if (!method.canAccess(controller)) {
                continue;
            }

            if (method.isAnnotationPresent(GetHandler.class)) {
                var annotation = method.getAnnotation(GetHandler.class);
                javalin.addHandler(
                        annotation.method(),
                        annotation.value(),
                        context -> method.invoke(controller, context)
                );
                return;
            }

            if (method.isAnnotationPresent(PostHandler.class)) {
                var annotation = method.getAnnotation(GetHandler.class);
                javalin.addHandler(
                        annotation.method(),
                        annotation.value(),
                        context -> method.invoke(controller, context)
                );
                return;
            }

            if (method.isAnnotationPresent(PutHandler.class)) {
                var annotation = method.getAnnotation(PutHandler.class);
                javalin.addHandler(
                        annotation.method(),
                        annotation.value(),
                        context -> method.invoke(controller, context)
                );
                return;
            }

            if (method.isAnnotationPresent(DeleteHandler.class)) {
                var annotation = method.getAnnotation(DeleteHandler.class);
                javalin.addHandler(
                        annotation.method(),
                        annotation.value(),
                        context -> method.invoke(controller, context)
                );
                return;
            }
        }

        throw new InvalidControllerException(
                "No public methods in %s annotated with @%s, @%s, @%s or @%s!"
                        .formatted(
                                controller.getClass(),
                                GetHandler.class.getSimpleName(),
                                PostHandler.class.getSimpleName(),
                                PutHandler.class.getSimpleName(),
                                DeleteHandler.class.getSimpleName()
                        ));
    }
}