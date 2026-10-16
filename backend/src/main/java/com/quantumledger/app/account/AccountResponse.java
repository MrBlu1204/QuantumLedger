package com.quantumledger.app.account;

public record AccountResponse(Long accountId, String accountType, String currency,
                              String status, long balanceMinor) {

    public static AccountResponse from(AccountService.WalletView walletView) {
        Account a = walletView.account();
        return new AccountResponse(a.getAccountId(), a.getAccountType().name(), a.getCurrency(), a.getStatus(),
                walletView.balanceMinor());
    }
}
