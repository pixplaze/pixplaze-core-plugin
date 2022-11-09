package com.pixplaze.http.server;

import com.pixplaze.exceptions.QueryParseException;

import java.util.*;

public class QueryParams {

	private Map<String, String> values;

	protected QueryParams() {}

	protected QueryParams(String query) {
		this(parse(query));
	}

	protected QueryParams(Map<String, String> values) {
		this.values = values;
	}

	public boolean isEmpty() {
		return this.values == null || this.values.isEmpty();
	}

	public boolean isPresent() {
		return !isEmpty();
	}

	public boolean has(String name) {
		return this.isPresent() && this.values.containsKey(name);
	}

	public Optional<Integer> getAsInt(final String key) {
		var value = values.get(key);
		try {
			return Optional.of(Integer.parseInt(value));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public Optional<Float> getAsFloat(final String key) {
		var value = this.values.get(key);
		try {
			return Optional.of(Float.parseFloat(value));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public Optional<Double> getAsDouble(final String key) {
		var value = this.values.get(key);
		try {
			return Optional.of(Double.parseDouble(value));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public Optional<Boolean> getAsBoolean(final String key) {
		var value = this.values.getOrDefault(key, "");
		if (value.equalsIgnoreCase("true"))
			return Optional.of(true);
		if (value.equalsIgnoreCase("false"))
			return Optional.of(false);
		return Optional.empty();
	}

	public Optional<String> getAsString(final String key) {
		return Optional.ofNullable(this.values.get(key));
	}

	public static Map<String, String> parse(String query) throws QueryParseException {
//		query = Optional.ofNullable(query).orElse("");
		var map = new HashMap<String, String>();
		if (query == null || query.isBlank()) return map;

		var key = "";
		var val = "";
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