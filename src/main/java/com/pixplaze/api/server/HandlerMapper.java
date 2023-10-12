package com.pixplaze.api.server;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.pixplaze.api.server.annotations.DeleteHandler;
import com.pixplaze.api.server.annotations.GetHandler;
import com.pixplaze.api.server.annotations.PostHandler;
import com.pixplaze.api.server.annotations.PutHandler;
import com.pixplaze.api.server.annotations.RestController;
import com.pixplaze.api.server.exceptions.InvalidControllerException;
import io.javalin.Javalin;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class HandlerMapper {
    private final Javalin javalin;
    private final ClassLoader classLoader;
    private final String controllersPackageName;
    private final Logger logger;

    public HandlerMapper(Javalin javalin) {
        this(javalin, "com.pixplaze.api"); // TODO: Изменить пакет
    }

    public HandlerMapper(Javalin javalin, String controllersPackageName) {
        this.javalin = javalin;
        this.logger = Logger.getLogger("HandlerMapper");
        this.classLoader = getClass().getClassLoader();
        this.controllersPackageName = controllersPackageName;
g
//        var classes = findControllerClasses(controllersPackageName);
//        logger.warning("Controllers:\n%s".formatted(classes.stream()
//            .map(Class::getCanonicalName)
//            .collect(Collectors.joining("\n"))));
    }

    private void mapControllers() {
        var controllerClasses = findControllerClasses(controllersPackageName);
        logger.warning("Controllers:\n%s".formatted(controllerClasses.stream()
            .map(Class::getCanonicalName)
            .collect(Collectors.joining("\n"))));
        for (var controllerClass : controllerClasses) {
            logger.info(controllerClass.getName());
            mapAnnotatedHandlersByClass(controllerClass);
        }
    }

    public <T> void mapAnnotatedHandlersByClass(Class<T> controllerClass, Object ... args) {
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

        mapAnnotatedHandlersByObject(controller);
    }

    protected <T> void mapAnnotatedHandlersByObject(T controller) {
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

    @SuppressWarnings("UnstableApiUsage")
    protected Set<Class<?>> findControllerClasses(String packageName) {
        try {
            return ClassPath.from(classLoader).getAllClasses().stream()
                .filter(classInfo -> classInfo.getPackageName().startsWith(packageName))
                .map(ClassInfo::load)
                .filter(cls -> cls.isAnnotationPresent(RestController.class))
                .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
