package com.quantumledger.app.account;

import com.quantumledger.app.ledger.LedgerService;
import com.quantumledger.app.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final String DEFAULT_CURRENCY = "INR";

    public record WalletView(Account account, long balanceMinor) {}

    private final AccountRepository accountRepository;
    private final LedgerService ledgerService;
    private final UserService userService;

    public AccountService(AccountRepository accountRepository,
                          LedgerService ledgerService,
                          UserService userService) {
        this.accountRepository = accountRepository;
        this.ledgerService = ledgerService;
        this.userService = userService;
    }


    /** Creates the user's wallet if they don't have one yet. Safe to call twice. */
    @Transactional
    public Account createWalletFor(Long userId) {
        return accountRepository.findByUserIdAndCurrency(userId, DEFAULT_CURRENCY)
                .orElseGet(() -> accountRepository.save(Account.wallet(userId, DEFAULT_CURRENCY)));

    }

    @Transactional(readOnly = true)
    public WalletView getWalletView(Long userId) {
        userService.getActiveUser(userId);

        Account account = accountRepository.findByUserIdAndCurrency(userId, DEFAULT_CURRENCY)
                .orElseThrow(() -> new AccountNotFoundException("Wallet not found"));

        long balance = ledgerService.balanceOf(account.getAccountId(), account.getAccountType().normalBalanceSide());

        return new WalletView(account, balance);
    }


}
