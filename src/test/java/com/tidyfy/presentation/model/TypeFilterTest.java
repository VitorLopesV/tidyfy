package com.tidyfy.presentation.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testa o filtro da lista por tipo.
 */
class TypeFilterTest {

    private final HelperItem organizer = item(HelperKind.ORGANIZER);
    private final HelperItem cleaner = item(HelperKind.CLEANER);

    @Test
    void allMatchesEveryType() {
        assertTrue(TypeFilter.ALL.matches(organizer));
        assertTrue(TypeFilter.ALL.matches(cleaner));
    }

    @Test
    void organizerMatchesOnlyFolderOrganizers() {
        assertTrue(TypeFilter.ORGANIZER.matches(organizer));
        assertFalse(TypeFilter.ORGANIZER.matches(cleaner));
    }

    @Test
    void cleanerMatchesOnlyCleaners() {
        assertTrue(TypeFilter.CLEANER.matches(cleaner));
        assertFalse(TypeFilter.CLEANER.matches(organizer));
    }

    private static HelperItem item(HelperKind kind) {
        return new HelperItem(kind.name(), "Ajudante", kind, Path.of("C:\\Pasta"), 1,
                List.of(LocalTime.NOON), true, null, 0);
    }
}
