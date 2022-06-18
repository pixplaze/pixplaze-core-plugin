package com.pixplaze.http.server;

import com.pixplaze.http.exceptions.InvalidRequestHandler;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.pixplaze.util.Types.stringifyTypes;

public interface HandlerValidator {
	default void validate(Method method) {
		if (!isParameterTypesValid(method)) {
			throw new InvalidRequestHandler(
					"Invalid handler params in method %s.%s(%s)!%nExpected: %s"
					.formatted(
							method.getDeclaringClass().getCanonicalName(),
							method.getName(),
							stringifyTypes(method.getParameterTypes()),
							stringifyTypes(getRequiredParameterTypes())
					)
			);
		}

		if (!isReturnTypeValid(method))
			throw new InvalidRequestHandler(
					"Invalid handler return type in %s! Expected: %s."
					.formatted(method.getName(), getRequiredReturnType())
			);
	}

	static Class<?>[] getMissedRequiredParams(Method method, Class<?>[] requiredParameterTypes) {
		var methodTypes = method.getParameterTypes();

		var requiredPresent = new LinkedHashMap<Class<?>, Boolean>();

		outer:
		for (var requiredType: requiredParameterTypes) {
			for (var methodType: methodTypes) {
				if (methodType.isAssignableFrom(requiredType)) {
					requiredPresent.put(requiredType, true);
					continue outer;
				}
			}
			requiredPresent.put(requiredType, false);
		}

		return requiredPresent.entrySet()
				.stream()
				.filter(entry -> !entry.getValue())
				.map(Map.Entry::getKey).toArray(Class[]::new);
	}

	boolean isParameterTypesValid(Method method);

	boolean isReturnTypeValid(Method method);

	Class<?>[] getRequiredParameterTypes();

	Class<?> getRequiredReturnType();
}
