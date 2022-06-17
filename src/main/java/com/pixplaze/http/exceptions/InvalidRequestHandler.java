package com.pixplaze.http.exceptions;

import java.lang.reflect.Method;

public class InvalidRequestHandler extends RuntimeException {
	public InvalidRequestHandler(String message) {
		super(message);
	}

	public InvalidRequestHandler(Method method) {
		this("Invalid request handler: %s!".formatted(method.getName()));
	}
}
