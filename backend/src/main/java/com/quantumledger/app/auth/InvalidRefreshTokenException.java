package com.quantumledger.app.auth;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {

        super("Refresh token is invalid or expired");
    }
}
