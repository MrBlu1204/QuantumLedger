package com.quantumledger.app.addmoney;

import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.account.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddMoneyRequestTest extends AbstractIntegrationTest {

    @Autowired
    AddMoneyRequestRepository repository;

    private AddMoneyRequest savePending(Account wallet) {
        return repository.save(AddMoneyRequest.pending(
                wallet.getUserId(), wallet.getAccountId(), 50_000, "INR"));
    }

    private long someLedgerTransactionId(Account wallet) {
        fund(wallet, 100);
        return jdbc.queryForObject("SELECT MAX(ledger_transaction_id) FROM ledger_transactions", Long.class);
    }

    @Test
    void pendingRequestIsSavedWithoutGatewayRefOrLedgerTransaction() {
        Account wallet = newWallet("Asha");
        AddMoneyRequest saved = savePending(wallet);

        AddMoneyRequest found = repository.findById(saved.getAddMoneyId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(AddMoneyStatus.PENDING);
        assertThat(found.getRequestRef()).hasSize(36);
        assertThat(found.getAmountMinor()).isEqualTo(50_000);
        assertThat(found.getGatewayRef()).isNull();
        assertThat(found.getLedgerTransactionId()).isNull();
    }

    @Test
    void markSuccessStoresGatewayRefAndLedgerTransaction() {
        Account wallet = newWallet("Ben");
        long txnId = someLedgerTransactionId(wallet);

        AddMoneyRequest r = savePending(wallet);
        r.markSuccess("GW-1", txnId);
        repository.save(r);

        AddMoneyRequest found = repository.findById(r.getAddMoneyId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(AddMoneyStatus.SUCCESS);
        assertThat(found.getGatewayRef()).isEqualTo("GW-1");
        assertThat(found.getLedgerTransactionId()).isEqualTo(txnId);
    }

    @Test
    void markFailedStoresReasonAndNoLedgerTransaction() {
        Account wallet = newWallet("Chitra");

        AddMoneyRequest r = savePending(wallet);
        r.markFailed("GW-2", "DECLINED_BY_BANK");
        repository.save(r);

        AddMoneyRequest found = repository.findById(r.getAddMoneyId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(AddMoneyStatus.FAILED);
        assertThat(found.getFailureReason()).isEqualTo("DECLINED_BY_BANK");
        assertThat(found.getLedgerTransactionId()).isNull();
    }

    @Test
    void finishedRequestCannotChangeAgain() {
        AddMoneyRequest r = AddMoneyRequest.pending(1L, 2L, 100, "INR");
        r.markFailed(null, "TIMEOUT");

        assertThatThrownBy(() -> r.markSuccess("GW-3", 9L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void databaseRejectsSuccessWithoutLedgerTransaction() {
        Account wallet = newWallet("Dev");
        AddMoneyRequest r = savePending(wallet);

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE add_money_requests SET status = 'SUCCESS' WHERE add_money_id = ?",
                r.getAddMoneyId()))
                .isInstanceOf(DataAccessException.class);
    }


}
