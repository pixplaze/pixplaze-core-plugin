package com.pixplaze.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Types {
	public static String stringifyTypes(Class<?>[] types) {
		return Arrays.stream(types)
				.map(Class::getSimpleName)
				.collect(Collectors.joining(", "));
	}
}
