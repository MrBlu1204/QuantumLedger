package com.quantumledger.app.common;

import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LockConflictRetryTest {
    private final List<Long> sleeps = new ArrayList<>();
    private final LockConflictRetry retry = new LockConflictRetry(3, 0, sleeps::add);

    private static RuntimeException lockConflict() {
        return new PessimisticLockingFailureException("lock wait timeout");
    }

    @Test
    void succeedsFirstTimeWithoutSleeping() {
        AtomicInteger calls = new AtomicInteger();

        String result = retry.execute("test", () -> {
            calls.incrementAndGet();
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void retriesLockConflictsThenSucceeds() {
        AtomicInteger calls = new AtomicInteger();

        String result = retry.execute("test", () -> {
            if (calls.incrementAndGet() < 3) {
                throw lockConflict();
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(3);
        assertThat(sleeps).hasSize(2);
    }

    @Test
    void retriesWhenADeadlockIsWrappedAsTheCause() {
        AtomicInteger calls = new AtomicInteger();

        String result = retry.execute("test", () -> {
            if (calls.incrementAndGet() == 1) {
                throw new RuntimeException("wrapped", new SQLException("deadlock", "40001", 1213));
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void doesNotRetryOtherFailures() {
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> retry.execute("test", () -> {
            calls.incrementAndGet();
            throw new IllegalArgumentException("bad input");
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(calls.get()).isEqualTo(1);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void givesUpAfterMaxAttempts() {
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> retry.execute("test", () -> {
            calls.incrementAndGet();
            throw lockConflict();
        }))
                .isInstanceOf(RetriesExhaustedException.class)
                .satisfies(e -> assertThat(((RetriesExhaustedException) e).getLastFailure())
                        .isInstanceOf(PessimisticLockingFailureException.class));

        assertThat(calls.get()).isEqualTo(3);
        assertThat(sleeps).hasSize(2);
    }
}
