package com.tidyfy.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.AgeLimit;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.ExecutionRecord;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes de integração da limpeza da lixeira, com a lixeira do Windows em memória
 * ({@code FakeRecycleBin}) e a configuração e o histórico em JSON no disco.
 */
class RecycleBinCleanupIntegrationTest {

    @TempDir
    Path tempDir;

    private IntegrationStack stack;

    @BeforeEach
    void setUp() {
        stack = new IntegrationStack(tempDir.resolve("storage"));
        stack.recycleBin()
                .addItem(IntegrationStack.daysAgo(40))
                .addItem(IntegrationStack.daysAgo(10))
                .addItem(IntegrationStack.daysAgo(1));
    }

    @Test
    void emptiesEverythingWithoutAgeLimit() {
        CleanerHelper helper = stack.saveRecycleBinCleaner(null);

        ExecutionRecord record = stack.run(helper).orElseThrow();

        assertEquals(0, stack.recycleBin().size());
        assertEquals(1, record.actions().size());
        assertEquals(ActionType.RECYCLE_BIN_EMPTIED, record.actions().get(0).type());
        assertEquals("3 itens removidos", record.actions().get(0).message());
    }

    @Test
    void removesOnlyItemsOlderThanTheLimit() {
        CleanerHelper helper = stack.saveRecycleBinCleaner(new AgeLimit(7));

        ExecutionRecord record = stack.run(helper).orElseThrow();

        assertEquals(1, stack.recycleBin().size());
        assertEquals(2, stack.recycleBin().removedCount());
        assertEquals(List.of(IntegrationStack.daysAgo(7)), stack.recycleBin().cutoffs());
        assertEquals("2 itens removidos", record.actions().get(0).message());
    }
}
