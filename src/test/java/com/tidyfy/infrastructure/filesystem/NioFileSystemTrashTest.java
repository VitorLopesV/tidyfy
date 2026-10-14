package com.tidyfy.infrastructure.filesystem;

import com.tidyfy.infrastructure.windows.CommandResult;
import com.tidyfy.infrastructure.windows.CommandRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de {@link NioFileSystem#moveToTrash}. Usa um executor de comandos falso: o PowerShell nunca roda
 * e a Lixeira real não é tocada.
 */
class NioFileSystemTrashTest {

    @TempDir
    Path temp;

    private final List<List<String>> executed = new ArrayList<>();
    private CommandResult nextResult = new CommandResult(0, "");

    private final CommandRunner runner = command -> {
        executed.add(command);
        return nextResult;
    };

    private final NioFileSystem fileSystem = new NioFileSystem(runner);

    @Test
    void sendsAFileWithTheFileCommand() throws IOException {
        Path file = Files.writeString(temp.resolve("a.txt"), "x");

        assertDoesNotThrow(() -> fileSystem.moveToTrash(file));

        assertEquals(List.of(TrashCommands.sendFileToTrash(file)), executed);
    }

    @Test
    void sendsAFolderWithTheFolderCommand() throws IOException {
        Path folder = Files.createDirectory(temp.resolve("Velhas"));

        fileSystem.moveToTrash(folder);

        assertEquals(List.of(TrashCommands.sendDirectoryToTrash(folder)), executed);
    }

    @Test
    void failsWithoutRunningTheCommandWhenThePathDoesNotExist() {
        Path missing = temp.resolve("nao-existe.txt");

        assertThrows(UncheckedIOException.class, () -> fileSystem.moveToTrash(missing));

        assertTrue(executed.isEmpty());
    }

    @Test
    void failsWithPathExitCodeAndOutputWhenTheCommandFails() throws IOException {
        Path file = Files.writeString(temp.resolve("a.txt"), "x");
        nextResult = new CommandResult(1, "Could not find file\r\n");

        UncheckedIOException error = assertThrows(UncheckedIOException.class, () -> fileSystem.moveToTrash(file));

        String message = error.getMessage();
        assertTrue(message.contains(file.toString()));
        assertTrue(message.contains("1"));
        assertTrue(message.contains("Could not find file"));
        assertTrue(message.contains("260"));
    }
}
