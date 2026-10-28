package com.tidyfy.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.application.fake.FakeTaskScheduler;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.DesktopArrangerAlreadyExistsException;
import com.tidyfy.domain.DesktopArrangerHelper;
import com.tidyfy.domain.DesktopLayout;
import com.tidyfy.domain.FolderAlreadyInUseException;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperLimitReachedException;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.HelperNameAlreadyUsedException;
import com.tidyfy.domain.HelperRules;
import com.tidyfy.domain.HelperType;
import com.tidyfy.domain.ProtectedFolderException;
import com.tidyfy.domain.ProtectedFolders;
import com.tidyfy.domain.RecycleBinTarget;
import com.tidyfy.domain.Schedule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link SaveHelperUseCase}, com os fakes das portas.
 */
class SaveHelperUseCaseTest {

    private static final Schedule SCHEDULE = new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    private static final ProtectedFolders PROTECTED = ProtectedFolders.forWindows(
            Path.of("C:\\Windows"), Path.of("C:\\Program Files"), Path.of("C:\\Program Files (x86)"),
            Path.of("C:\\ProgramData"), Path.of("C:\\Users\\nome"));

    private final FakeHelperRepository repository = new FakeHelperRepository();

    private final FakeTaskScheduler scheduler = new FakeTaskScheduler();

    private final SaveHelperUseCase useCase = new SaveHelperUseCase(repository, scheduler, new HelperRules(PROTECTED));

    private static Helper organizer(HelperId id, String name, String folder, boolean enabled) {
        return new FolderOrganizerHelper(id, new HelperName(name), SCHEDULE, enabled, Path.of(folder));
    }

    private static Helper organizer(String name, String folder) {
        return organizer(HelperId.newId(), name, folder, true);
    }

    private static Helper recycleBinCleaner(String name) {
        return new CleanerHelper(HelperId.newId(), new HelperName(name), SCHEDULE, true,
                new RecycleBinTarget(), null);
    }

    private void fillOrganizers(int count) {
        for (int i = 0; i < count; i++) {
            repository.with(organizer("Existente " + i, "C:\\Users\\nome\\pasta" + i));
        }
    }

    @Test
    void createsNewHelperRegisteringAndSaving() {
        Helper helper = organizer("Downloads", "C:\\Users\\nome\\Downloads");

        useCase.execute(helper);

        assertEquals(List.of(helper), repository.findAll());
        assertTrue(scheduler.isRegistered(helper.id()));
        assertEquals(1, repository.saveCount());
    }

    @Test
    void rejectsCreationWhenTypeIsAtLimit() {
        fillOrganizers(HelperType.MAX_PER_TYPE);

        assertThrows(HelperLimitReachedException.class,
                () -> useCase.execute(organizer("Novo", "C:\\Users\\nome\\novo")));
        assertEquals(0, repository.saveCount());
        assertTrue(scheduler.registerCalls().isEmpty());
    }

    @Test
    void doesNotCheckLimitWhenEditing() {
        fillOrganizers(HelperType.MAX_PER_TYPE - 1);
        HelperId id = HelperId.newId();
        repository.with(organizer(id, "Editado", "C:\\Users\\nome\\editado", true));

        Helper edited = organizer(id, "Editado 2", "C:\\Users\\nome\\editado", true);
        useCase.execute(edited);

        assertEquals(edited, repository.findById(id).orElseThrow());
        assertEquals(HelperType.MAX_PER_TYPE, repository.countByType(HelperType.FOLDER_ORGANIZER));
    }

    @Test
    void allowsOtherTypeWhenOneTypeIsAtLimit() {
        fillOrganizers(HelperType.MAX_PER_TYPE);

        useCase.execute(recycleBinCleaner("Lixeira"));

        assertEquals(1, repository.countByType(HelperType.CLEANER));
    }

    private static Helper desktopArranger(HelperId id, String name, DesktopLayout layout) {
        return new DesktopArrangerHelper(id, new HelperName(name), SCHEDULE, true, layout);
    }

    @Test
    void createsTheFirstDesktopArranger() {
        Helper helper = desktopArranger(HelperId.newId(), "Área de trabalho", DesktopLayout.LEFT);

        useCase.execute(helper);

        assertEquals(1, repository.countByType(HelperType.DESKTOP_ARRANGER));
        assertTrue(scheduler.isRegistered(helper.id()));
    }

    @Test
    void rejectsASecondDesktopArranger() {
        repository.with(desktopArranger(HelperId.newId(), "Primeiro", DesktopLayout.LEFT));

        assertThrows(DesktopArrangerAlreadyExistsException.class,
                () -> useCase.execute(desktopArranger(HelperId.newId(), "Segundo", DesktopLayout.TOP)));
        assertEquals(0, repository.saveCount());
        assertTrue(scheduler.registerCalls().isEmpty());
    }

    @Test
    void allowsEditingTheExistingDesktopArranger() {
        HelperId id = HelperId.newId();
        repository.with(desktopArranger(id, "Área", DesktopLayout.LEFT));

        Helper edited = desktopArranger(id, "Área", DesktopLayout.BORDERS);
        useCase.execute(edited);

        assertEquals(edited, repository.findById(id).orElseThrow());
        assertEquals(1, repository.countByType(HelperType.DESKTOP_ARRANGER));
    }

    @Test
    void rejectsRepeatedNameIgnoringCase() {
        repository.with(organizer("Organizar Downloads", "C:\\Users\\nome\\a"));

        assertThrows(HelperNameAlreadyUsedException.class,
                () -> useCase.execute(organizer("organizar downloads", "C:\\Users\\nome\\b")));
        assertEquals(0, repository.saveCount());
    }

    @Test
    void rejectsFolderAlreadyUsedBySameType() {
        repository.with(organizer("A", "C:\\Users\\nome\\Downloads"));

        assertThrows(FolderAlreadyInUseException.class,
                () -> useCase.execute(organizer("B", "C:\\Users\\nome\\Downloads\\Imagens")));
        assertEquals(0, repository.saveCount());
    }

    @Test
    void rejectsSecondRecycleBinCleaner() {
        repository.with(recycleBinCleaner("A"));

        assertThrows(FolderAlreadyInUseException.class, () -> useCase.execute(recycleBinCleaner("B")));
    }

    @Test
    void allowsCleanerOnFolderOfOrganizer() {
        repository.with(organizer("A", "C:\\Users\\nome\\Downloads"));
        Helper cleaner = new CleanerHelper(HelperId.newId(), new HelperName("B"), SCHEDULE, true,
                new FolderTarget(Path.of("C:\\Users\\nome\\Downloads"), AgeReference.CREATED), null);

        useCase.execute(cleaner);

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void rejectsProtectedFolder() {
        assertThrows(ProtectedFolderException.class,
                () -> useCase.execute(organizer("Sistema", "C:\\Windows\\System32")));
        assertEquals(0, repository.saveCount());
        assertTrue(scheduler.registerCalls().isEmpty());
    }

    @Test
    void registersEnabledHelperInScheduler() {
        Helper helper = organizer(HelperId.newId(), "Ligado", "C:\\Users\\nome\\a", true);

        useCase.execute(helper);

        assertSame(helper, scheduler.registeredHelper(helper.id()));
        assertTrue(scheduler.unregisterCalls().isEmpty());
    }

    @Test
    void unregistersDisabledHelperAndSavesIt() {
        HelperId id = HelperId.newId();
        useCase.execute(organizer(id, "A", "C:\\Users\\nome\\a", true));
        Helper disabled = organizer(id, "A", "C:\\Users\\nome\\a", false);

        useCase.execute(disabled);

        assertFalse(scheduler.isRegistered(id));
        assertEquals(List.of(id), scheduler.unregisterCalls());
        assertEquals(disabled, repository.findById(id).orElseThrow());
    }

    @Test
    void doesNotSaveAndPropagatesWhenSchedulerFails() {
        UncheckedIOException failure = new UncheckedIOException(new IOException("schtasks falhou"));
        scheduler.failNextRegisterWith(failure);
        Helper helper = organizer("A", "C:\\Users\\nome\\a");

        UncheckedIOException thrown = assertThrows(UncheckedIOException.class, () -> useCase.execute(helper));

        assertSame(failure, thrown);
        assertTrue(repository.findAll().isEmpty());
        assertEquals(0, repository.saveCount());
    }

    @Test
    void keepsPreviousConfigurationWhenSchedulerFailsOnEdit() {
        HelperId id = HelperId.newId();
        Helper original = organizer(id, "A", "C:\\Users\\nome\\a", true);
        repository.with(original);
        scheduler.failNextRegisterWith(new UncheckedIOException(new IOException("falha")));

        assertThrows(UncheckedIOException.class,
                () -> useCase.execute(organizer(id, "A", "C:\\Users\\nome\\b", true)));

        assertEquals(original, repository.findById(id).orElseThrow());
    }

    @Test
    void editingDoesNotCountItselfInNameOrFolderRules() {
        HelperId id = HelperId.newId();
        repository.with(organizer(id, "A", "C:\\Users\\nome\\Downloads", true));
        Helper edited = organizer(id, "A", "C:\\Users\\nome\\Downloads\\Imagens", true);

        useCase.execute(edited);

        assertEquals(edited, repository.findById(id).orElseThrow());
    }
}
