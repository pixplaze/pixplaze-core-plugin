package com.pixplaze.exceptions;

public class CannotDefineAddressException extends Exception {
	public CannotDefineAddressException() {
		this("Unable to define local address automatically!");
	}

	public CannotDefineAddressException(String message) {
		super(message);
	}

	public CannotDefineAddressException(String message, Throwable cause) {
		super(message, cause);
	}
}
