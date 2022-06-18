package com.pixplaze.http.server;

import com.pixplaze.http.annotations.*;
import com.pixplaze.http.exceptions.InvalidRequestHandler;
import com.sun.net.httpserver.HttpExchange;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ContextMapper {
	private final Map<String, HashMap<String, Method>> pathMapping;
	private final static Class<?>[] requiredTypes = new Class[] {
			HttpExchange.class, QueryParams.class
	};

	protected ContextMapper() {
		pathMapping = new HashMap<>();
	}

	protected void mapContext(String path, String method, Method handler) {
		var map = pathMapping.get(path);
		if (map == null) {
			map = new HashMap<>();
			map.put(method, handler);
			pathMapping.put(path, map);
		} else {
			map.put(method, handler);
		}
	}

	protected Map<String, HashMap<String, Method>> getContextMapping() {
		return this.pathMapping;
	}

	protected static ContextMapper scanHandlers(Method[] methods) throws InvalidRequestHandler {
		var contextMapper = new ContextMapper();
		for (var method: methods) {
			if (method.isAnnotationPresent(RequestHandler.class)) {
				validateHandler(method);
				var annotation = method.getAnnotation(RequestHandler.class);
				contextMapper.mapContext(annotation.path(), annotation.method(), method);
			} else if (method.isAnnotationPresent(GetHandler.class)) {
				validateHandler(method);
				var annotation = method.getAnnotation(GetHandler.class);
				contextMapper.mapContext(annotation.value(), annotation.method(), method);
			} else if (method.isAnnotationPresent(PostHandler.class)) {
				validateHandler(method);
				var annotation = method.getAnnotation(PostHandler.class);
				contextMapper.mapContext(annotation.value(), annotation.method(), method);
			} else if (method.isAnnotationPresent(PutHandler.class)) {
				validateHandler(method);
				var annotation = method.getAnnotation(PutHandler.class);
				contextMapper.mapContext(annotation.value(), annotation.method(), method);
			} else if (method.isAnnotationPresent(DeleteHandler.class)) {
				validateHandler(method);
				var annotation = method.getAnnotation(DeleteHandler.class);
				contextMapper.mapContext(annotation.value(), annotation.method(), method);
			}
		}
		return contextMapper;
	}

	protected static void validateHandler(Method method) throws InvalidRequestHandler {
		var missedRequiredParams = getMissedRequiredParams(method);
		if (missedRequiredParams.length > 0) {
			throw new InvalidRequestHandler(
					"Invalid handler params in method %s.%s(%s)!%nMissed: %s"
					.formatted(
							method.getDeclaringClass().getCanonicalName(),
							method.getName(),
							stringifyTypes(method.getParameterTypes()),
							stringifyTypes(missedRequiredParams)
					)
			);
		}
		if (!isReturnTypeValid(method))
			throw new InvalidRequestHandler(
					"Invalid handler return type in %s! Expected: %s."
					.formatted(method.getName(), "void"));
	}

	protected static Class<?>[] getMissedRequiredParams(Method method) {
		var methodTypes = method.getParameterTypes();

		var requiredPresent = new LinkedHashMap<Class<?>, Boolean>();

		outer:
		for (var requiredType: requiredTypes) {
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

	protected static boolean isReturnTypeValid(Method method) {
		var type = method.getReturnType();
		return type.isAssignableFrom(Void.TYPE);
	}

	private static String stringifyTypes(Class<?>[] types) {
		return Arrays.stream(types)
				.map(Class::getSimpleName)
				.collect(Collectors.joining(", "));
	}
}
