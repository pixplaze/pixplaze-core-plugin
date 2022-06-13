package com.pixplaze.exceptions;

public class UnableToDefineLocalAddress extends Exception {
	public UnableToDefineLocalAddress() {
		this("Unable to define local address automatically!");
	}

	public UnableToDefineLocalAddress(String message) {
		super(message);
	}

	public UnableToDefineLocalAddress(String message, Throwable cause) {
		super(message, cause);
	}
}
