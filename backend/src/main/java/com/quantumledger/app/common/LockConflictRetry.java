package com.quantumledger.app.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;

import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Repeats an action when the database guarantees nothing was committed (deadlock, lock timeout).
 * The action must start its own transaction on every call, so call this OUTSIDE any transaction.
 */
public class LockConflictRetry {

    private static final Logger log = LoggerFactory.getLogger(LockConflictRetry.class);

    private final int maxAttempts;
    private final long baseDelayMs;
    private final Sleeper sleeper;


    public LockConflictRetry(int maxAttempts, long baseDelayMs, Sleeper sleeper) {
        if(maxAttempts < 1){
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }

        if(baseDelayMs < 0){
            throw new IllegalArgumentException("baseDelayMs must not be negative");
        }

        this.maxAttempts = maxAttempts;
        this.baseDelayMs = baseDelayMs;
        this.sleeper = sleeper;

    }

    public <T> T execute(String what, Supplier<T> action){
        RuntimeException lastFailure = null;

        for(int attempt = 1; attempt <= maxAttempts; attempt++){
            try {
                return action.get();
            } catch (RuntimeException e) {
                if(!isRetryable(e)){
                    throw e;
                }
                lastFailure = e;
                if(attempt < maxAttempts){
                    long delay = backOffMillis(attempt);
                    log.warn("{}: attempt {} of {} hit a lock conflict ({}); retrying in {} ms",
                            what, attempt, maxAttempts, e.getClass().getSimpleName(), delay);
                    sleeper.sleep(delay);
                }
            }
        }
        throw new RetriesExhaustedException(lastFailure);
    }

    /** Only failures where the database guarantees nothing was committed. */
    public static boolean isRetryable(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof PessimisticLockingFailureException) {
                return true;
            }
            if (t instanceof SQLException sql
                    && (sql.getErrorCode() == 1213 || sql.getErrorCode() == 1205
                    || "40001".equals(sql.getSQLState()))) {
                return true;
            }
        }
        return false;
    }

    /** Full jitter: a random delay between 0 and base*2^(attempt-1). */
    private long backOffMillis(int attempt) {
        long cap = baseDelayMs * (1L << (attempt - 1));
        return ThreadLocalRandom.current().nextLong(cap + 1);
    }
}
