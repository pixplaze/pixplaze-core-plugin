package com.pixplaze.http.server;

import com.pixplaze.http.annotations.*;
import com.pixplaze.http.exceptions.InvalidRequestHandler;
import com.sun.net.httpserver.HttpExchange;

import java.lang.reflect.Method;
import java.util.*;

public class ContextMapper {
	private final Map<String, HashMap<String, Method>> pathMapping;
	private final HandlerValidator validator = new DefaultValidator();

	private static class DefaultValidator implements HandlerValidator {

		final Class<?> requiredReturnType = Void.TYPE;
		final Class<?>[] requiredParameterTypes = new Class[] {HttpExchange.class, QueryParams.class};

		@Override
		public boolean isParameterTypesValid(Method method) {
			return HandlerValidator.getMissedRequiredParams(method, requiredParameterTypes).length == 0;
		}

		@Override
		public boolean isReturnTypeValid(Method method) {
			return method.getReturnType().isAssignableFrom(requiredReturnType);
		}

		@Override
		public Class<?>[] getRequiredParameterTypes() {
			return requiredParameterTypes;
		}

		@Override
		public Class<?> getRequiredReturnType() {
			return requiredReturnType;
		}
	}

	protected ContextMapper(Method[] methods) {
		pathMapping = new HashMap<>();
		scanHandlers(methods);
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

	private void scanHandlers(Method[] methods) throws InvalidRequestHandler {
		for (var method: methods) {
			if (method.isAnnotationPresent(RequestHandler.class)) {
				validator.validate(method);
				var annotation = method.getAnnotation(RequestHandler.class);
				mapContext(annotation.path(), annotation.method(), method);
			} else if (method.isAnnotationPresent(GetHandler.class)) {
				validator.validate(method);
				var annotation = method.getAnnotation(GetHandler.class);
				mapContext(annotation.value(), annotation.method(), method);
			} else if (method.isAnnotationPresent(PostHandler.class)) {
				validator.validate(method);
				var annotation = method.getAnnotation(PostHandler.class);
				mapContext(annotation.value(), annotation.method(), method);
			} else if (method.isAnnotationPresent(PutHandler.class)) {
				validator.validate(method);
				var annotation = method.getAnnotation(PutHandler.class);
				mapContext(annotation.value(), annotation.method(), method);
			} else if (method.isAnnotationPresent(DeleteHandler.class)) {
				validator.validate(method);
				var annotation = method.getAnnotation(DeleteHandler.class);
				mapContext(annotation.value(), annotation.method(), method);
			}
		}
	}
}
