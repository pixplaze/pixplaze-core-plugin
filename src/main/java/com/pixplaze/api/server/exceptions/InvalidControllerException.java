package com.pixplaze.api.server.exceptions;

import java.util.Arrays;
import java.util.stream.Collectors;

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
        super("No such controller constructor: %s(%s)!".formatted(
                controllerClass.getSimpleName(),
                Arrays.stream(parameters)
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", "))));
    }
}
