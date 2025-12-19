package com.finditnow.auth.exceptions;

public class NoSuchCredentialException extends RuntimeException {
    public NoSuchCredentialException() {
    }

    public NoSuchCredentialException(String message) {
        super(message);
    }

    public NoSuchCredentialException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchCredentialException(Throwable cause) {
        super(cause);
    }

    public NoSuchCredentialException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
