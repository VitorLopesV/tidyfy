package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeExecutionHistoryRepository;
import com.tidyfy.application.usecase.ListExecutionHistoryUseCase;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperId;
import com.tidyfy.presentation.model.ExecutionItem;
import com.tidyfy.presentation.model.ExecutionItemMapper;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testa a conversão do id do ajudante e do histórico em itens de tela.
 */
class HistoryLoaderTest {

    private static final Instant BASE = Instant.parse("2026-03-05T12:00:00Z");

    private final FakeExecutionHistoryRepository repository = new FakeExecutionHistoryRepository();

    private final HistoryLoader loader = new HistoryLoader(new ListExecutionHistoryUseCase(repository),
            new ExecutionItemMapper(ZoneOffset.UTC));

    @Test
    void convertsTheTextIdAndReturnsOnlyThatHelperRecordsNewestFirst() {
        HelperId id = HelperId.newId();
        repository.add(record(id, 0));
        repository.add(record(id, 2));
        repository.add(record(HelperId.newId(), 5));

        List<ExecutionItem> items = loader.apply(id.value().toString());

        assertEquals(List.of("05/03/2026 14:00", "05/03/2026 12:00"),
                items.stream().map(ExecutionItem::executedAt).toList());
    }

    @Test
    void returnsAnEmptyListWhenTheHelperNeverRan() {
        assertTrue(loader.apply(HelperId.newId().value().toString()).isEmpty());
    }

    @Test
    void rejectsAnIdThatIsNotAUuid() {
        assertThrows(IllegalArgumentException.class, () -> loader.apply("h1"));
    }

    @Test
    void rejectsANullId() {
        assertThrows(NullPointerException.class, () -> loader.apply(null));
    }

    private static ExecutionRecord record(HelperId id, long hoursAfterBase) {
        return new ExecutionRecord(id, "Ajudante", BASE.plusSeconds(hoursAfterBase * 3600), List.of());
    }
}
