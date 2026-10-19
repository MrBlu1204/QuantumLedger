package com.quantumledger.app.posting;


import com.quantumledger.app.account.Account;
import com.quantumledger.app.account.AccountFrozenException;
import com.quantumledger.app.account.AccountRepository;
import com.quantumledger.app.ledger.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostingService {

    private static final Logger log = LoggerFactory.getLogger(PostingService.class);

    private final AccountRepository accountRepository;
    private final LedgerTransactionRespository ledgerTransactionRespository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public PostingService(AccountRepository accountRepository,
                          LedgerTransactionRespository transactionRespository,
                          LedgerEntryRepository entryRepository) {

        this.accountRepository = accountRepository;
        this.ledgerTransactionRespository = transactionRespository;
        this.ledgerEntryRepository = entryRepository;
    }

    /** The only way money moves. Joins the caller's transaction, or starts one. */
    @Transactional
    public PostingResult post(PostingRequest request){
        Map<Long, Account> locked = lockAccounts(request);

        for(Account account : locked.values()){
            if(!account.getCurrency().equals(account.getCurrency())){
                throw new InvalidPostingException(
                        "currency mismatch on account " + account.getAccountId()
                );
            }
            if(!account.isActive()){
                throw new AccountFrozenException("Account is not active");
            }
        }

        for(PostingLine line : request.lines()){
            locked.get(line.accountId()).applyEntry(line.direction(), line.amountMinor());
        }

        LedgerTransaction ledgerTransaction = ledgerTransactionRespository.save(
                new LedgerTransaction(request.type(), request.description())
        );

        List<LedgerEntry> entries = request.lines().stream()
                .map(line -> new LedgerEntry(ledgerTransaction.getLedgerTransactionId(),
                        line.accountId(), line.direction(), line.amountMinor(), request.currency()))
                .toList();

        ledgerEntryRepository.saveAll(entries);

        accountRepository.flush();

        log.info("Posted {} {} line={} total={}", request.type(),
                ledgerTransaction.getTransactionRef(), entries.size() , request.totalAmountMinor());

        return new PostingResult(ledgerTransaction.getLedgerTransactionId(), ledgerTransaction.getTransactionRef());
    }

    /** Locks every account the posting touches, always in ascending id order. */
    private Map<Long, Account> lockAccounts(PostingRequest request) {
        Map<Long, Account> locked = new LinkedHashMap<>();
        for(Long id : request.accountIdsInLockOrder()){
            Account account = accountRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new InvalidPostingException("unknown account " + id));
            locked.put(id, account);
        }

        return locked;
    }


}
