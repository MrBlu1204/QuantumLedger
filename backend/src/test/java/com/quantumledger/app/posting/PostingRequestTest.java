package com.quantumledger.app.posting;

import com.quantumledger.app.ledger.InvalidPostingException;
import com.quantumledger.app.ledger.LedgerTransactionType;
import com.quantumledger.app.ledger.PostingLine;
import com.quantumledger.app.ledger.PostingRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PostingRequestTest {

    private static final long BANK = 1L;
    private static final long FEE = 2L;
    private static final long ASHA = 30L;
    private static final long RAVI = 40L;


    private PostingRequest request(List<PostingLine> lines) {
        return new PostingRequest(LedgerTransactionType.TRANSFER, "test", "INR", lines);
    }

    @Test
    void acceptsTopUp(){
        PostingRequest r = new PostingRequest(LedgerTransactionType.TOP_UP, "add 1000", "INR",
                List.of(PostingLine.debit(BANK, 100_000),
                        PostingLine.credit(ASHA, 100_000)));

        assertEquals(100_000, r.totalAmountMinor());
    }

    @Test
    void acceptsTransferWithFee(){
        PostingRequest r = request(List.of(
                PostingLine.debit(ASHA, 25_500),
                PostingLine.credit(RAVI, 25_000),
                PostingLine.credit(FEE, 500)));

        assertEquals(25_500, r.totalAmountMinor());
    }

    @Test
    void rejectsUnbalancedPosting(){
        InvalidPostingException e = assertThrows(InvalidPostingException.class,
                () -> request(List.of(
                        PostingLine.debit(ASHA, 25_500),
                        PostingLine.credit(RAVI,25_000))));
        assertEquals("unbalanced posting: debits=25500 credits=25000", e.getMessage());
    }

    @Test
    void rejectsSingleLine(){
        assertThrows(InvalidPostingException.class,
                () -> request(List.of(PostingLine.debit(ASHA, 100))));
    }

    @Test
    void rejectsSameAccountTwice(){
        assertThrows(InvalidPostingException.class,
                () -> request(List.of(PostingLine.debit(ASHA, 100),
                        PostingLine.debit(ASHA, 100)))
        );
    }

    @Test
    void rejectsZeroAndNegativeAmounts(){
        assertThrows(InvalidPostingException.class,
                () -> PostingLine.debit(ASHA, 0)
        );
        assertThrows(InvalidPostingException.class,
                () -> PostingLine.debit(ASHA, -5));

    }

    @Test
    void rejectsBadCurrency(){
        assertThrows(InvalidPostingException.class,
                () -> new PostingRequest(
                        LedgerTransactionType.TRANSFER, "x", "inr", List.of(
                                PostingLine.debit(ASHA, 100),
                        PostingLine.credit(RAVI, 100)
                )
                )
        );
    }

    @Test
    void rejectsOverflowThatWouldLookBalanced(){
        // Two huge debits wrap around to a small negative number with plain addition.
        // Math.addExact throws instead, so this is rejected.
        // Without addExact: debits wrap to -2 and credits wrap to -2, so they look equal.
        assertThrows(InvalidPostingException.class, () -> request(List.of(
                PostingLine.debit(ASHA, Long.MAX_VALUE),
                PostingLine.debit(RAVI, Long.MAX_VALUE),
                PostingLine.credit(BANK, Long.MAX_VALUE),
                PostingLine.credit(FEE, Long.MAX_VALUE))));
    }

    @Test
    void returnsAccountIdsInAscendingOrder(){
        PostingRequest r = request(List.of(
                PostingLine.debit(RAVI, 100),
                PostingLine.credit(BANK, 50),
                PostingLine.credit(ASHA, 50)
        ));

        assertEquals(List.of(BANK, ASHA, RAVI), r.accountIdsInLockOrder());
    }

    @Test
    void callerCannotChangeLinesAfterConstruction(){
        List<PostingLine> lines = new ArrayList<>(List.of(
                PostingLine.debit(ASHA, 100), PostingLine.credit(RAVI, 100)));
        PostingRequest r = request(lines);

        lines.add(PostingLine.credit(BANK, 999));          // tamper with the original list

        assertEquals(2, r.lines().size());                  // the request is unaffected
        assertThrows(UnsupportedOperationException.class,
                () -> r.lines().add(PostingLine.credit(BANK, 1)));
    }
}
