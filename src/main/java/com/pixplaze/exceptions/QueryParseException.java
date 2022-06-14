package com.pixplaze.exceptions;

public class QueryParseException extends RuntimeException {

	public QueryParseException() {
		this("Unable to parse query params!");
	}

	public QueryParseException(String message) {
		super(message);
	}
}
