package com.pixplaze.http;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ContextMapper {
	private final Map<String, HashMap<RestMethods, Method>> pathMapping;

	public ContextMapper() {
		pathMapping = new HashMap<>();
	}

	public void mapContext(String path, RestMethods method, Method handler) {
		var map = pathMapping.get(path);
		if (map == null) {
			map = new HashMap<>();
			map.put(method, handler);
			pathMapping.put(path, map);
		} else {
			map.put(method, handler);
		}
	}

	public Map<String, HashMap<RestMethods, Method>> getContextMapping() {
		return this.pathMapping;
	}
}
