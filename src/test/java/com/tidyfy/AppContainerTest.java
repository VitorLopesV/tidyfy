package com.tidyfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.ProtectedFolders;
import com.tidyfy.domain.Schedule;
import com.tidyfy.infrastructure.persistence.StorageLocation;
import com.tidyfy.infrastructure.windows.CommandResult;
import com.tidyfy.infrastructure.windows.CommandRunner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes de {@link AppContainer}, com pasta temporária e executor de processos falso
 * (nenhum processo real é executado e {@code create()} não é chamado).
 */
class AppContainerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);

    private static final ProtectedFolders PROTECTED = ProtectedFolders.forWindows(
            Path.of("C:\\Windows"), Path.of("C:\\Program Files"), Path.of("C:\\Program Files (x86)"),
            Path.of("C:\\ProgramData"), Path.of("C:\\Users\\nome"));

    @TempDir
    Path tempDir;

    private final List<List<String>> commands = new ArrayList<>();

    private AppContainer container;

    private Path storageFolder;

    @BeforeEach
    void setUp() {
        storageFolder = tempDir.resolve("Tidyfy");
        CommandRunner runner = command -> {
            commands.add(command);
            return new CommandResult(0, "");
        };
        container = new AppContainer(new StorageLocation(storageFolder), Path.of("Tidyfy.exe"), CLOCK, PROTECTED,
                runner);
    }

    @Test
    void exposesAllUseCases() {
        assertNotNull(container.saveHelper());
        assertNotNull(container.removeHelper());
        assertNotNull(container.listHelpers());
        assertNotNull(container.listExecutionHistory());
        assertNotNull(container.runHelper());
        assertNotNull(container.syncScheduler());
    }

    @Test
    void returnsTheSameInstanceOnRepeatedCalls() {
        assertSame(container.saveHelper(), container.saveHelper());
        assertSame(container.removeHelper(), container.removeHelper());
        assertSame(container.listHelpers(), container.listHelpers());
        assertSame(container.listExecutionHistory(), container.listExecutionHistory());
        assertSame(container.runHelper(), container.runHelper());
        assertSame(container.syncScheduler(), container.syncScheduler());
    }

    @Test
    void listsNoHelpersWhenTheFileDoesNotExist() {
        assertTrue(container.listHelpers().execute().isEmpty());
    }

    @Test
    void createsTheTasksFolderForTheScheduler() {
        assertTrue(Files.isDirectory(storageFolder.resolve("tasks")));
    }

    @Test
    void savesADisabledHelperInTheStorageFolderUsingOnlyTheFakeRunner() {
        FolderOrganizerHelper helper = new FolderOrganizerHelper(HelperId.newId(), new HelperName("Organizar"),
                new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1)), false,
                tempDir.resolve("Downloads"));

        container.saveHelper().execute(helper);

        assertTrue(Files.exists(storageFolder.resolve("helpers.txt")));
        assertEquals(List.of(helper), container.listHelpers().execute());
        assertEquals("schtasks", commands.get(0).get(0));
    }

    @Test
    void listsNoHistoryForAnUnknownHelper() {
        assertTrue(container.listExecutionHistory().execute(HelperId.newId()).isEmpty());
    }

    @Test
    void resolvesTheLauncherOfTheCurrentProcess() {
        assertNotNull(AppContainer.currentLauncher());
    }
}
