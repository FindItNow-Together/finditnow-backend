package com.finditnow.auth.exceptions;

public class CredentialException extends RuntimeException {
    public CredentialException() {
    }

    public CredentialException(String message) {
        super(message);
    }

    public CredentialException(String message, Throwable cause) {
        super(message, cause);
    }

    public CredentialException(Throwable cause) {
        super(cause);
    }

    public CredentialException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
