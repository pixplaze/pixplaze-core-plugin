package com.pixplaze.webapi;

/// Unexpected web-api response (not an OAuth error) or a network failure.
public class WebApiException extends RuntimeException {
    private final int status;

    public WebApiException(int status, String message) {
        super("web-api responded " + status + ": " + message);
        this.status = status;
    }

    public WebApiException(String message, Throwable cause) {
        super(message, cause);
        this.status = -1;
    }

    /// HTTP status, or `-1` for a network failure.
    public int status() {
        return status;
    }

    public boolean isUnauthorized() {
        return status == 401;
    }
}
