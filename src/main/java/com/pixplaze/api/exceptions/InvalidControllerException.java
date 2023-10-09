package com.pixplaze.api.exceptions;

import java.util.Arrays;

public class InvalidControllerException extends RuntimeException {
    public InvalidControllerException() {
        super("Invalid controller class!");
    }

    public InvalidControllerException(String message) {
        super(message);
    }

    public InvalidControllerException(Throwable cause) {
        super(cause);
    }

    public InvalidControllerException(Class<?> controllerClass, Class<?>[] parameters) {
        super("Invalid controller %s(%s)!".formatted(controllerClass.getSimpleName(), Arrays.toString(parameters)));
    }
}
