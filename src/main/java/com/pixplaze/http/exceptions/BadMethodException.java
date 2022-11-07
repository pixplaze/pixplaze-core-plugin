package com.pixplaze.http.exceptions;

import com.pixplaze.http.HttpStatus;

public class BadMethodException extends HttpException {
	public BadMethodException() {
		this("Method is not defined!");
	}

	public BadMethodException(String message) {
		super(HttpStatus.BAD_METHOD, null, message);
	}

	public BadMethodException(String method, String context) {
		this("Method %s is not defined as handler for '%s'!".formatted(method, context));
	}
}
