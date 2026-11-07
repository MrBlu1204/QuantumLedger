package com.quantumledger.app.addmoney;

import com.quantumledger.app.account.Account;
import com.quantumledger.app.addmoney.AddMoneyService.AddMoneyCommand;
import com.quantumledger.app.addmoney.AddMoneyService.AddMoneyReceipt;
import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.payment.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AddMoneyFaultInjectionTest extends AbstractIntegrationTest {

    @Autowired
    AddMoneyService service;

    @MockitoSpyBean
    AddMoneyRecorder recorder;

    private AddMoneyCommand command(Account wallet, String payerRef) {
        return new AddMoneyCommand(wallet.getUserId(), wallet.getAccountId(), 50_000,
                PaymentMethod.UPI, payerRef);
    }

    @Test
    void aLockConflictDuringSettlementIsRetriedAndTheWalletIsCreditedOnce() {
        Account wallet = newWallet("Asha");
        doThrow(new PessimisticLockingFailureException("lock wait timeout"))
                .doCallRealMethod()
                .when(recorder).settleSuccess(any(), any());

        AddMoneyReceipt receipt = service.addMoney(command(wallet, "asha@okbank"));

        assertThat(receipt.status()).isEqualTo(AddMoneyStatus.SUCCESS);
        verify(recorder, times(2)).settleSuccess(any(), any());
        assertThat(balanceOf(wallet)).isEqualTo(50_000);
        assertThat(countEntries()).isEqualTo(2);
        assertThat(unbalancedTransactions()).isEmpty();
        assertThat(balanceMismatches()).isEmpty();
    }

    @Test
    void whenSettlementNeverSucceedsTheRequestStaysPendingAndNoMoneyMoves() {
        Account wallet = newWallet("Ben");
        doThrow(new PessimisticLockingFailureException("lock wait timeout"))
                .when(recorder).settleSuccess(any(), any());

        AddMoneyReceipt receipt = service.addMoney(command(wallet, "ben@okbank"));

        assertThat(receipt.status()).isEqualTo(AddMoneyStatus.PENDING);
        verify(recorder, times(5)).settleSuccess(any(), any());   // app.add-money.retry.max-attempts
        assertThat(balanceOf(wallet)).isZero();
        assertThat(countEntries()).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM add_money_requests", String.class))
                .isEqualTo("PENDING");
    }
}
