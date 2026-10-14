package com.tidyfy.infrastructure.filesystem;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes do comando do PowerShell que envia itens à lixeira. Só valida o texto montado; nada é executado
 * e a lixeira real não é tocada.
 */
class TrashCommandsTest {

    @Test
    void buildsTheExactCommandForAFile() {
        List<String> command = TrashCommands.sendFileToTrash(Path.of("C:\\Users\\ana\\Downloads\\a.txt"));

        assertEquals(List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
                "-ExecutionPolicy", "Bypass", "-Command",
                "Add-Type -AssemblyName Microsoft.VisualBasic; "
                        + "[Microsoft.VisualBasic.FileIO.FileSystem]::DeleteFile("
                        + "'C:\\Users\\ana\\Downloads\\a.txt', 'OnlyErrorDialogs', 'SendToRecycleBin')"),
                command);
    }

    @Test
    void buildsTheExactCommandForADirectory() {
        List<String> command = TrashCommands.sendDirectoryToTrash(Path.of("C:\\Users\\ana\\Downloads\\Velhas"));

        assertEquals("Add-Type -AssemblyName Microsoft.VisualBasic; "
                        + "[Microsoft.VisualBasic.FileIO.FileSystem]::DeleteDirectory("
                        + "'C:\\Users\\ana\\Downloads\\Velhas', 'OnlyErrorDialogs', 'SendToRecycleBin')",
                command.get(command.size() - 1));
    }

    @Test
    void doublesSingleQuotesInThePath() {
        List<String> command = TrashCommands.sendFileToTrash(Path.of("C:\\Users\\ana\\d'Ouro\\it's.txt"));

        assertEquals("Add-Type -AssemblyName Microsoft.VisualBasic; "
                        + "[Microsoft.VisualBasic.FileIO.FileSystem]::DeleteFile("
                        + "'C:\\Users\\ana\\d''Ouro\\it''s.txt', 'OnlyErrorDialogs', 'SendToRecycleBin')",
                command.get(command.size() - 1));
    }

    @Test
    void keepsAccentsAndSpacesInThePath() {
        List<String> command = TrashCommands.sendDirectoryToTrash(Path.of("C:\\Users\\ana\\Relat\u00f3rios do m\u00eas"));

        assertEquals("Add-Type -AssemblyName Microsoft.VisualBasic; "
                        + "[Microsoft.VisualBasic.FileIO.FileSystem]::DeleteDirectory("
                        + "'C:\\Users\\ana\\Relat\u00f3rios do m\u00eas', 'OnlyErrorDialogs', 'SendToRecycleBin')",
                command.get(command.size() - 1));
    }

    @Test
    void rejectsANullPath() {
        assertThrows(NullPointerException.class, () -> TrashCommands.sendFileToTrash(null));
        assertThrows(NullPointerException.class, () -> TrashCommands.sendDirectoryToTrash(null));
    }
}
