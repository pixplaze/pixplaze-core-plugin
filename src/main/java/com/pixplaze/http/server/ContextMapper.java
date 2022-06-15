package com.pixplaze.http.server;

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
}
