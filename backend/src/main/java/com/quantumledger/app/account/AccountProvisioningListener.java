package com.quantumledger.app.account;

import com.quantumledger.app.user.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class AccountProvisioningListener {

    private static final Logger log = LoggerFactory.getLogger(AccountProvisioningListener.class.getName());

    private final AccountService accountService;

    AccountProvisioningListener(AccountService accountService) {
        this.accountService = accountService;
    }

    @EventListener
    void onUserRegistered(UserRegisteredEvent event) {
        Account wallet = accountService.createWalletFor(event.userId());
        log.info("Wallet {} has been created for user {}",wallet.getAccountId(), event.userId());
    }
}
