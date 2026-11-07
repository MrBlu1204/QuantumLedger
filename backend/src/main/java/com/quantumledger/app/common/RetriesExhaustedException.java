package com.quantumledger.app.common;

/** Throws only when every attempt failed with a retryable lock conflict. */
public class RetriesExhaustedException extends RuntimeException {

    private final RuntimeException lastFailure;

    public RetriesExhaustedException(RuntimeException lastFailure) {
        super("gave up after repeated lock conficts", lastFailure);
        this.lastFailure = lastFailure;
    }

    public RuntimeException getLastFailure() {
        return lastFailure;
    }
}
