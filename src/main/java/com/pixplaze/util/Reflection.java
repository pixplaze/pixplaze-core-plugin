package com.pixplaze.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.function.Consumer;

public class Reflection {
	public static Method[] getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotation) {
		var allMethods = clazz.getMethods();
		var annotatedMethods = new ArrayList<Method>();

		for (var method: allMethods) {
			if (method.isAnnotationPresent(annotation))
				annotatedMethods.add(method);
		}
		return annotatedMethods.toArray(Method[]::new);
	}
}
