package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link ProtectedFolders} e {@link ProtectedFolderException}.
 */
class ProtectedFoldersTest {

    private final ProtectedFolders folders = ProtectedFolders.forWindows(
            Path.of("C:\\Windows"),
            Path.of("C:\\Program Files"),
            Path.of("C:\\Program Files (x86)"),
            Path.of("C:\\ProgramData"),
            Path.of("C:\\Users\\ana"));

    private void assertRefused(String folder) {
        assertThrows(ProtectedFolderException.class, () -> folders.assertAllowed(Path.of(folder)));
    }

    private void assertAccepted(String folder) {
        assertDoesNotThrow(() -> folders.assertAllowed(Path.of(folder)));
    }

    @Test
    void refusesEachSystemFolderItself() {
        assertRefused("C:\\Windows");
        assertRefused("C:\\Program Files");
        assertRefused("C:\\Program Files (x86)");
        assertRefused("C:\\ProgramData");
    }

    @Test
    void refusesAnyFolderInsideTheSystemFolders() {
        assertRefused("C:\\Windows\\System32");
        assertRefused("C:\\Windows\\System32\\drivers\\etc");
        assertRefused("C:\\Program Files\\App\\bin");
        assertRefused("C:\\Program Files (x86)\\App");
        assertRefused("C:\\ProgramData\\Vendor\\Cache");
    }

    @Test
    void refusesSystemFoldersIgnoringCaseAndDotSegments() {
        assertRefused("c:\\windows\\system32");
        assertRefused("C:\\Users\\ana\\..\\..\\Windows");
    }

    @Test
    void refusesTheRootOfAnyDrive() {
        assertRefused("C:\\");
        assertRefused("D:\\");
    }

    @Test
    void refusesTheUserFolderItself() {
        assertRefused("C:\\Users\\ana");
    }

    @Test
    void acceptsFoldersInsideTheUserFolder() {
        assertAccepted("C:\\Users\\ana\\Downloads");
        assertAccepted("C:\\Users\\ana\\Downloads\\Imagens");
    }

    @Test
    void acceptsOrdinaryFoldersOutsideTheProtectedOnes() {
        assertAccepted("D:\\Fotos");
        assertAccepted("C:\\Dados");
    }

    @Test
    void acceptsFoldersWhoseNameOnlyStartsLikeAProtectedOne() {
        assertAccepted("C:\\WindowsBackup");
        assertAccepted("C:\\Program Files Old");
    }

    @Test
    void exceptionIsADomainExceptionMentioningTheRefusedFolder() {
        ProtectedFolderException e = assertThrows(ProtectedFolderException.class,
                () -> folders.assertAllowed(Path.of("C:\\Windows")));
        assertTrue(e.getMessage().contains("C:\\Windows"));
        assertTrue(e instanceof DomainException);
    }
}
