package com.quantumledger.app.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final String DEFAULT_CURRENCY = "INR";
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }


    /** Creates the user's wallet if they don't have one yet. Safe to call twice. */
    @Transactional
    public Account createWalletFor(Long userId) {
        return accountRepository.findByUserIdAndCurrency(userId, DEFAULT_CURRENCY)
                .orElseGet(() -> accountRepository.save(Account.wallet(userId, DEFAULT_CURRENCY)));

    }


}
