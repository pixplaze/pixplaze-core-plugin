package com.pixplaze.http.exceptions;

import com.pixplaze.http.HttpStatus;

public class HttpException extends RuntimeException {

	private final HttpStatus status;

	public HttpException() {
		this(HttpStatus.INTERNAL_ERROR);
	}

	public HttpException(final HttpStatus status) {
		this(status, status.getMessage());
	}

	public HttpException(final HttpStatus status, final String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return this.status;
	}
}
