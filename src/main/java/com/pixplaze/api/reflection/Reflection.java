package com.pixplaze.api.reflection;

import com.pixplaze.util.Types;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public class Reflection {

    private final Class<?> reflectedClass;
    private final Object reflectedObject;
    private final Map<String, Method> reflectedMethods;
    private final Map<String, Field> reflectedFields;
    private ReflectionDebug reflectionDebug;

    private Reflection(Class<?> reflectedClass, Object reflectedObject) {
        this.reflectedClass = reflectedClass;
        this.reflectedObject = reflectedObject;
        this.reflectedMethods = new HashMap<>();
        this.reflectedFields = new HashMap<>();
        this.reflectionDebug = null;
    }

    public static Reflection provide(String fullClassName, Object ... constructorParameters) {
        try {
            final var parameterTypes = Types.toObjectTypes(constructorParameters);
            final var reflectedClass = Class.forName(fullClassName);
            final var reflectedClassConstructor = reflectedClass.getDeclaredConstructor(parameterTypes);
            final var reflectedObject = reflectedClassConstructor.newInstance(constructorParameters);
            return new Reflection(reflectedClass, reflectedObject);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Reflection provide(ObjectProducer<T> objectProducer) {
        try {
            final var reflectedObject = objectProducer.produce();
            return new Reflection(reflectedObject.getClass(), reflectedObject);
        } catch (ClassNotFoundException |
                 InvocationTargetException |
                 NoSuchMethodException |
                 IllegalAccessException e) {
            throw new ReflectedApiException(e);
        }
    }

    public static Reflection wrap(Object reflectedObject) {
        return new Reflection(reflectedObject.getClass(), reflectedObject);
    }

    public <T> T call(String name, Object ... parameters) {
        try {
            final var parameterTypes = Types.toObjectTypes(parameters);
            return call(name, parameterTypes, parameters);
        } catch (ReflectedApiException e) { // TODO: оптимизировать вызов метода с дженериками
            final var genericParameterTypes = Types.toGenericObjectTypes(parameters);
            return call(name, genericParameterTypes, parameters);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T call(String name, Class<?>[] parameterTypes, Object ... parameters) {
        try {
            final var key = buildMethodKey(name, parameterTypes);
            return ((T) getMethod(name, key, parameterTypes).invoke(reflectedObject, parameters));
        } catch (NoSuchMethodException e) {
            throw new ReflectedApiException(e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private Method getMethod(String name, String key, Class<?>[] parameterTypes) throws NoSuchMethodException {
        var reflectedMethod = reflectedMethods.get(key);

        if (Objects.nonNull(reflectedMethod)) {
            return reflectedMethod;
        }

        return addMethod(name, key, parameterTypes);
    }

    private Method addMethod(String name, String key, Class<?>[] parameterTypes) throws NoSuchMethodException {
        final var method = reflectedClass.getMethod(name, parameterTypes);

        method.setAccessible(true);
        reflectedMethods.put(key, method);

        return method;
    }

    private String buildMethodKey(String name, Class<?>[] parameterTypes) {
        final var stringTypes = Types.stringifyTypes(parameterTypes);
        return name + "(" + stringTypes + ")";
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        try {
            return (T) getField(name).get(reflectedObject);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ReflectedApiException(e);
        }
    }

    public void set(String name, Object value) {
        try {
            final var field = getField(name);
            field.set(reflectedObject, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ReflectedApiException(e);
        }
    }

    private Field getField(String name) throws NoSuchFieldException {
        var field = reflectedFields.get(name);

        if (Objects.nonNull(field)) {
            return field;
        }

        return addField(name);
    }

    private Field addField(String name) throws NoSuchFieldException {
        Field field;

        try {
            field = reflectedClass.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            field = reflectedClass.getField(name);
        }

        field.setAccessible(true);
        reflectedFields.put(name, field);

        return field;
    }

    public Class<?> getReflectedClass() {
        return reflectedClass;
    }

    public Object getReflectedObject() {
        return reflectedObject;
    }

    @SuppressWarnings("unused")
    public ReflectionDebug debug() {
        if (Objects.isNull(reflectionDebug)) {
            reflectionDebug = new ReflectionDebug(this);
        }

        return reflectionDebug;
    }
}
