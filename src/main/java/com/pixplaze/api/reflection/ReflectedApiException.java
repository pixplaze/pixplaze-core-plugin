package com.pixplaze.api.reflection;

public class ReflectedApiException extends RuntimeException {
    public ReflectedApiException() {
        super();
    }

    public ReflectedApiException(String message) {
        super(message);
    }

    public ReflectedApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReflectedApiException(Throwable cause) {
        super(cause);
    }

    protected ReflectedApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
