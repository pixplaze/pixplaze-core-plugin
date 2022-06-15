package com.pixplaze.http.server;

import com.pixplaze.exceptions.QueryParseException;

import java.util.*;

public class QueryParams {

	private final Map<String, String> variables;

	protected QueryParams(String query) {
		this.variables = parse(query);
	}

	protected QueryParams(Map<String, String> variables) {
		this.variables = variables;
	}

	public boolean isEmpty() {
		return variables == null || variables.isEmpty();
	}

	public boolean isPresent() {
		return !isEmpty();
	}

	public boolean has(String name) {
		return variables.containsKey(name);
	}

	public int getAsInt(String key) {
		return Integer.parseInt(variables.get(key));
	}

	public String getAsString(String key) {
		return variables.get(key);
	}

	public double getAsDouble(String key) {
		return Double.parseDouble(variables.get(key));
	}

	public float getAsFloat(String key) {
		return Float.parseFloat(key);
	}

	public boolean getAsBoolean(String key) {
		return Boolean.parseBoolean(key);
	}

	public static Map<String, String> parse(String query) throws QueryParseException {
		if (query == null || query.isBlank()) {
			return null;
		}

		var key = "";
		var val = "";
		var map = new HashMap<String, String>();
		var stk = new StringTokenizer(query, "?&=");
		if (stk.countTokens() < 2) throw new QueryParseException();
		try {
			while (stk.hasMoreTokens()) {
				key = stk.nextToken();
				val = stk.nextToken();
				map.put(key, val);
			}
		} catch (NoSuchElementException e) {
			throw new QueryParseException("No value specified for the '%s=' parameter!".formatted(key));
		} catch (IllegalArgumentException e) {
			throw new QueryParseException(e.getMessage());
		}
		return map;
	}
}