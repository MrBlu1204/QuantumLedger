package com.quantumledger.app.user;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException() {
    }

    public DuplicateUserException(String message) {
        super(message);
    }
}
