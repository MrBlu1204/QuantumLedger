package com.quantumledger.app.transfer;


import com.quantumledger.app.common.LockConflictRetry;
import com.quantumledger.app.common.RetriesExhaustedException;
import com.quantumledger.app.common.Sleeper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;


/**
 * Entry point for transfers. Deliberately NOT @Transactional: every attempt runs
 * in its own fresh transaction inside TransferService, so a rolled-back attempt
 * can simply be repeated.
 */
@Service
public class TransferFacade {

    private final TransferService transferService;
    private final LockConflictRetry retry;
;

    public TransferFacade(TransferService transferService,
                          TransferRetryProperties retryProperties,
                          Sleeper sleeper) {
        this.transferService = transferService;
        this.retry = new LockConflictRetry(retryProperties.maxAttempts(), retryProperties.baseDelayMs(),  sleeper);
    }

    public TransferService.TransferReceipt transfer(TransferService.TransferCommand command) {
        if(TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "TransferFacade must not run inside a transaction: a failed attempt could not be retried"
            );
        }

        try{
            return retry.execute("transfer", () -> transferService.transfer(command));
        } catch (RetriesExhaustedException e){
            throw new TransferBusyException(e.getLastFailure());
        }
    }

}
