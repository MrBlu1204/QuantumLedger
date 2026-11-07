package com.quantumledger.app.common;

@FunctionalInterface
public interface Sleeper {
    void sleep(long millis);
}
