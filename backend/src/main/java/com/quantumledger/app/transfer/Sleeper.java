package com.quantumledger.app.transfer;

@FunctionalInterface
public interface Sleeper {
    void sleep(long millis);
}
