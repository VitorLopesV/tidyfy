package com.tidyfy.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.HelperId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes de integração do histórico: a execução grava o registro em JSON no disco e uma pilha nova,
 * montada sobre a mesma pasta, o lê de volta.
 */
class ExecutionHistoryIntegrationTest {

    @TempDir
    Path tempDir;

    private Path storage;

    private IntegrationStack stack;

    private Path folder;

    @BeforeEach
    void setUp() {
        storage = tempDir.resolve("storage");
        stack = new IntegrationStack(storage);
        folder = tempDir.resolve("Pasta");
    }

    private List<ExecutionRecord> readFromDisk(HelperId id) {
        return new IntegrationStack(storage).listHistory().execute(id);
    }

    @Test
    void recordsMovedAndDuplicateActionsOfTheOrganizer() {
        IntegrationStack.write(folder.resolve("Documentos").resolve("a.pdf"), "igual");
        Path duplicate = IntegrationStack.write(folder.resolve("a.pdf"), "igual");
        Path photo = IntegrationStack.write(folder.resolve("foto.png"), "foto");
        FolderOrganizerHelper helper = stack.saveOrganizer(folder);

        stack.run(helper);

        List<ExecutionRecord> history = readFromDisk(helper.id());
        assertEquals(1, history.size());
        ExecutionRecord record = history.get(0);
        assertEquals("Organizar", record.helperName());
        assertEquals(IntegrationStack.NOW, record.executedAt());
        assertEquals(List.of(
                new HelperAction(ActionType.DUPLICATE_TRASHED, duplicate, null, null),
                new HelperAction(ActionType.MOVED, photo, folder.resolve("Imagens").resolve("foto.png"), null)),
                record.actions());
    }

    @Test
    void recordsTrashedFilesOfTheFolderCleaner() {
        Path file = IntegrationStack.write(folder.resolve("a.txt"), "a");
        CleanerHelper helper = stack.saveFolderCleaner(folder, AgeReference.MODIFIED, null);

        stack.run(helper);

        assertEquals(List.of(new HelperAction(ActionType.TRASHED, file, null, null)),
                readFromDisk(helper.id()).get(0).actions());
    }

    @Test
    void recordsTheRecycleBinEmptiedAction() {
        stack.recycleBin().addItem(IntegrationStack.daysAgo(3));
        CleanerHelper helper = stack.saveRecycleBinCleaner(null);

        stack.run(helper);

        List<HelperAction> actions = readFromDisk(helper.id()).get(0).actions();
        assertEquals(1, actions.size());
        assertEquals(ActionType.RECYCLE_BIN_EMPTIED, actions.get(0).type());
        assertEquals("1 itens removidos", actions.get(0).message());
    }

    @Test
    void recordsFailedWhenTheTrashRejectsAnItemAndKeepsProcessingTheOthers() {
        Path blocked = IntegrationStack.write(folder.resolve("preso.txt"), "x");
        Path free = IntegrationStack.write(folder.resolve("livre.txt"), "y");
        stack.failTrashOf(blocked);
        CleanerHelper helper = stack.saveFolderCleaner(folder, AgeReference.MODIFIED, null);

        stack.run(helper);

        List<HelperAction> actions = readFromDisk(helper.id()).get(0).actions();
        assertEquals(2, actions.size());
        assertEquals(new HelperAction(ActionType.TRASHED, free, null, null), actions.get(0));
        assertEquals(ActionType.FAILED, actions.get(1).type());
        assertEquals(blocked, actions.get(1).source());
        assertTrue(actions.get(1).message().contains("código de saída 1"));
        assertTrue(Files.exists(blocked));
    }

    @Test
    void recordsFailedWhenTheWholeExecutionFails() {
        CleanerHelper helper = stack.saveFolderCleaner(folder, AgeReference.MODIFIED, null);

        stack.run(helper);

        List<HelperAction> actions = readFromDisk(helper.id()).get(0).actions();
        assertEquals(1, actions.size());
        assertEquals(ActionType.FAILED, actions.get(0).type());
    }

    @Test
    void keepsOneRecordPerRunAcrossRuns() {
        IntegrationStack.write(folder.resolve("a.txt"), "a");
        CleanerHelper helper = stack.saveFolderCleaner(folder, AgeReference.MODIFIED, null);

        stack.run(helper);
        stack.run(helper);

        List<ExecutionRecord> history = readFromDisk(helper.id());
        assertEquals(2, history.size());
        assertEquals(List.of(0, 1), history.stream().map(record -> record.actions().size()).sorted().toList());
    }

    @Test
    void doesNotRunADisabledHelperNorRecordHistory() {
        Path file = IntegrationStack.write(folder.resolve("a.txt"), "a");
        CleanerHelper helper = stack.saveCleaner(new FolderTarget(folder, AgeReference.MODIFIED), null, false);

        Optional<ExecutionRecord> record = stack.run(helper);

        assertTrue(record.isEmpty());
        assertTrue(Files.exists(file));
        assertTrue(stack.trashed().isEmpty());
        assertTrue(readFromDisk(helper.id()).isEmpty());
    }
}
