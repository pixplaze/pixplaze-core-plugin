package com.pixplaze.http.server;

import com.pixplaze.exceptions.QueryParseException;

import java.util.*;

public class QueryParams {

	private final Map<String, String> values;

	protected QueryParams(String query) {
		this.values = parse(query);
	}

	protected QueryParams(Map<String, String> values) {
		this.values = values;
	}

	public boolean isEmpty() {
		return values == null || values.isEmpty();
	}

	public boolean isPresent() {
		return !isEmpty();
	}

	public boolean has(String name) {
		return this.isPresent() && this.values.containsKey(name);
	}

// TODO: изменить API извлечения параметра запроса на похожее:
//	public Optional<Integer> getAsInt(String key) {
//		var value = variables.get(key);
//		Optional<Integer> result;
//		try {
//			result = Optional.of(Integer.parseInt(value));
//		} catch (Exception e) {
//			result = Optional.empty();
//		}
//
//		return result;
//	}

	@Deprecated(since = "0.1.3-indev", forRemoval = true)
	public String getAsString(String key) {
		return values.get(key);
	}

	@Deprecated(since = "0.1.3-indev", forRemoval = true)
	public int getAsInt(String key) {
		return Integer.parseInt(values.get(key));
	}

	@Deprecated(since = "0.1.3-indev", forRemoval = true)
	public double getAsDouble(String key) {
		return Double.parseDouble(values.get(key));
	}

	@Deprecated(since = "0.1.3-indev", forRemoval = true)
	public float getAsFloat(String key) {
		return Float.parseFloat(values.get(key));
	}

	@Deprecated(since = "0.1.3-indev", forRemoval = true)
	public boolean getAsBoolean(String key) {
		return Boolean.parseBoolean(values.get(key));
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