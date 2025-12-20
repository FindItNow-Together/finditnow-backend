package com.finditnow.userservice.exception;

public class DuplicateResourceException extends RuntimeException{
    public DuplicateResourceException() {

    }

    public DuplicateResourceException(String message) {
        super(message);
    }
}
