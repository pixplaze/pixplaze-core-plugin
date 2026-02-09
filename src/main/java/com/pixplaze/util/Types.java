package com.pixplaze.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Types {
	public static String stringifyTypes(Class<?>[] types) {
		return Arrays.stream(types)
				.map(Class::getSimpleName)
				.collect(Collectors.joining(", "));
	}

	public static Class<?>[] toObjectTypes(Object ... objects) {
        if (objects.length == 0) {
			return new Class<?>[0];
		}

		final var types = new Class<?>[objects.length];

		for (int i = 0; i < types.length; i++) {
			types[i] = objects[i].getClass();
		}

		return types;
	}

	public static Class<?>[] toGenericObjectTypes(Object ... objects) {
		if (objects.length == 0) {
			return new Class<?>[0];
		}

		final var types = new Class<?>[objects.length];

        Arrays.fill(types, Object.class);

		return types;
	}
}
