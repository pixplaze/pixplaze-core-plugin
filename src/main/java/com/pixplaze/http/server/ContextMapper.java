package com.pixplaze.http.server;

import com.pixplaze.http.annotations.*;
import com.pixplaze.http.exceptions.InvalidRequestHandler;
import com.sun.net.httpserver.HttpExchange;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ContextMapper {
	private final Map<String, HashMap<String, Method>> pathMapping;

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
		if (!isRequiredParametersPresent(method)) {
			throw new InvalidRequestHandler(
					"Invalid handler params in %s! Handler method requires: %s, %s."
					.formatted(method.getName(),
							   HttpExchange.class.getSimpleName(),
							   QueryParams.class.getSimpleName()));
		}
		if (!isReturnTypeValid(method))
			throw new InvalidRequestHandler(
					"Invalid handler return type in %s! Expected: %s."
					.formatted(method.getName(), "void"));
	}

	protected static boolean isRequiredParametersPresent(Method method) {
		var types = method.getParameterTypes();
		var isHttpExchangePresent = false;
		var isQueryParamsPresent = false;

		for (var type: types) {
			if (type.isAssignableFrom(HttpExchange.class))
				isHttpExchangePresent = true;
			if (type.isAssignableFrom(QueryParams.class))
				isQueryParamsPresent = true;
		}
		return isHttpExchangePresent && isQueryParamsPresent;
	}

	protected static boolean isReturnTypeValid(Method method) {
		var type = method.getReturnType();
		return type.isAssignableFrom(Void.TYPE);
	}
}
