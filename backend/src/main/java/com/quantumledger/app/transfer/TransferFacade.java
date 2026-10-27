package com.quantumledger.app.transfer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Entry point for transfers. Deliberately NOT @Transactional: every attempt runs
 * in its own fresh transaction inside TransferService, so a rolled-back attempt
 * can simply be repeated.
 */
@Service
public class TransferFacade {

    private static final Logger log = LoggerFactory.getLogger(TransferFacade.class);

    private final TransferService transferService;
    private final TransferRetryProperties retry;
    private final Sleeper sleeper;

    public TransferFacade(TransferService transferService,
                          TransferRetryProperties retry,
                          Sleeper sleeper) {
        this.transferService = transferService;
        this.retry = retry;
        this.sleeper = sleeper;
    }

    public TransferService.TransferReceipt transfer(TransferService.TransferCommand command) {
        if(TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "TransferFacade must not run inside a transaction: a failed attempt could not be retried"
            );
        }

        RuntimeException lastFailure = null;
        for(int attempt = 1; attempt <= retry.maxAttempts(); attempt++) {
            try {
                return transferService.transfer(command);
            } catch (RuntimeException e) {
                if(!isRetryable(e)){
                    throw e;
                }
                lastFailure = e;
                if(attempt < retry.maxAttempts()) {
                    long delay = backOffMillis(attempt);
                    log.warn("Transfer attempt {} of {} hit a lock conflict ({}); retrying in {] ms",
                            attempt, retry.maxAttempts(), e.getClass().getSimpleName(), delay);
                    sleeper.sleep(delay);
                }
            }
        }
        throw new TransferBusyException(lastFailure);
    }

    /** Only failures where the database guarantees nothing was committed. */
    static boolean isRetryable(Throwable error) {
        for(Throwable t = error; t != null; t = t.getCause()) {
            if( t instanceof PessimisticLockingFailureException){
                return true;
            }

            if( t instanceof SQLException sql
                            && (sql.getErrorCode() == 1213 || sql.getErrorCode() == 1205
                                    || "40001".equals(sql.getSQLState()))
            ){
                return true;
            }
        }
        return false;
    }

    /** Full jitter: a random delay between 0 and base*2^(attempt-1). */
    private long backOffMillis(int attempt) {
        long cap = retry.baseDelayMs() * (1L <<(attempt - 1));
        return ThreadLocalRandom.current().nextLong(cap + 1);
    }
}
