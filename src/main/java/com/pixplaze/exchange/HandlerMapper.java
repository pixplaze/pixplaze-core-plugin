package com.pixplaze.exchange;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.pixplaze.exchange.annotations.*;
import com.pixplaze.exchange.exceptions.HandlerMappingException;
import io.javalin.Javalin;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class HandlerMapper {
    private final ClassLoader classLoader;
    private final String packageName;
    private final Logger logger;

    public HandlerMapper(String packageName) {
        this.logger = Logger.getLogger("HandlerMapper");
        this.classLoader = getClass().getClassLoader();
        this.packageName = packageName;
    }

    @Beta
    public ImmutableMap<Class<?>, ImmutableSet<MethodHandlerWrapper>> map(Javalin javalin) {
        final var handlers = lookup(classLoader, packageName);

        for (var handler : handlers.entrySet()) {
            var methodHandlers = handler.getValue();
            for (var methodHandler : methodHandlers) {
                javalin.addHandler(
                        methodHandler.method(),
                        methodHandler.path(),
                        methodHandler);
            }
        }

        logger.info("Initialized REST handlers:%n%n%s%n".formatted(
                handlers.entrySet().stream()
                        .map(entry -> "Controller class %s:%n%s".formatted(
                                entry.getKey().getCanonicalName(),
                                entry.getValue().stream()
                                        .map(wrapper -> "%-6s: %s".formatted(
                                                        wrapper.method().name().toUpperCase(),
                                                        "%-40s%40s".formatted(
                                                                wrapper.path(),
                                                                wrapper.handler().getName()
                                                        ).replace(" ", ".")
                                                )
                                        ).collect(Collectors.joining("\n"))
                        ))
                        .collect(Collectors.joining("\n\n"))));
        return handlers;
    }

    /**
     * Attempts to load REST controller classes from package with name filter {@code packageName}.
     * Lookups in this package and its subpackages for classes annotated with {@link RestController}.
     * If such classes were found, returns map of {@link MethodHandlerWrapper} - mapped REST handlers,
     * accessible by controller class key, in which they are defined.
     *
     * @param classLoader a class loader for loading classes.
     * @param packageName a name filter, where to lookup controller classes.
     * @return a map of {@link MethodHandlerWrapper}, accessible by controller class key, in which they are defined.
     * @see MethodHandlerWrapper
     * @see RestController
     */
    public static ImmutableMap<Class<?>, ImmutableSet<MethodHandlerWrapper>> lookup(ClassLoader classLoader, String packageName) {
        final var controllerClasses = findControllerClasses(classLoader, packageName, RestController.class);
        final ImmutableMap.Builder<Class<?>, ImmutableSet<MethodHandlerWrapper>> controllerClassesMapBuilder = ImmutableMap.builder();
        final ImmutableMap<Class<?>, ImmutableSet<MethodHandlerWrapper>> controllerClassesMap;

        for (var controllerClass : controllerClasses) {
            ImmutableSet<MethodHandlerWrapper> methodHandlerWrapperContextsSet = null;
            try {
                methodHandlerWrapperContextsSet = lookupControllerClass(controllerClass);
            } finally {
                Optional.ofNullable(methodHandlerWrapperContextsSet)
                        .ifPresent(handlerContext -> controllerClassesMapBuilder.put(controllerClass, handlerContext));
            }
        }

        controllerClassesMap = controllerClassesMapBuilder.build();

        if (controllerClassesMap.size() == 0) {
            throw new HandlerMappingException(
                    "The package %s does not contain any classes annotated with the @%s annotation."
                            .formatted(packageName, RestController.class));
        }

        return controllerClassesMap;
    }

    /**
     * Attempts to determine whether the provided {@code controllerClass} is a REST controller,
     * and whether it has methods annotated with REST annotations.
     * If it is, produces an instance of {@code controllerClass} and
     * returns a set of mapped {@link MethodHandlerWrapper} methods found in {@code controllerClass} that are REST handlers.
     *
     * @param controllerClass a class intended to be a REST controller class, annotated with {@link RestController}.
     * @param args            an arguments of {@code controllerClass} constructor.
     * @return set of mapped {@link MethodHandlerWrapper} REST handlers of {@code controllerClass} instance.
     * @throws HandlerMappingException if provided {@code controllerClass} is not a REST controller, or
     *                                 if provided {@code controllerClass} can not be produced with given arguments, or
     *                                 if provided {@code controllerClass} does not contain any REST handler methods.
     * @see MethodHandlerWrapper
     */
    @Beta
    public static ImmutableSet<MethodHandlerWrapper> lookupControllerClass(
            Class<?> controllerClass,
            Object... args
    ) throws HandlerMappingException {
        final ImmutableSet.Builder<MethodHandlerWrapper> handlerMethodsSetBuilder = ImmutableSet.builder();
        final ImmutableSet<MethodHandlerWrapper> handlerMethodsSet;

        Constructor<?> controllerConstructor;
        Class<?>[] controllerConstructorParameters = Arrays.stream(args)
                .map(Object::getClass)
                .toArray(Class[]::new);
        Object controller;

        try {
            controllerConstructor = controllerClass.getDeclaredConstructor(controllerConstructorParameters);
            controller = controllerConstructor.newInstance(args);
        } catch (NoSuchMethodException e) {
            throw new HandlerMappingException(controllerClass, controllerConstructorParameters);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new HandlerMappingException(e);
        }

        for (var controllerMethod : controllerClass.getDeclaredMethods()) {
            MethodHandlerWrapper methodHandlerWrapper = null;
            try {
                methodHandlerWrapper = lookupMethodHandler(controller, controllerMethod);
            } catch (HandlerMappingException ignored) {
            } finally {
                Optional.ofNullable(methodHandlerWrapper)
                        .ifPresent(handlerMethodsSetBuilder::add);
            }
        }

        handlerMethodsSet = handlerMethodsSetBuilder.build();

        if (handlerMethodsSet.size() == 0) {
            throw new HandlerMappingException(
                    "No public methods in %s annotated with @%s, @%s, @%s or @%s!"
                            .formatted(
                                    controller.getClass(),
                                    GetHandler.class.getSimpleName(),
                                    PostHandler.class.getSimpleName(),
                                    PutHandler.class.getSimpleName(),
                                    DeleteHandler.class.getSimpleName()
                            ));
        }

        return handlerMethodsSet;
    }

    /**
     * Attempts to determine whether the provided {@code handlerMethod} is a REST handlerMethod.
     * If it is, maps {@code controllerObj} object with provided {@code handlerMethod} method
     * and returns mapped {@link MethodHandlerWrapper}.
     *
     * @param controllerObj an object intended to be a REST controllerObj class.
     * @param handlerMethod a reflected method intended to be a REST handlerMethod annotated
     *                      with {@link GetHandler}, {@link PostHandler}, {@link PutHandler} or {@link DeleteHandler}.
     * @return {@link MethodHandlerWrapper} as mapped REST handlerMethod.
     * @throws HandlerMappingException if provided {@link Method} {@code handlerMethod} is not annotated as REST handlerMethod,
     *                                 or if provided {@code controllerObj} class is not annotated with {@link RestController},
     *                                 or if provided {@code controllerObj} class does not contain any method annotated with REST annotation.
     * @see MethodHandlerWrapper
     */
    @Beta
    public static <T> MethodHandlerWrapper lookupMethodHandler(
            T controllerObj,
            Method handlerMethod
    ) throws HandlerMappingException {
        if (!controllerObj.getClass().isAnnotationPresent(RestController.class)) {
            throw new HandlerMappingException(
                    "%s class is not annotated with @%s annotation!"
                            .formatted(
                                    controllerObj.getClass().getCanonicalName(),
                                    RestController.class.getSimpleName()
                            ));
        }

        final var controllerPath = controllerObj.getClass().getAnnotation(RestController.class).value();

        if (handlerMethod.isAnnotationPresent(GetHandler.class)) {
            var annotation = handlerMethod.getAnnotation(GetHandler.class);
            var path = controllerPath + annotation.value();
            var method = annotation.method();

            return new MethodHandlerWrapper(controllerObj, path, method, handlerMethod);
        }

        if (handlerMethod.isAnnotationPresent(PostHandler.class)) {
            var annotation = handlerMethod.getAnnotation(PostHandler.class);
            var path = controllerPath + annotation.value();
            var method = annotation.method();

            return new MethodHandlerWrapper(controllerObj, path, method, handlerMethod);
        }

        if (handlerMethod.isAnnotationPresent(PutHandler.class)) {
            var annotation = handlerMethod.getAnnotation(PutHandler.class);
            var path = controllerPath + annotation.value();
            var method = annotation.method();

            return new MethodHandlerWrapper(controllerObj, path, method, handlerMethod);
        }

        if (handlerMethod.isAnnotationPresent(DeleteHandler.class)) {
            var annotation = handlerMethod.getAnnotation(DeleteHandler.class);
            var path = controllerPath + annotation.value();
            var method = annotation.method();

            return new MethodHandlerWrapper(controllerObj, path, method, handlerMethod);
        }

        throw new HandlerMappingException(
                "%s#%s() method is not annotated with @%s, @%s, @%s or @%s!"
                        .formatted(
                                controllerObj.getClass().getCanonicalName(),
                                handlerMethod.getName(),
                                GetHandler.class.getSimpleName(),
                                PostHandler.class.getSimpleName(),
                                PutHandler.class.getSimpleName(),
                                DeleteHandler.class.getSimpleName()
                        ));
    }

    @SuppressWarnings({"UnstableApiUsage", "SameParameterValue"})
    public static Set<Class<?>> findControllerClasses(
            ClassLoader classLoader,
            String packageName,
            Class<? extends Annotation> annotationClass) {
        try {
            return ClassPath.from(classLoader).getAllClasses().stream()
                    .filter(classInfo -> classInfo.getPackageName().startsWith(packageName))
                    .map(ClassInfo::load)
                    .filter(cls -> cls.isAnnotationPresent(annotationClass))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
