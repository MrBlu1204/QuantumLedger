package com.quantumledger.app.addmoney;

import com.quantumledger.app.account.AccountNotFoundException;
import com.quantumledger.app.account.AccountRepository;
import com.quantumledger.app.account.AccountType;
import com.quantumledger.app.common.LockConflictRetry;
import com.quantumledger.app.common.RetriesExhaustedException;
import com.quantumledger.app.common.Sleeper;
import com.quantumledger.app.payment.*;
import com.quantumledger.app.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;


/** Orchestrates add-money. Deliberately NOT @Transactional: the gateway call must never sit inside a transaction. */
@Service
public class AddMoneyService {

    public record AddMoneyCommand(Long userId, Long accountId, long amountMinor,
                                  PaymentMethod method, String payerRef){ }

    public record AddMoneyReceipt(String requestRef, AddMoneyStatus status, long amountMinor,
                                  String currency, String failureReason){ }


    private static final Logger log = LoggerFactory.getLogger(AddMoneyService.class);

    private final UserService userService;
    private final AccountRepository accountRepository;
    private final PaymentGatewayFactory gatewayFactory;
    private final AddMoneyRecorder recorder;
    private final AddMoneyProperties properties;
    private final LockConflictRetry settleRetry;

    public AddMoneyService(UserService userService,
                           AccountRepository accountRepository,
                           PaymentGatewayFactory gatewayFactory,
                           AddMoneyRecorder recorder,
                           AddMoneyProperties properties,
                           AddMoneyRetryProperties retryProperties,
                           Sleeper sleeper) {
        this.userService = userService;
        this.accountRepository = accountRepository;
        this.gatewayFactory = gatewayFactory;
        this.recorder = recorder;
        this.properties = properties;
        this.settleRetry = new LockConflictRetry(retryProperties.maxAttempts(), retryProperties.baseDelayMs(), sleeper);
    }

    public AddMoneyReceipt addMoney(AddMoneyCommand command){

        if(TransactionSynchronizationManager.isActualTransactionActive()){
            throw new IllegalStateException("AddMoneyService must not run inside a transaction: the gateway call " +
                    "would hold it open");
        }

        long amount = command.amountMinor();

        if(amount < properties.minAmountMinor() || amount > properties.maxAmountMinor()){
            throw new InvalidAddMoneyAmountException("amount must be between " + properties.minAmountMinor() + " and " + properties.maxAmountMinor() + " paise");
        }

        userService.getActiveUser(command.userId);

        if(!accountRepository.existsByAccountIdAndUserIdAndAccountType(command.accountId(), command.userId(),
                AccountType.WALLET)){
            throw new AccountNotFoundException("account not found");
        }

        PaymentGateway gateway = gatewayFactory.forMethod(command.method());

        AddMoneyRequest request = recorder.open(command.userId(), command.accountId(), amount, command.method());

        GatewayResult result;

        try{
            // Called exactly once, never retried: it has a real-world side effect.
            result = gateway.collect(new GatewayRequest(
                    request.getRequestRef(), amount, request.getCurrency(), command.payerRef()
            ));
        } catch (GatewayUnavailableException ex){
            //Outcome Unknown: money may have been collected. Leave PENDING for later resolution.
            log.warn("Add-money {} left PENDING, gateway unavailable: {}", request.getRequestRef(), ex.getMessage());
            return receipt(request);
        }


        try {
            // Database-only and idompotent, so safe to repeat. Each attempt is its own transaction.
            AddMoneyRequest settled = settleRetry.execute("add-money settlement", () -> result.successful()
                    ? recorder.settleSuccess(request.getAddMoneyId(), result.gatewayRef())
                    : recorder.settleFailure(request.getAddMoneyId(), result.gatewayRef(), result.failureReason()));

            return receipt(settled);

        } catch (RetriesExhaustedException ex) {
            log.error("Add-money {} left PENDING, settlement kept hitting lock conflict (gatewayRef={}, approved={})",
                    request.getRequestRef(), result.gatewayRef(), result.successful());
            return receipt(request);
        }
    }


    private AddMoneyReceipt receipt(AddMoneyRequest request){
        return new AddMoneyReceipt(request.getRequestRef(), request.getStatus(), request.getAmountMinor(), request.getCurrency(), request.getFailureReason());
    }

}
