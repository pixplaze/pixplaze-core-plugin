package com.pixplaze.http.exceptions;

public class BadMethodException extends Exception {
	public BadMethodException() {
		this("Method is not defined!");
	}

	public BadMethodException(String message) {
		super(message);
	}

	public BadMethodException(String method, String context) {
		this("Method %s is not defined as handler for '%s'!".formatted(method, context));
	}
}
