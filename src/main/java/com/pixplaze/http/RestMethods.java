package com.pixplaze.http;

public enum RestMethods {
	GET,
	POST,
	PUT,
	DELETE;

	boolean equals(String str) {
		return this.name().equalsIgnoreCase(str);
	}
}
