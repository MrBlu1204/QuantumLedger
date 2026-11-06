package com.quantumledger.app.addmoney;

import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.account.Account;
import com.quantumledger.app.account.AccountNotFoundException;
import com.quantumledger.app.payment.PaymentMethod;
import com.quantumledger.app.addmoney.AddMoneyService.AddMoneyCommand;
import com.quantumledger.app.addmoney.AddMoneyService.AddMoneyReceipt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class AddMoneyServiceTest extends AbstractIntegrationTest {

    @Autowired
    AddMoneyService service;
    @Autowired
    PlatformTransactionManager txManager;

    private AddMoneyCommand command(Account wallet, long amountMinor, PaymentMethod method,
                                                     String payerRef) {
        return new AddMoneyCommand(wallet.getUserId(), wallet.getAccountId(), amountMinor, method, payerRef);
    }

    private long requestCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM add_money_requests", Long.class);
    }

    @Test
    void approvedPaymentCreditsTheWallet() {
        Account wallet = newWallet("Asha");

        AddMoneyReceipt receipt = service.addMoney(command(wallet, 50_000, PaymentMethod.UPI, "asha@okbank"));

        assertThat(receipt.status()).isEqualTo(AddMoneyStatus.SUCCESS);
        assertThat(receipt.amountMinor()).isEqualTo(50_000);
        assertThat(balanceOf(wallet)).isEqualTo(50_000);
        assertThat(unbalancedTransactions()).isEmpty();
        assertThat(balanceMismatches()).isEmpty();
    }

    @Test
    void declinedPaymentIsRecordedAsFailedAndMovesNoMoney() {
        Account wallet = newWallet("Ben");

        AddMoneyReceipt receipt = service.addMoney(command(wallet, 50_000, PaymentMethod.UPI, "ben@fail"));

        assertThat(receipt.status()).isEqualTo(AddMoneyStatus.FAILED);
        assertThat(receipt.failureReason()).isEqualTo("DECLINED_BY_BANK");
        assertThat(balanceOf(wallet)).isZero();
        assertThat(countEntries()).isZero();
        assertThat(requestCount()).isEqualTo(1);
    }

    @Test
    void unavailableGatewayLeavesTheRequestPendingAndMovesNoMoney() {
        Account wallet = newWallet("Chitra");

        AddMoneyReceipt receipt = service.addMoney(command(wallet, 50_000, PaymentMethod.UPI, "chitra@timeout"));

        assertThat(receipt.status()).isEqualTo(AddMoneyStatus.PENDING);
        assertThat(balanceOf(wallet)).isZero();
        assertThat(countEntries()).isZero();
        assertThat(requestCount()).isEqualTo(1);
    }

    @Test
    void netBankingIsRoutedToItsOwnGateway() {
        Account wallet = newWallet("Dev");

        AddMoneyReceipt receipt = service.addMoney(command(wallet, 25_000, PaymentMethod.NET_BANKING, "HDFC"));

        assertThat(receipt.status()).isEqualTo(AddMoneyStatus.SUCCESS);
        assertThat(balanceOf(wallet)).isEqualTo(25_000);
    }

    @Test
    void amountOutsideTheLimitsIsRejectedBeforeAnythingIsSaved() {
        Account wallet = newWallet("Esha");

        assertThatThrownBy(() -> service.addMoney(command(wallet, 50, PaymentMethod.UPI, "esha@okbank")))
                .isInstanceOf(InvalidAddMoneyAmountException.class);
        assertThatThrownBy(() -> service.addMoney(command(wallet, 10_000_001, PaymentMethod.UPI, "esha@okbank")))
                .isInstanceOf(InvalidAddMoneyAmountException.class);

        assertThat(requestCount()).isZero();
    }

    @Test
    void someoneElsesWalletIsNotFoundAndNothingIsSaved() {
        Account mine = newWallet("Gita");
        Account other = newWallet("Hari");
        AddMoneyCommand command = new AddMoneyCommand(
                mine.getUserId(), other.getAccountId(), 50_000, PaymentMethod.UPI, "gita@okbank");

        assertThatThrownBy(() -> service.addMoney(command))
                .isInstanceOf(AccountNotFoundException.class);

        assertThat(requestCount()).isZero();
        assertThat(balanceOf(other)).isZero();
    }

    @Test
    void refusesToRunInsideATransaction() {
        Account wallet = newWallet("Isha");
        TransactionTemplate tx = new TransactionTemplate(txManager);

        assertThatThrownBy(() -> tx.execute(status ->
                service.addMoney(command(wallet, 50_000, PaymentMethod.UPI, "isha@okbank"))))
                .isInstanceOf(IllegalStateException.class);

        assertThat(requestCount()).isZero();
    }

}
