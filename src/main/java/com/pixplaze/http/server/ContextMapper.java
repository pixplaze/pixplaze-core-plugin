package com.pixplaze.http.server;

import com.pixplaze.exceptions.HttpServerException;
import com.pixplaze.http.HttpController;
import com.pixplaze.http.annotations.*;
import com.pixplaze.http.exceptions.InvalidRequestHandler;
import com.pixplaze.http.server.validation.HandlerValidationStrategy;

import java.lang.reflect.Method;
import java.util.*;

public class ContextMapper {
	private final Map<String, HashMap<String, Method>> pathMapping;
	private final HandlerValidationStrategy validationStrategy;
	private final HttpController controller;

	protected ContextMapper(HttpController controller, HandlerValidationStrategy validationStrategy) {
		var methods = controller.getClass().getMethods();
		this.controller = controller;
		this.validationStrategy = validationStrategy;
		this.pathMapping = new HashMap<>();
		scanHandlers(methods);
	}

	private void mapContext(String path, String method, Method handler) {
		if (path == null || path.isBlank()) {
//			throw new HttpServerException(
//					"Mapping path is null or blank for method %s!"
//					.formatted(handler.getName())
//			);
		}

		// TODO: сделать проверки на наличие path, method, handler
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

	protected HttpController getHttpController() {
		return controller;
	}

	private void scanHandlers(Method[] methods) throws InvalidRequestHandler {
		for (var method: methods) {
			if (method.isAnnotationPresent(RequestHandler.class)) {
				validationStrategy.validate(method);
				var annotation = method.getAnnotation(RequestHandler.class);
				mapContext(annotation.path(), annotation.method(), method);
			} else if (method.isAnnotationPresent(GetHandler.class)) {
				validationStrategy.validate(method);
				var annotation = method.getAnnotation(GetHandler.class);
				mapContext(annotation.value(), annotation.method(), method);
			} else if (method.isAnnotationPresent(PostHandler.class)) {
				validationStrategy.validate(method);
				var annotation = method.getAnnotation(PostHandler.class);
				mapContext(annotation.value(), annotation.method(), method);
			} else if (method.isAnnotationPresent(PutHandler.class)) {
				validationStrategy.validate(method);
				var annotation = method.getAnnotation(PutHandler.class);
				mapContext(annotation.value(), annotation.method(), method);
			} else if (method.isAnnotationPresent(DeleteHandler.class)) {
				validationStrategy.validate(method);
				var annotation = method.getAnnotation(DeleteHandler.class);
				mapContext(annotation.value(), annotation.method(), method);
			}
		}
	}

	@Override
	public String toString() {
		var contexts = this.pathMapping;
		var contextsCount = contexts.size();
		var text = new StringBuilder("Count of contexts: %s%n".formatted(contextsCount));

		contexts.forEach((path, mapping) -> {
			text.append("Context path: %s%n".formatted(path));
			mapping.forEach((restMethods, method) -> {
				text.append("\tContext method: %s%n".formatted(restMethods));
				text.append("\tContext handler: %s%n".formatted(method.getName()));
			});
			text.append("\n");
		});

		return text.toString();
	}
}
