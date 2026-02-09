package com.pixplaze.api.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public final class ReflectionDebug {

    private static final BiFunction<String, String, Boolean> EQUALS_EXACT_FILTER = String::equals;
    private static final BiFunction<String, String, Boolean> STARTS_WITH_FILTER = (s, p) -> s.toLowerCase().startsWith(p.replace("*", "").toLowerCase());
    private static final BiFunction<String, String, Boolean> ENDS_WITH_FILTER = (s, p) -> s.toLowerCase().endsWith(p.replace("*", "").toLowerCase());
    private static final BiFunction<String, String, Boolean> CONTAINS_FILTER = (s, p) -> s.toLowerCase().contains(p.replace("*", "").toLowerCase());
    private final Reflection reflection;
    private ReflectedFieldFounder reflectedFieldFounder = null;
    private ReflectedMethodFounder reflectedMethodFounder = null;
    private ReflectedConstructorFounder reflectedConstructorFounder = null;

    public static final class ReflectedFieldFounder {
        private final Reflection reflection;

        private ReflectedFieldFounder(Reflection reflection) {
            this.reflection = reflection;
        }

        public List<Field> named(String searchString) {
            final var searchFilter = ReflectionDebug.resolveFilter(searchString);
            return Arrays.stream(reflection.getReflectedClass().getFields())
                    .filter(field -> searchFilter.apply(field.getName(), searchString))
                    .toList();
        }

        public List<Field> typed(Class<?> ... types) {
            final var typeList = Arrays.stream(types).toList();
            return Arrays.stream(reflection.getReflectedClass().getDeclaredFields())
                    .filter(method -> typeList.contains(method.getType()))
                    .toList();
        }

        public List<Field> all() {
            return Arrays.stream(reflection.getReflectedClass().getDeclaredFields())
                    .toList();
        }

        public List<Field> declared() {
            return Arrays.stream(reflection.getReflectedClass().getFields())
                    .toList();
        }
    }

    public static final class ReflectedMethodFounder {
        private final Reflection reflection;

        private ReflectedMethodFounder(Reflection reflection) {
            this.reflection = reflection;
        }

        public Method find(String methodName, Class<?> ... parameterTypes) {
            try {
                return reflection.getReflectedClass().getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public List<Method> named(String searchString) {
            final var searchFilter = ReflectionDebug.resolveFilter(searchString);
            return Arrays.stream(reflection.getReflectedClass().getMethods())
                    .filter(method -> searchFilter.apply(method.getName(), searchString))
                    .toList();
        }

        public List<Method> returns(Class<?> ... returnTypes) {
            final var returnTypeList = Arrays.stream(returnTypes).toList();
            return Arrays.stream(reflection.getReflectedClass().getMethods())
                    .filter(method -> returnTypeList.contains(method.getReturnType()))
                    .toList();
        }

        public List<Method> parametrized(Class<?> ... returnTypes) {
            final var returnTypeSet = Arrays.stream(returnTypes).collect(Collectors.toSet());
            return Arrays.stream(reflection.getReflectedClass().getMethods())
                    .filter(method -> returnTypeSet.containsAll(Arrays.stream(method.getParameterTypes()).toList()))
                    .toList();
        }

        public List<Method> all() {
            return Arrays.stream(reflection.getReflectedClass().getMethods())
                    .toList();
        }

        public List<Method> declared() {
            return Arrays.stream(reflection.getReflectedClass().getDeclaredMethods())
                    .toList();
        }
    }

    public static final class ReflectedConstructorFounder {
        private final Reflection reflection;

        public ReflectedConstructorFounder(Reflection reflection) {
            this.reflection = reflection;
        }

        public Constructor<?> find(Class<?> parameterTypes) {
            try {
                return reflection.getReflectedClass().getDeclaredConstructor(parameterTypes);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public List<Constructor<?>> all() {
            return Arrays.stream(reflection.getReflectedClass().getDeclaredConstructors())
                    .toList();
        }

    }

    ReflectionDebug(Reflection reflection) {
        this.reflection = reflection;
    }

    public ReflectedFieldFounder fields() {
        if (Objects.isNull(reflectedFieldFounder)) {
            reflectedFieldFounder = new ReflectedFieldFounder(reflection);
        }

        return reflectedFieldFounder;
    }

    public ReflectedMethodFounder methods() {
        if (Objects.isNull(reflectedMethodFounder)) {
            reflectedMethodFounder = new ReflectedMethodFounder(reflection);
        }

        return reflectedMethodFounder;
    }

    public ReflectedConstructorFounder constructors() {
        if (Objects.isNull(reflectedConstructorFounder)) {
            reflectedConstructorFounder = new ReflectedConstructorFounder(reflection);
        }

        return reflectedConstructorFounder;
    }

    private static BiFunction<String, String, Boolean> resolveFilter(String searchString) {
        if (searchString.startsWith("*") && searchString.endsWith("*")) {
            return CONTAINS_FILTER;
        }

        if (searchString.endsWith("*")) {
            return STARTS_WITH_FILTER;
        }

        if (searchString.startsWith("*")) {
            return ENDS_WITH_FILTER;
        }

        return EQUALS_EXACT_FILTER;
    }
}
