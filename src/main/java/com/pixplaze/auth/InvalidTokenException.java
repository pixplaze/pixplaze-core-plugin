package com.pixplaze.auth;

/// Token is missing, malformed, badly signed, expired or addressed to another server: 401 / close 4401.
public class InvalidTokenException extends Exception {
    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
