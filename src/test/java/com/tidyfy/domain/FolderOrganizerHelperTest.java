package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link FolderOrganizerHelper}.
 */
class FolderOrganizerHelperTest {

    private static final Schedule SCHEDULE = new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    private static FolderOrganizerHelper helper(Path folder) {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName("Downloads"), SCHEDULE, true, folder);
    }

    @Test
    void typeIsFolderOrganizer() {
        assertEquals(HelperType.FOLDER_ORGANIZER, helper(Path.of("downloads")).type());
    }

    @Test
    void actsOnASingleFolder() {
        Path folder = Path.of("downloads");
        FolderOrganizerHelper helper = helper(folder);

        assertEquals(folder, helper.folder());
        assertTrue(helper.enabled());
    }

    @Test
    void isAHelper() {
        Helper helper = helper(Path.of("downloads"));

        assertEquals(new HelperName("Downloads"), helper.name());
        assertEquals(SCHEDULE, helper.schedule());
    }

    @Test
    void rejectsNullFolder() {
        assertThrows(NullPointerException.class, () -> helper(null));
    }

    @Test
    void rejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new FolderOrganizerHelper(null, new HelperName("A"), SCHEDULE, true, Path.of("a")));
    }

    @Test
    void rejectsNullName() {
        assertThrows(NullPointerException.class,
                () -> new FolderOrganizerHelper(HelperId.newId(), null, SCHEDULE, true, Path.of("a")));
    }

    @Test
    void rejectsNullSchedule() {
        assertThrows(NullPointerException.class,
                () -> new FolderOrganizerHelper(HelperId.newId(), new HelperName("A"), null, true, Path.of("a")));
    }
}
