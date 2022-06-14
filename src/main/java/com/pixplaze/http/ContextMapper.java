package com.pixplaze.http;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ContextMapper {
	private final Map<String, HashMap<Methods, Method>> pathMapping;

	public ContextMapper() {
		pathMapping = new HashMap<>();
	}

	public void mapContext(String path, Methods method, Method handler) {
		var map = pathMapping.get(path);
		if (map == null) {
			map = new HashMap<>();
			map.put(method, handler);
			pathMapping.put(path, map);
		} else {
			map.put(method, handler);
		}
	}

	public Map<String, HashMap<Methods, Method>> getContextMapping() {
		return this.pathMapping;
	}
}
