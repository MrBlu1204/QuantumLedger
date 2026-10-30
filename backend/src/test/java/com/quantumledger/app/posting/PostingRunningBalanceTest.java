package com.quantumledger.app.posting;

import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.account.Account;
import com.quantumledger.app.transfer.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostingRunningBalanceTest extends AbstractIntegrationTest {

    @Autowired
    TransferService transferService;

    private String phoneOf(Account wallet){
        return jdbc.queryForObject("SELECT phone FROM users WHERE user_id = ?",
                String.class, wallet.getUserId());
    }

    private void send(Account from, Account to, long amount){
        transferService.transfer(new TransferService.TransferCommand(from.getUserId(), "+91", phoneOf(to), amount));
    }

    private List<Long> balancesAfter(Account account){
        return jdbc.queryForList("""
                SELECT balance_after_minor FROM ledger_entries
                WHERE fk_account_id = ? ORDER BY ledger_entry_id
                """, Long.class, account.getAccountId());
    }

    @Test
    void everyEntryStoresTheAccountBalanceRightAfterIt(){
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");
        fund(asha, 100_000);

        send(asha,ravi,25_000);         //asha 75,000   ravi 25,000
        send(ravi,asha,5_000);          //asha 80,000   ravi 20,000

        List<Long> asha_series = balancesAfter(asha);
        List<Long> ravi_series = balancesAfter(ravi);

        assertEquals(List.of(75_000L, 80_000L), asha_series.subList(asha_series.size() -2, asha_series.size()));
        assertEquals(List.of(25_000L, 20_000L), ravi_series);
    }

    @Test
    void theLastStoredBalanceAlwaysEqualsTheCachedAccountBalance(){
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");

        fund(asha, 50_000);

        send(asha,ravi, 10_000);
        send(asha,ravi,7_500);
        send(ravi,asha,2_500);

        for(Account account : List.of(asha,ravi)){
            List<Long> series = balancesAfter(account);
            assertEquals(balanceOf(account),series.get(series.size()-1));
        }

    }
}
