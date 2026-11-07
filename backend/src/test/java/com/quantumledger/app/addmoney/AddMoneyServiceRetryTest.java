package com.quantumledger.app.addmoney;

import com.quantumledger.app.account.AccountRepository;
import com.quantumledger.app.payment.GatewayResult;
import com.quantumledger.app.payment.PaymentGateway;
import com.quantumledger.app.payment.PaymentGatewayFactory;
import com.quantumledger.app.payment.PaymentMethod;
import com.quantumledger.app.user.UserService;
import com.quantumledger.app.addmoney.AddMoneyService.AddMoneyCommand;
import com.quantumledger.app.addmoney.AddMoneyService.AddMoneyReceipt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class AddMoneyServiceRetryTest {

    private final UserService userService = mock(UserService.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final PaymentGatewayFactory gatewayFactory = mock(PaymentGatewayFactory.class);
    private final PaymentGateway gateway = mock(PaymentGateway.class);
    private final AddMoneyRecorder recorder = mock(AddMoneyRecorder.class);
    private final List<Long> sleeps = new ArrayList<>();

    private final AddMoneyService service = new AddMoneyService(
            userService, accountRepository, gatewayFactory, recorder,
            new AddMoneyProperties(100, 10_000_000),
            new AddMoneyRetryProperties(3, 0),
            sleeps::add);

    private static AddMoneyRequest pending() {
        return AddMoneyRequest.pending(1L, 2L, 50_000, "INR", PaymentMethod.UPI);
    }

    private static AddMoneyRequest succeeded() {
        AddMoneyRequest r = pending();
        r.markSuccess("GW-1", 99L);
        return r;
    }

    private static AddMoneyRequest failed() {
        AddMoneyRequest r = pending();
        r.markFailed("GW-1", "DECLINED_BY_BANK");
        return r;
    }

    private static AddMoneyCommand command() {
        return new AddMoneyCommand(1L, 2L, 50_000, PaymentMethod.UPI, "asha@okbank");
    }

    private static RuntimeException lockConflict() {
        return new PessimisticLockingFailureException("lock wait timeout");
    }

    @BeforeEach
    void wire() {
        when(accountRepository.existsByAccountIdAndUserIdAndAccountType(any(), any(), any())).thenReturn(true);
        when(gatewayFactory.forMethod(PaymentMethod.UPI)).thenReturn(gateway);
        when(recorder.open(any(), any(), anyLong(), any())).thenReturn(pending());
    }

    @Test
    void settleSuccessIsRetriedAndTheGatewayIsCalledOnlyOnce() {
        when(gateway.collect(any())).thenReturn(GatewayResult.success("GW-1"));
        when(recorder.settleSuccess(any(), any()))
                .thenThrow(lockConflict())
                .thenThrow(lockConflict())
                .thenReturn(succeeded());

        AddMoneyReceipt receipt = service.addMoney(command());

        assertThat(receipt.status()).isEqualTo(AddMoneyStatus.SUCCESS);
        verify(recorder, times(3)).settleSuccess(any(), any());
        verify(gateway, times(1)).collect(any());
        assertThat(sleeps).hasSize(2);
    }

    @Test
    void settleFailureIsRetriedToo() {
        when(gateway.collect(any())).thenReturn(GatewayResult.declined("GW-1", "DECLINED_BY_BANK"));
        when(recorder.settleFailure(any(), any(), any()))
                .thenThrow(lockConflict())
                .thenReturn(failed());

        AddMoneyReceipt receipt = service.addMoney(command());

        assertThat(receipt.status()).isEqualTo(AddMoneyStatus.FAILED);
        verify(recorder, times(2)).settleFailure(any(), any(), any());
        verify(recorder, never()).settleSuccess(any(), any());
    }

    @Test
    void whenRetriesRunOutTheRequestStaysPendingAndTheGatewayIsNotCalledAgain() {
        when(gateway.collect(any())).thenReturn(GatewayResult.success("GW-1"));
        when(recorder.settleSuccess(any(), any())).thenThrow(lockConflict());

        AddMoneyReceipt receipt = service.addMoney(command());

        assertThat(receipt.status()).isEqualTo(AddMoneyStatus.PENDING);
        verify(recorder, times(3)).settleSuccess(any(), any());
        verify(gateway, times(1)).collect(any());
    }

    @Test
    void otherFailuresAreNotRetried() {
        when(gateway.collect(any())).thenReturn(GatewayResult.success("GW-1"));
        when(recorder.settleSuccess(any(), any())).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> service.addMoney(command()))
                .isInstanceOf(IllegalStateException.class);

        verify(recorder, times(1)).settleSuccess(any(), any());
        assertThat(sleeps).isEmpty();
    }
}
