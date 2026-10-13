package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link CleanupTarget}, {@link RecycleBinTarget} e {@link FolderTarget}.
 */
class CleanupTargetTest {

    @Test
    void folderTargetRejectsNullFolder() {
        assertThrows(NullPointerException.class, () -> new FolderTarget(null, AgeReference.CREATED));
    }

    @Test
    void folderTargetRejectsNullAgeReference() {
        assertThrows(NullPointerException.class, () -> new FolderTarget(Path.of("pasta"), null));
    }

    @Test
    void folderTargetKeepsFolderAndAgeReference() {
        FolderTarget target = new FolderTarget(Path.of("pasta"), AgeReference.MODIFIED);

        assertEquals(Path.of("pasta"), target.folder());
        assertEquals(AgeReference.MODIFIED, target.ageReference());
    }

    @Test
    void recycleBinTargetsAreEqual() {
        assertEquals(new RecycleBinTarget(), new RecycleBinTarget());
    }

    @Test
    void cleanupTargetIsSealedAndOnlyPermitsRecycleBinAndFolder() {
        assertTrue(CleanupTarget.class.isSealed());
        assertEquals(Set.of(RecycleBinTarget.class, FolderTarget.class),
                Set.of(CleanupTarget.class.getPermittedSubclasses()));
    }
}
