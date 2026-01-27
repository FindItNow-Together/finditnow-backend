package com.finditnow.jwt.exceptions;

public class JwtInvalidException extends JwtValidationException {
    public JwtInvalidException(Throwable cause) {
        super("JWT invalid", cause);
    }
}
