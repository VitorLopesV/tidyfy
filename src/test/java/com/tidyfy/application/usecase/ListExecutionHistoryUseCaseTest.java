package com.tidyfy.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeExecutionHistoryRepository;
import com.tidyfy.application.port.ExecutionHistoryRepository;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link ListExecutionHistoryUseCase}, com o fake em memória do histórico.
 */
class ListExecutionHistoryUseCaseTest {

    private static final Instant BASE = Instant.parse("2026-03-01T10:00:00Z");

    private FakeExecutionHistoryRepository history;
    private ListExecutionHistoryUseCase useCase;

    @BeforeEach
    void setUp() {
        history = new FakeExecutionHistoryRepository();
        useCase = new ListExecutionHistoryUseCase(history);
    }

    private ExecutionRecord record(HelperId id, long hoursAfterBase) {
        return new ExecutionRecord(id, "Ajudante", BASE.plusSeconds(hoursAfterBase * 3600), List.of());
    }

    @Test
    void returnsOnlyRecordsOfRequestedHelper() {
        HelperId mine = HelperId.newId();
        HelperId other = HelperId.newId();
        history.add(record(mine, 1));
        history.add(record(other, 2));
        history.add(record(mine, 3));

        List<ExecutionRecord> result = useCase.execute(mine);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.helperId().equals(mine)));
    }

    @Test
    void ordersFromNewestToOldestEvenWhenInsertedOutOfOrder() {
        HelperId id = HelperId.newId();
        ExecutionRecord middle = record(id, 2);
        ExecutionRecord oldest = record(id, 1);
        ExecutionRecord newest = record(id, 3);
        history.add(middle);
        history.add(oldest);
        history.add(newest);

        assertEquals(List.of(newest, middle, oldest), useCase.execute(id));
    }

    @Test
    void ordersByInstantEvenWhenRepositoryReturnsOldestFirst() {
        HelperId id = HelperId.newId();
        ExecutionRecord oldest = record(id, 1);
        ExecutionRecord newest = record(id, 2);
        ExecutionHistoryRepository ascending = new ExecutionHistoryRepository() {
            @Override
            public void add(ExecutionRecord record) {
            }

            @Override
            public List<ExecutionRecord> findByHelper(HelperId helperId) {
                return new ArrayList<>(List.of(oldest, newest));
            }

            @Override
            public void deleteExpired(Instant now) {
            }
        };

        assertEquals(List.of(newest, oldest), new ListExecutionHistoryUseCase(ascending).execute(id));
    }

    @Test
    void returnsEmptyListWhenHelperHasNoRecords() {
        history.add(record(HelperId.newId(), 1));

        assertTrue(useCase.execute(HelperId.newId()).isEmpty());
    }

    @Test
    void returnsImmutableList() {
        HelperId id = HelperId.newId();
        history.add(record(id, 1));

        List<ExecutionRecord> result = useCase.execute(id);

        assertThrows(UnsupportedOperationException.class, () -> result.add(record(id, 2)));
    }

    @Test
    void rejectsNullId() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void rejectsNullRepository() {
        assertThrows(NullPointerException.class, () -> new ListExecutionHistoryUseCase(null));
    }
}
