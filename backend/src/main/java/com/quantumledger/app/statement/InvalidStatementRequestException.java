package com.quantumledger.app.statement;

public class InvalidStatementRequestException extends RuntimeException {

    private final String code;

    public InvalidStatementRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
