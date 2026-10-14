package com.tidyfy.application.fake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.HelperType;
import com.tidyfy.domain.RecycleBinTarget;
import com.tidyfy.domain.Schedule;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link FakeHelperRepository}: garantem que ele segue o contrato da porta.
 */
class FakeHelperRepositoryTest {

    private static final Schedule SCHEDULE =
            new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    private static Helper organizer(String name) {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName(name), SCHEDULE, true, Path.of("C:\\Pasta"));
    }

    private static Helper cleaner(String name) {
        return new CleanerHelper(HelperId.newId(), new HelperName(name), SCHEDULE, true, new RecycleBinTarget(), null);
    }

    @Test
    void saveIncludesNewHelperAndFindByIdReturnsIt() {
        FakeHelperRepository repository = new FakeHelperRepository();
        Helper helper = organizer("A");

        repository.save(helper);

        assertEquals(helper, repository.findById(helper.id()).orElseThrow());
        assertEquals(1, repository.saveCount());
    }

    @Test
    void findByIdIsEmptyWhenMissing() {
        assertTrue(new FakeHelperRepository().findById(HelperId.newId()).isEmpty());
    }

    @Test
    void saveReplacesHelperWithSameIdKeepingPosition() {
        FakeHelperRepository repository = new FakeHelperRepository();
        Helper first = organizer("A");
        Helper second = organizer("B");
        repository.save(first);
        repository.save(second);
        Helper edited = new FolderOrganizerHelper(first.id(), new HelperName("A2"), SCHEDULE, false, Path.of("C:\\Pasta"));

        repository.save(edited);

        assertEquals(List.of(edited, second), repository.findAll());
    }

    @Test
    void findAllKeepsCreationOrderAndIsEmptyAtStart() {
        FakeHelperRepository repository = new FakeHelperRepository();
        assertTrue(repository.findAll().isEmpty());
        Helper a = organizer("A");
        Helper b = cleaner("B");
        repository.save(a);
        repository.save(b);

        assertEquals(List.of(a, b), repository.findAll());
    }

    @Test
    void countByTypeCountsOnlyThatType() {
        FakeHelperRepository repository = new FakeHelperRepository()
                .with(organizer("A")).with(organizer("B")).with(cleaner("C"));

        assertEquals(2, repository.countByType(HelperType.FOLDER_ORGANIZER));
        assertEquals(1, repository.countByType(HelperType.CLEANER));
    }

    @Test
    void countByTypeIsZeroWhenNone() {
        assertEquals(0, new FakeHelperRepository().countByType(HelperType.CLEANER));
    }

    @Test
    void deleteRemovesHelperAndUnknownIdDoesNothing() {
        Helper helper = organizer("A");
        FakeHelperRepository repository = new FakeHelperRepository().with(helper);

        repository.delete(HelperId.newId());
        assertEquals(1, repository.findAll().size());

        repository.delete(helper.id());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void withDoesNotCountAsSave() {
        assertEquals(0, new FakeHelperRepository().with(organizer("A")).saveCount());
    }

    @Test
    void failNextSaveWithThrowsOnceAndStoresNothing() {
        FakeHelperRepository repository = new FakeHelperRepository();
        repository.failNextSaveWith(new IllegalStateException("falha"));
        Helper helper = organizer("A");

        assertThrows(IllegalStateException.class, () -> repository.save(helper));
        assertTrue(repository.findAll().isEmpty());

        repository.save(helper);
        assertEquals(1, repository.findAll().size());
    }
}
