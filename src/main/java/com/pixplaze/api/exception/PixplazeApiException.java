package com.pixplaze.api.exception;

public class PixplazeApiException extends RuntimeException {
    public PixplazeApiException() {
    }

    public PixplazeApiException(String message) {
        super(message);
    }

    public PixplazeApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public PixplazeApiException(Throwable cause) {
        super(cause);
    }

    public PixplazeApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
