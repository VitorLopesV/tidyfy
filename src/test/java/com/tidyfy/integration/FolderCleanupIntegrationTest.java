package com.tidyfy.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.AgeLimit;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperAction;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes de integração da limpeza de pasta: rodam de verdade no disco (pasta temporária), com a lixeira
 * simulada por um executor de comandos dublê. A data de criação é alterada com {@code setTimes}, que
 * funciona no NTFS do Windows (o teste confere essa pré-condição).
 */
class FolderCleanupIntegrationTest {

    @TempDir
    Path tempDir;

    private IntegrationStack stack;

    private Path folder;

    @BeforeEach
    void setUp() {
        stack = new IntegrationStack(tempDir.resolve("storage"));
        folder = tempDir.resolve("Temporarios");
    }

    @Test
    void sendsEveryLooseFileToTheTrashWithoutAgeLimitAndKeepsSubfolders() {
        Path first = IntegrationStack.write(folder.resolve("a.txt"), "a");
        Path second = IntegrationStack.write(folder.resolve("b.log"), "b");
        Path inside = IntegrationStack.write(folder.resolve("Sub").resolve("c.txt"), "c");
        CleanerHelper helper = stack.saveFolderCleaner(folder, AgeReference.MODIFIED, null);

        ExecutionRecord record = stack.run(helper).orElseThrow();

        assertFalse(Files.exists(first));
        assertFalse(Files.exists(second));
        assertTrue(Files.exists(inside));
        assertEquals(List.of(first, second), stack.trashed());
        assertEquals(List.of(ActionType.TRASHED, ActionType.TRASHED),
                record.actions().stream().map(HelperAction::type).toList());
    }

    @Test
    void cleansOnlyFilesOlderThanTheLimitByModificationDate() {
        Path old = IntegrationStack.write(folder.resolve("velho.txt"), "velho");
        Path recent = IntegrationStack.write(folder.resolve("recente.txt"), "recente");
        Path edge = IntegrationStack.write(folder.resolve("limite.txt"), "limite");
        IntegrationStack.setTimes(old, IntegrationStack.daysAgo(10), IntegrationStack.daysAgo(1));
        IntegrationStack.setTimes(recent, IntegrationStack.daysAgo(2), IntegrationStack.daysAgo(30));
        IntegrationStack.setTimes(edge, IntegrationStack.daysAgo(7), IntegrationStack.daysAgo(30));
        CleanerHelper helper = stack.saveFolderCleaner(folder, AgeReference.MODIFIED, new AgeLimit(7));

        stack.run(helper);

        assertFalse(Files.exists(old));
        assertTrue(Files.exists(recent));
        assertTrue(Files.exists(edge));
        assertEquals(List.of(old), stack.trashed());
    }

    @Test
    void cleansOnlyFilesOlderThanTheLimitByCreationDate() throws IOException {
        Path oldCreated = IntegrationStack.write(folder.resolve("criado-antes.txt"), "x");
        Path recentCreated = IntegrationStack.write(folder.resolve("criado-agora.txt"), "y");
        IntegrationStack.setTimes(oldCreated, IntegrationStack.daysAgo(1), IntegrationStack.daysAgo(10));
        IntegrationStack.setTimes(recentCreated, IntegrationStack.daysAgo(10), IntegrationStack.daysAgo(1));
        BasicFileAttributes attributes = Files.readAttributes(oldCreated, BasicFileAttributes.class);
        assertEquals(IntegrationStack.daysAgo(10).getEpochSecond(),
                attributes.creationTime().toInstant().getEpochSecond());
        CleanerHelper helper = stack.saveFolderCleaner(folder, AgeReference.CREATED, new AgeLimit(7));

        stack.run(helper);

        assertFalse(Files.exists(oldCreated));
        assertTrue(Files.exists(recentCreated));
    }

    @Test
    void leavesEverythingWhenNoFileIsOldEnough() {
        Path file = IntegrationStack.write(folder.resolve("a.txt"), "a");
        IntegrationStack.setTimes(file, IntegrationStack.daysAgo(1), IntegrationStack.daysAgo(1));
        CleanerHelper helper = stack.saveFolderCleaner(folder, AgeReference.MODIFIED, new AgeLimit(7));

        ExecutionRecord record = stack.run(helper).orElseThrow();

        assertTrue(Files.exists(file));
        assertTrue(record.actions().isEmpty());
    }
}
