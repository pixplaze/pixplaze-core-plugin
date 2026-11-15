package com.pixplaze.auth;

/// The token cannot be checked yet: the server is not registered in Pixplaze or the signing keys are not loaded
/// (right after start, web-api unreachable). Not the client's fault: 503 / close 1013, the client retries later
/// with the same token instead of exchanging a new one.
public class VerifierNotReadyException extends InvalidTokenException {
    public VerifierNotReadyException(String message) {
        super(message);
    }

    public VerifierNotReadyException(String message, Throwable cause) {
        super(message, cause);
    }
}
