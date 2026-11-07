package com.quantumledger.app.addmoney;

import com.quantumledger.app.account.AccountRepository;
import com.quantumledger.app.account.AccountService;
import com.quantumledger.app.ledger.LedgerTransactionType;
import com.quantumledger.app.payment.PaymentMethod;
import com.quantumledger.app.posting.PostingLine;
import com.quantumledger.app.posting.PostingRequest;
import com.quantumledger.app.posting.PostingResult;
import com.quantumledger.app.posting.PostingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddMoneyRecorder {

    private static final Logger log = LoggerFactory.getLogger(AddMoneyRecorder.class);
    private static final String BANK_ACCOUNT_CODE = "BANK_SETTLEMENT";

    private final AddMoneyRequestRepository repository;
    private final AccountRepository accountRepository;
    private final PostingService postingService;

    public AddMoneyRecorder(AddMoneyRequestRepository repository,
                            AccountRepository accountRepository,
                            PostingService postingService) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.postingService = postingService;
    }

    /** Transaction 1: durable record of intent, before any money movement. */
    @Transactional
    public AddMoneyRequest open(Long  userId, Long  accountId, long amountMinor, PaymentMethod method) {
        return repository.save(AddMoneyRequest.pending(
                userId, accountId, amountMinor, AccountService.DEFAULT_CURRENCY, method
        ));

    }

    /** Transaction 2(approved) : credit the wallet and mark SUCCESS, atomically. No-op if already settled. */
    @Transactional
    public AddMoneyRequest settleSuccess(Long addMoneyRequestId, String gatewayRef) {
        AddMoneyRequest request = lockRequest(addMoneyRequestId);

        if(request.getStatus() != AddMoneyStatus.PENDING) {
            log.warn("Add-money {} already {}, success settlement ignored",
                    request.getRequestRef(), request.getStatus());
            return request;
        }

        Long bankId = accountRepository.findIdByAccountCode(BANK_ACCOUNT_CODE)
                .orElseThrow(() -> new IllegalStateException(BANK_ACCOUNT_CODE + "account is missing"));

        long amount = request.getAmountMinor();

        PostingResult posting = postingService.post(new PostingRequest(
                LedgerTransactionType.TOP_UP, "Add money", request.getCurrency(),
                List.of(PostingLine.debit(bankId, amount),
                        PostingLine.credit(request.getAccountId(), amount))
        ));

        request.markSuccess(gatewayRef, posting.ledgerTransactionId());

        log.info("Add-money {} succeeded: amount={} ledgerTransaction={}",
                request.getRequestRef(), amount, posting.ledgerTransactionId());

        return request;
    }

    /**Transaction 2(declined): mark FAILED. No-op if already settle, so a late failure never undoes a success. */
    @Transactional
    public AddMoneyRequest settleFailure(Long addMoneyRequestId, String gatewayRef, String failureReason) {
        AddMoneyRequest request = lockRequest(addMoneyRequestId);

        if(request.getStatus() != AddMoneyStatus.PENDING) {
            log.warn("Add-money {} already {}, failure settlement ignored",
                    request.getRequestRef(), request.getStatus());
            return request;
        }

        request.markFailed(gatewayRef, failureReason);
        log.info("Add-money {} failed: {}", request.getRequestRef(), failureReason);

        return request;
    }

    private AddMoneyRequest lockRequest(Long addMoneyRequestId) {
        return repository.findByIdForUpdate(addMoneyRequestId)
                .orElseThrow(() -> new IllegalStateException("add-money request not found: " + addMoneyRequestId));
    }
}
