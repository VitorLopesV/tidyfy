package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link CleanerHelper}.
 */
class CleanerHelperTest {

    private static final Schedule SCHEDULE = new Schedule(7, List.of(LocalTime.of(22, 0)), LocalDate.of(2026, 1, 1));

    private static CleanerHelper helper(CleanupTarget target, AgeLimit ageLimit) {
        return new CleanerHelper(HelperId.newId(), new HelperName("Lixeira"), SCHEDULE, false, target, ageLimit);
    }

    @Test
    void withEnabledChangesOnlyTheEnabledState() {
        CleanerHelper helper = helper(new RecycleBinTarget(), new AgeLimit(30));

        CleanerHelper on = helper.withEnabled(true);

        assertEquals(new CleanerHelper(helper.id(), helper.name(), helper.schedule(), true, helper.target(),
                helper.ageLimit()), on);
        assertEquals(helper, on.withEnabled(false));
    }

    @Test
    void typeIsCleaner() {
        assertEquals(HelperType.CLEANER, helper(new RecycleBinTarget(), null).type());
    }

    @Test
    void acceptsAbsentAgeLimitMeaningCleanEverything() {
        CleanerHelper helper = helper(new RecycleBinTarget(), null);

        assertNull(helper.ageLimit());
        assertEquals(Optional.empty(), helper.findAgeLimit());
    }

    @Test
    void keepsAgeLimitWhenInformed() {
        AgeLimit limit = new AgeLimit(30);
        CleanerHelper helper = helper(new FolderTarget(Path.of("temp"), AgeReference.MODIFIED), limit);

        assertEquals(Optional.of(limit), helper.findAgeLimit());
    }

    @Test
    void actsOnASingleTarget() {
        CleanupTarget target = new FolderTarget(Path.of("temp"), AgeReference.CREATED);
        CleanerHelper helper = helper(target, null);

        assertEquals(target, helper.target());
        assertFalse(helper.enabled());
    }

    @Test
    void rejectsNullTarget() {
        assertThrows(NullPointerException.class, () -> helper(null, null));
    }

    @Test
    void rejectsNullId() {
        assertThrows(NullPointerException.class, () -> new CleanerHelper(null, new HelperName("A"), SCHEDULE,
                true, new RecycleBinTarget(), null));
    }

    @Test
    void rejectsNullName() {
        assertThrows(NullPointerException.class, () -> new CleanerHelper(HelperId.newId(), null, SCHEDULE,
                true, new RecycleBinTarget(), null));
    }

    @Test
    void rejectsNullSchedule() {
        assertThrows(NullPointerException.class, () -> new CleanerHelper(HelperId.newId(), new HelperName("A"),
                null, true, new RecycleBinTarget(), null));
    }
}
