package com.pixplaze.http.server;

import com.pixplaze.http.HttpController;
import com.pixplaze.http.annotations.*;
import com.pixplaze.http.exceptions.InvalidRequestHandler;
import com.pixplaze.http.server.validation.HandlerValidationStrategy;

import java.lang.reflect.Method;
import java.util.*;

public class ContextMapper {
	private final Map<String, HashMap<String, Method>> pathMapping;
	private final HandlerValidationStrategy validationStrategy;

	protected ContextMapper(HttpController controller, HandlerValidationStrategy validationStrategy) {
		var methods = controller.getClass().getMethods();
		this.validationStrategy = validationStrategy;
		this.pathMapping = new HashMap<>();
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
}
