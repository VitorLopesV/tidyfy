package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link ExecutionRecord}, {@link HelperAction} e {@link ActionType}.
 */
class ExecutionRecordTest {

    private static final Instant EXECUTED_AT = Instant.parse("2026-01-01T12:00:00Z");
    private static final HelperAction MOVED =
            new HelperAction(ActionType.MOVED, Path.of("a.jpg"), Path.of("Fotos", "a.jpg"), null);

    private static ExecutionRecord recordWith(List<HelperAction> actions) {
        return new ExecutionRecord(HelperId.newId(), "Fotos", EXECUTED_AT, actions);
    }

    @Test
    void isNotExpiredRightAfterExecution() {
        assertFalse(recordWith(List.of()).isExpired(EXECUTED_AT));
    }

    @Test
    void isNotExpiredWhenExactlyThirtyDaysOld() {
        assertFalse(recordWith(List.of()).isExpired(Instant.parse("2026-01-31T12:00:00Z")));
    }

    @Test
    void isExpiredAfterMoreThanThirtyDays() {
        assertTrue(recordWith(List.of()).isExpired(Instant.parse("2026-01-31T12:00:01Z")));
    }

    @Test
    void retentionIsThirtyDays() {
        assertEquals(30, ExecutionRecord.RETENTION_DAYS);
    }

    @Test
    void actionsListIsImmutable() {
        ExecutionRecord record = recordWith(List.of(MOVED));
        assertThrows(UnsupportedOperationException.class, () -> record.actions().add(MOVED));
    }

    @Test
    void actionsListIsNotAffectedByLaterChangesToTheOriginal() {
        List<HelperAction> original = new ArrayList<>(List.of(MOVED));
        ExecutionRecord record = recordWith(original);
        original.add(MOVED);
        assertEquals(1, record.actions().size());
    }

    @Test
    void keepsHelperDataAndActionsInOrder() {
        HelperId id = HelperId.newId();
        HelperAction failed = new HelperAction(ActionType.FAILED, null, null, "sem permissão");
        ExecutionRecord record = new ExecutionRecord(id, "Fotos", EXECUTED_AT, List.of(MOVED, failed));
        assertEquals(id, record.helperId());
        assertEquals("Fotos", record.helperName());
        assertEquals(EXECUTED_AT, record.executedAt());
        assertEquals(List.of(MOVED, failed), record.actions());
    }

    @Test
    void rejectsNullFields() {
        HelperId id = HelperId.newId();
        assertThrows(NullPointerException.class, () -> new ExecutionRecord(null, "x", EXECUTED_AT, List.of()));
        assertThrows(NullPointerException.class, () -> new ExecutionRecord(id, null, EXECUTED_AT, List.of()));
        assertThrows(NullPointerException.class, () -> new ExecutionRecord(id, "x", null, List.of()));
        assertThrows(NullPointerException.class, () -> new ExecutionRecord(id, "x", EXECUTED_AT, null));
    }

    @Test
    void rejectsNullAction() {
        List<HelperAction> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> recordWith(withNull));
    }

    @Test
    void rejectsNullNowWhenCheckingExpiration() {
        assertThrows(NullPointerException.class, () -> recordWith(List.of()).isExpired(null));
    }

    @Test
    void actionAcceptsOptionalComponentsAsNull() {
        HelperAction emptied = new HelperAction(ActionType.RECYCLE_BIN_EMPTIED, null, null, null);
        assertNull(emptied.source());
        assertNull(emptied.target());
        assertNull(emptied.message());
    }

    @Test
    void actionRejectsNullType() {
        assertThrows(NullPointerException.class, () -> new HelperAction(null, null, null, null));
    }

    @Test
    void actionTypesAreTheSixKnownOnes() {
        assertEquals(
                List.of("MOVED", "DUPLICATE_TRASHED", "TRASHED", "RECYCLE_BIN_EMPTIED", "ARRANGED", "FAILED"),
                Arrays.stream(ActionType.values()).map(Enum::name).toList());
    }
}
