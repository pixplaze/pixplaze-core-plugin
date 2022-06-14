package com.pixplaze.http;

public enum Methods {
	GET,
	POST,
	PUT,
	DELETE;

	boolean equals(String str) {
		return this.name().equalsIgnoreCase(str);
	}
}
