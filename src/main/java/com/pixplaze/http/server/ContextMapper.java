package com.pixplaze.http.server;

import com.pixplaze.http.annotations.*;

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

	protected static ContextMapper scanHandlers(Method[] methods) {
		var contextMapper = new ContextMapper();
		for (var method: methods) {
			if (method.isAnnotationPresent(RequestHandler.class)) {
				var annotation = method.getAnnotation(RequestHandler.class);
				contextMapper.mapContext(annotation.path(), annotation.method(), method);
			} else if (method.isAnnotationPresent(GetHandler.class)) {
				var annotation = method.getAnnotation(GetHandler.class);
				contextMapper.mapContext(annotation.value(), annotation.method(), method);
			} else if (method.isAnnotationPresent(PostHandler.class)) {
				var annotation = method.getAnnotation(PostHandler.class);
				contextMapper.mapContext(annotation.value(), annotation.method(), method);
			} else if (method.isAnnotationPresent(PutHandler.class)) {
				var annotation = method.getAnnotation(PutHandler.class);
				contextMapper.mapContext(annotation.value(), annotation.method(), method);
			} else if (method.isAnnotationPresent(DeleteHandler.class)) {
				var annotation = method.getAnnotation(DeleteHandler.class);
				contextMapper.mapContext(annotation.value(), annotation.method(), method);
			}
		}
		return contextMapper;
	}
}
