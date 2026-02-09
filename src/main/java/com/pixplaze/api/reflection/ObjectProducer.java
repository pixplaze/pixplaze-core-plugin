package com.pixplaze.api.reflection;

import java.lang.reflect.InvocationTargetException;

@FunctionalInterface
public interface ObjectProducer<T extends Object> {
    T produce() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException;
}
