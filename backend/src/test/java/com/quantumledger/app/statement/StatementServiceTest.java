package com.quantumledger.app.statement;

import com.quantumledger.app.statement.StatementRepository.StatementRow;
import com.quantumledger.app.account.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    private static final long USER = 7L;
    private static final long WALLET = 70L;

    @Mock
    StatementRepository statementRepository;
    @Mock
    AccountService accountService;

    StatementService service;

    @BeforeEach
    void setUp() {
        service = new StatementService(statementRepository, accountService);
    }

    private static StatementRepository.StatementRow row(long entryId) {
        return new StatementRow(entryId, new StatementEntry("ref" + entryId, "TRANSFER",
                StatementDirection.IN, 100, "INR", 1_000, null, Instant.EPOCH));
    }

    private void walletExists() {
        when(accountService.getWalletId(USER)).thenReturn(WALLET);
    }

    @Test
    void usesDefaultLimitAndStartsFromTheNewestEntry() {
        walletExists();
        when(statementRepository.findPage(USER, WALLET, Long.MAX_VALUE, 21)).thenReturn(List.of());

        StatementPage page = service.getStatement(USER, null, null);

        assertTrue(page.items().isEmpty());
        assertFalse(page.hasMore());
        assertNull(page.nextCursor());
    }

    @Test
    void rejectsLimitZero() {
        InvalidStatementRequestException e = assertThrows(InvalidStatementRequestException.class,
                () -> service.getStatement(USER, 0, null));

        assertEquals("VALIDATION_FAILED", e.getCode());
        verify(statementRepository, never()).findPage(anyLong(), anyLong(), anyLong(), anyInt());
    }

    @Test
    void rejectsLimitAboveTheMaximum() {
        InvalidStatementRequestException e = assertThrows(InvalidStatementRequestException.class,
                () -> service.getStatement(USER, StatementService.MAX_PAGE_SIZE + 1, null));

        assertEquals("VALIDATION_FAILED", e.getCode());
    }

    @Test
    void rejectsACorruptCursor() {
        InvalidStatementRequestException e = assertThrows(InvalidStatementRequestException.class,
                () -> service.getStatement(USER, 20, "not*base64"));

        assertEquals("INVALID_CURSOR", e.getCode());
        verify(statementRepository, never()).findPage(anyLong(), anyLong(), anyLong(), anyInt());
    }

    @Test
    void reportsAnotherPageAndHandsBackACursorForTheLastItemShown() {
        walletExists();
        when(statementRepository.findPage(USER, WALLET, Long.MAX_VALUE, 3))
                .thenReturn(List.of(row(30), row(20), row(10)));

        StatementPage page = service.getStatement(USER, 2, null);

        assertEquals(2, page.items().size());
        assertTrue(page.hasMore());
        assertEquals(StatementCursor.encode(20), page.nextCursor());
    }

    @Test
    void theLastPageHasNoCursor() {
        walletExists();
        when(statementRepository.findPage(USER, WALLET, Long.MAX_VALUE, 3))
                .thenReturn(List.of(row(20), row(10)));

        StatementPage page = service.getStatement(USER, 2, null);

        assertEquals(2, page.items().size());
        assertFalse(page.hasMore());
        assertNull(page.nextCursor());
    }

    @Test
    void passesTheDecodedCursorToTheQuery() {
        walletExists();
        when(statementRepository.findPage(USER, WALLET, 50, 21)).thenReturn(List.of());

        service.getStatement(USER, null, StatementCursor.encode(50));

        verify(statementRepository).findPage(USER, WALLET, 50, 21);
    }

}
