package com.quantumledger.app.account;

import com.quantumledger.app.ledger.Direction;

public enum AccountType {
    WALLET,
    ASSET,
    INCOME;


    public Direction normalBalanceSide(){
        return this == ASSET ? Direction.DEBIT : Direction.CREDIT;
    }
}
