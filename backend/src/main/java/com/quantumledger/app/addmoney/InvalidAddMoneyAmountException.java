package com.quantumledger.app.addmoney;

public class InvalidAddMoneyAmountException extends RuntimeException {
    public InvalidAddMoneyAmountException(String message) {
        super(message);
    }
}
