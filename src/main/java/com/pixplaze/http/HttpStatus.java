package com.pixplaze.http;

import static java.net.HttpURLConnection.*;

public enum HttpStatus {
	/* 2XX: SUCCESS */
	OK(HTTP_OK, "OK"),
	CREATED(HTTP_CREATED, "Created"),
	ACCEPTED(HTTP_ACCEPTED, "Accepted"),
	NOT_AUTHORITATIVE(HTTP_NOT_AUTHORITATIVE, "Non-Authoritative Information"),
	NO_CONTENT(HTTP_NO_CONTENT, "No Content"),
	RESET(HTTP_RESET, "Reset Content"),
	PARTIAL(HTTP_PARTIAL, "Partial Content"),

	/* 3XX: RELOCATION / REDIRECT */
	MULT_CHOICE(HTTP_MULT_CHOICE, "Multiple Choices"),
	MOVED_PERM(HTTP_MOVED_PERM, "Moved Permanently"),
	MOVED_TEMP(HTTP_MOVED_TEMP, "Temporary Redirect"),
	SEE_OTHER(HTTP_SEE_OTHER, "See Other"),
	NOT_MODIFIED(HTTP_NOT_MODIFIED, "Not Modified"),
	USE_PROXY(HTTP_USE_PROXY, "Use Proxy"),

	/* 4XX: CLIENT ERROR */
	BAD_REQUEST(HTTP_BAD_REQUEST, "Bad Request"),
	UNAUTHORIZED(HTTP_UNAUTHORIZED, "Unauthorized"),
	PAYMENT_REQUIRED(HTTP_PAYMENT_REQUIRED, "Payment Required"),
	FORBIDDEN(HTTP_FORBIDDEN, "Forbidden"),
	NOT_FOUND(HTTP_NOT_FOUND, "Not Found"),
	BAD_METHOD(HTTP_BAD_METHOD, "Method Not Allowed"),
	NOT_ACCEPTABLE(HTTP_NOT_ACCEPTABLE, "Not Acceptable"),
	PROXY_AUTH(HTTP_PROXY_AUTH, "Proxy Authentication Required"),
	CLIENT_TIMEOUT(HTTP_CLIENT_TIMEOUT, "Request Time-Out"),
	CONFLICT(HTTP_CONFLICT, "Conflict"),
	GONE(HTTP_GONE, "Gone"),
	LENGTH_REQUIRED(HTTP_LENGTH_REQUIRED, "Length Required"),
	PRECON_FAILED(HTTP_PRECON_FAILED, "Precondition Failed"),
	ENTITY_TOO_LARGE(HTTP_ENTITY_TOO_LARGE, "Request Entity Too Large"),
	REQ_TOO_LONG(HTTP_REQ_TOO_LONG, "Request-URI Too Large"),
	UNSUPPORTED_TYPE(HTTP_UNSUPPORTED_TYPE, "Unsupported Media Type"),

	/* 5XX: SERVER ERROR */
	INTERNAL_ERROR(HTTP_INTERNAL_ERROR, "Internal Server Error"),
	NOT_IMPLEMENTED(HTTP_NOT_IMPLEMENTED, "Not Implemented"),
	BAD_GATEWAY(HTTP_BAD_GATEWAY, "Bad Gateway"),
	UNAVAILABLE(HTTP_UNAVAILABLE, "Service Unavailable"),
	GATEWAY_TIMEOUT(HTTP_GATEWAY_TIMEOUT, "Gateway Timeout"),
	VERSION(HTTP_VERSION, "HTTP Version Not Supported");

	private final int code;
	private final String message;

	HttpStatus(final int code, final String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public HttpStatus forCode(final int code) {
		for (var status: HttpStatus.values()) {
			if (status.code == code) return status;
		}
		return null;
	}
}
