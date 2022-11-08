package com.pixplaze.util;

import java.util.function.Consumer;
import java.util.function.Function;

@Deprecated(since = "0.1.3-indev", forRemoval = true)
public class Optional {

    /**
     * Выполняет функцию {@code function} объекта {@code obj}, если {@code obj != null},
     * возвращает результат выполнения {@code function}.
     * В противном случае возвращает {@code null}.
     *
     * @see Optional#returnNotNull(Object, Function, Object)
     */
    public static <T, R> R returnNotNull(T obj, Function<T, R> function) {
        return returnNotNull(obj, function, null);
    }

    /**
     * Выполняет функцию {@code function} объекта {@code obj}, если {@code obj != null},
     * возвращает результат выполнения {@code function}.
     * В противном случае возвращает значение {@code fallback}.
     *
     * @param obj      объект, проверяемый на null.
     * @param function функция, вызываемая из объекта, если он не равен null.
     * @param fallback возвращемое значение, в случае, если объект оказался равен null.
     * @param <T>      тип аргумента функии.
     * @param <R>      тип возвращаемого значения.
     * @return результат выполнения функции {@code function} из объекта {@code obj}.
     * @see java.util.function.Function
     */
    public static <T, R> R returnNotNull(T obj, Function<T, R> function, R fallback) {
        if (obj == null) return fallback;
        else return function.apply(obj);
    }

    /**
     * Выполняет {@code method}, если {@code obj != null}.
     * В противном случае метод не выполняется.
     *
     * @param obj    объект, проверяемый на null
     * @param method метод, вызываемый, если объект не равен null
     * @param <T>    тип аргумента функции (=> объекта, который мы проверяем)
     * @see java.util.function.Consumer
     */
    public static <T> void runNotNull(T obj, Consumer<T> method) {
        if (obj != null) method.accept(obj);
    }
}
