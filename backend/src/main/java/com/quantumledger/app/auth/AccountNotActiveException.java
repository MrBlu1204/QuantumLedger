package com.quantumledger.app.auth;

public class AccountNotActiveException extends RuntimeException{

    public AccountNotActiveException(String message){
        super(message);
    }
}
