package com.finditnow.jwt.exceptions;

public class JwtExpiredException extends JwtValidationException {
    public JwtExpiredException(Throwable cause) {
        super("JWT expired", cause);
    }
}