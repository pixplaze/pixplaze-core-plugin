package com.pixplaze.http.exceptions;

import com.pixplaze.http.HttpStatus;

public class HttpException extends RuntimeException {

	private final HttpStatus status;

	public HttpException(Throwable cause) {
		this(HttpStatus.INTERNAL_ERROR, cause);
	}

	public HttpException(final HttpStatus status, Throwable cause) {
		this(status, cause, status.getMessage());
	}

	public HttpException(final HttpStatus status, Throwable cause, final String message) {
		super(message, cause);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return this.status;
	}
}
