package com.automation.application.financial.comparison;

import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public final class FinancialSnapshotComparisonTest {

    @Test
    public void shouldDetectUnchangedSnapshot() {
        FinancialSnapshot before =
                createSnapshot(
                        "13714323",
                        0,
                        "877716729",
                        "568338116",
                        "9268419047",
                        "1000.00"
                );

        FinancialSnapshot after =
                createSnapshot(
                        "13714323",
                        0,
                        "877716729",
                        "568338116",
                        "9268419047",
                        "1000.00"
                );

        FinancialSnapshotComparison comparison =
                new FinancialSnapshotComparison(
                        before,
                        after
                );

        assertFalse(comparison.statusChanged());
        assertFalse(comparison.balanceChanged());
        assertFalse(comparison.historyChanged());
        assertFalse(comparison.ticketChanged());

        assertEquals(
                comparison.balanceDifference(),
                new BigDecimal("0.00")
        );
    }

    @Test
    public void shouldDetectBalanceReduction() {
        FinancialSnapshot before =
                createSnapshot(
                        "13714323",
                        0,
                        "877716729",
                        "568338116",
                        "9268419047",
                        "1000.00"
                );

        FinancialSnapshot after =
                createSnapshot(
                        "13714323",
                        0,
                        "877716729",
                        "568338116",
                        "9268419047",
                        "800.00"
                );

        FinancialSnapshotComparison comparison =
                new FinancialSnapshotComparison(
                        before,
                        after
                );

        assertTrue(comparison.balanceChanged());

        assertEquals(
                comparison.balanceDifference(),
                new BigDecimal("-200.00")
        );
    }

    @Test
    public void shouldDetectBalanceIncrease() {
        FinancialSnapshot before =
                createSnapshot(
                        "13714323",
                        0,
                        "877716729",
                        "568338116",
                        "9268419047",
                        "800.00"
                );

        FinancialSnapshot after =
                createSnapshot(
                        "13714323",
                        0,
                        "877716729",
                        "568338116",
                        "9268419047",
                        "950.00"
                );

        FinancialSnapshotComparison comparison =
                new FinancialSnapshotComparison(
                        before,
                        after
                );

        assertTrue(comparison.balanceChanged());

        assertEquals(
                comparison.balanceDifference(),
                new BigDecimal("150.00")
        );
    }

    @Test
    public void shouldDetectStatusChange() {
        FinancialSnapshot before =
                createSnapshot(
                        "13714323",
                        0,
                        "877716729",
                        "568338116",
                        "9268419047",
                        "1000.00"
                );

        FinancialSnapshot after =
                createSnapshot(
                        "13714323",
                        10,
                        "877716729",
                        "568338116",
                        "9268419047",
                        "1000.00"
                );

        FinancialSnapshotComparison comparison =
                new FinancialSnapshotComparison(
                        before,
                        after
                );

        assertTrue(comparison.statusChanged());
    }

    @Test
    public void shouldDetectHistoryChange() {
        FinancialSnapshot before =
                createSnapshot(
                        "13714323",
                        0,
                        "877716729",
                        "568338116",
                        "9268419047",
                        "1000.00"
                );

        FinancialSnapshot after =
                createSnapshot(
                        "13714323",
                        0,
                        "877716730",
                        "568338116",
                        "9268419047",
                        "1000.00"
                );

        FinancialSnapshotComparison comparison =
                new FinancialSnapshotComparison(
                        before,
                        after
                );

        assertTrue(comparison.historyChanged());
    }

    @Test
    public void shouldDetectTicketChange() {
        FinancialSnapshot before =
                createSnapshot(
                        "13714323",
                        0,
                        "877716729",
                        "568338116",
                        "9268419047",
                        "1000.00"
                );

        FinancialSnapshot after =
                createSnapshot(
                        "13714323",
                        0,
                        "877716729",
                        "568338117",
                        "9268419050",
                        "1000.00"
                );

        FinancialSnapshotComparison comparison =
                new FinancialSnapshotComparison(
                        before,
                        after
                );

        assertTrue(comparison.ticketChanged());
    }

    @Test
    public void shouldRejectDifferentAccounts() {
        FinancialSnapshot before =
                createSnapshot(
                        "13714323",
                        0,
                        "877716729",
                        "568338116",
                        "9268419047",
                        "1000.00"
                );

        FinancialSnapshot after =
                createSnapshot(
                        "1710122",
                        0,
                        "877716729",
                        "568338116",
                        "9268419047",
                        "1000.00"
                );

        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new FinancialSnapshotComparison(
                        before,
                        after
                )
        );

        assertEquals(
                exception.getMessage(),
                "Financial snapshots must belong to the same account."
        );
    }

    private FinancialSnapshot createSnapshot(
            String accountId,
            int status,
            String historyId,
            String ticketId,
            String nossoNumero,
            String currentBalance
    ) {
        return new FinancialSnapshot(
                new AccountId(accountId),
                status,
                historyId,
                ticketId,
                nossoNumero,
                new BigDecimal(currentBalance),
                new BigDecimal("200.00"),
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 10),
                Instant.parse("2026-08-07T12:00:00Z")
        );
    }
}