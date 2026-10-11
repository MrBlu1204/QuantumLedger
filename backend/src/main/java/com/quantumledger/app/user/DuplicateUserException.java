package com.quantumledger.app.user;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException() {
        super("User with the same email id or phone already exists");
    }

    public DuplicateUserException(String message) {
        super(message);
    }
}
