package com.tidyfy.application.fake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link FakeExecutionHistoryRepository}: garantem que ele segue o contrato da porta.
 */
class FakeExecutionHistoryRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    private static ExecutionRecord record(HelperId id, Instant executedAt) {
        return new ExecutionRecord(id, "Ajudante", executedAt, List.of());
    }

    @Test
    void findByHelperReturnsMostRecentFirstAndOnlyThatHelper() {
        FakeExecutionHistoryRepository history = new FakeExecutionHistoryRepository();
        HelperId id = HelperId.newId();
        ExecutionRecord old = record(id, NOW.minus(2, ChronoUnit.DAYS));
        ExecutionRecord recent = record(id, NOW.minus(1, ChronoUnit.DAYS));
        history.add(old);
        history.add(recent);
        history.add(record(HelperId.newId(), NOW));

        assertEquals(List.of(recent, old), history.findByHelper(id));
    }

    @Test
    void findByHelperIsEmptyForUnknownId() {
        assertTrue(new FakeExecutionHistoryRepository().findByHelper(HelperId.newId()).isEmpty());
    }

    @Test
    void deleteExpiredRemovesOnlyRecordsOlderThanRetention() {
        FakeExecutionHistoryRepository history = new FakeExecutionHistoryRepository();
        HelperId id = HelperId.newId();
        ExecutionRecord expired = record(id, NOW.minus(ExecutionRecord.RETENTION_DAYS + 1, ChronoUnit.DAYS));
        ExecutionRecord kept = record(id, NOW.minus(ExecutionRecord.RETENTION_DAYS - 1, ChronoUnit.DAYS));
        history.add(expired);
        history.add(kept);

        history.deleteExpired(NOW);

        assertEquals(List.of(kept), history.all());
    }

    @Test
    void deleteExpiredWithoutExpiredRecordsChangesNothing() {
        FakeExecutionHistoryRepository history = new FakeExecutionHistoryRepository();
        history.add(record(HelperId.newId(), NOW));

        history.deleteExpired(NOW);

        assertEquals(1, history.all().size());
    }

    @Test
    void failNextAddWithThrowsOnceAndStoresNothing() {
        FakeExecutionHistoryRepository history = new FakeExecutionHistoryRepository();
        history.failNextAddWith(new IllegalStateException("falha"));
        ExecutionRecord record = record(HelperId.newId(), NOW);

        assertThrows(IllegalStateException.class, () -> history.add(record));
        assertTrue(history.all().isEmpty());

        history.add(record);
        assertEquals(List.of(record), history.all());
    }
}
