package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.application.fake.FakeTaskScheduler;
import com.tidyfy.application.usecase.SaveHelperUseCase;
import com.tidyfy.domain.AgeLimit;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.HelperRules;
import com.tidyfy.domain.ProtectedFolders;
import com.tidyfy.domain.RecycleBinTarget;
import com.tidyfy.domain.Schedule;
import com.tidyfy.presentation.component.FolderField;
import com.tidyfy.presentation.component.ScheduleEditor;
import com.tidyfy.presentation.component.ToggleSwitch;
import com.tidyfy.presentation.model.HelperItem;
import com.tidyfy.presentation.model.HelperItemMapper;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testa o salvamento pelos formulários de organização e de limpeza, com o {@link SaveHelperUseCase} real
 * sobre os fakes do repositório e do agendador.
 */
class HelperFormSavingTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZoneOffset.UTC);

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

    private static final LocalDate OLD_START = LocalDate.of(2026, 1, 5);

    private static final ProtectedFolders PROTECTED = ProtectedFolders.forWindows(
            Path.of("C:\\Windows"), Path.of("C:\\Program Files"), Path.of("C:\\Program Files (x86)"),
            Path.of("C:\\ProgramData"), Path.of("C:\\Users\\nome"));

    @TempDir
    Path tempDir;

    private final FakeHelperRepository repository = new FakeHelperRepository();

    private final FakeTaskScheduler scheduler = new FakeTaskScheduler();

    private final SaveHelperUseCase saveHelper =
            new SaveHelperUseCase(repository, scheduler, new HelperRules(PROTECTED));

    /** Formulário de organização que conta quantas vezes foi fechado. */
    private static final class RecordingOrganizerForm extends FolderOrganizerFormController {
        int closed;

        RecordingOrganizerForm(HelperItem editing, SaveHelperUseCase saveHelper) {
            super(editing, saveHelper, CLOCK);
        }

        @Override
        protected void onClose() {
            closed++;
        }
    }

    /** Formulário de limpeza que conta quantas vezes foi fechado. */
    private static final class RecordingCleanerForm extends CleanerFormController {
        int closed;

        RecordingCleanerForm(HelperItem editing, SaveHelperUseCase saveHelper) {
            super(editing, saveHelper, CLOCK);
        }

        @Override
        protected void onClose() {
            closed++;
        }
    }

    @Test
    void creatingAnOrganizerSavesItAndClosesTheForm() {
        JavaFx.run(() -> {
            RecordingOrganizerForm form = new RecordingOrganizerForm(null, saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("  Organizar Downloads ");
            folder(view).setFolder(tempDir.resolve("Downloads"));
            ((ScheduleEditor) view.lookup("#scheduleEditor")).setIntervalDays(2);

            save(view).fire();

            assertTrue(form.saved());
            assertEquals(1, form.closed);
            FolderOrganizerHelper saved = assertInstanceOf(FolderOrganizerHelper.class, repository.findAll().get(0));
            assertEquals("Organizar Downloads", saved.name().value());
            assertEquals(tempDir.resolve("Downloads"), saved.folder());
            assertEquals(new Schedule(2, List.of(LocalTime.of(9, 0)), TODAY), saved.schedule());
            assertTrue(saved.enabled());
            assertEquals(saved, scheduler.registeredHelper(saved.id()));
        });
    }

    @Test
    void creatingAFolderCleanerUsesTheFolderAndDefaultAge() {
        JavaFx.run(() -> {
            RecordingCleanerForm form = new RecordingCleanerForm(null, saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Limpar temporários");
            folder(view).setFolder(tempDir.resolve("Temp"));

            save(view).fire();

            assertTrue(form.saved());
            CleanerHelper saved = assertInstanceOf(CleanerHelper.class, repository.findAll().get(0));
            assertEquals(new FolderTarget(tempDir.resolve("Temp"), AgeReference.MODIFIED), saved.target());
            assertNull(saved.ageLimit());
            assertEquals(TODAY, saved.schedule().startDate());
            assertTrue(scheduler.isRegistered(saved.id()));
        });
    }

    @Test
    void creatingARecycleBinCleanerUsesTheRecycleBin() {
        JavaFx.run(() -> {
            RecordingCleanerForm form = new RecordingCleanerForm(null, saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Esvaziar lixeira");
            ((ToggleButton) view.lookup("#binChoice")).fire();

            save(view).fire();

            assertTrue(form.saved());
            CleanerHelper saved = assertInstanceOf(CleanerHelper.class, repository.findAll().get(0));
            assertEquals(new RecycleBinTarget(), saved.target());
        });
    }

    @Test
    void editingKeepsTheIdAndTheStartDateWhenTheFrequencyDoesNotChange() {
        Helper original = organizer("Organizar", tempDir.resolve("A"), 1, true);
        repository.with(original);
        JavaFx.run(() -> {
            RecordingOrganizerForm form = new RecordingOrganizerForm(item(original), saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Organizar tudo");

            save(view).fire();

            assertTrue(form.saved());
            assertEquals(1, repository.findAll().size());
            Helper saved = repository.findAll().get(0);
            assertEquals(original.id(), saved.id());
            assertEquals("Organizar tudo", saved.name().value());
            assertEquals(OLD_START, saved.schedule().startDate());
        });
    }

    @Test
    void editingUsesTodayWhenTheIntervalChanges() {
        Helper original = organizer("Organizar", tempDir.resolve("A"), 1, true);
        repository.with(original);
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(new RecordingOrganizerForm(item(original), saveHelper).createView());
            ((ScheduleEditor) view.lookup("#scheduleEditor")).setIntervalDays(3);

            save(view).fire();

            assertEquals(new Schedule(3, List.of(LocalTime.of(9, 0)), TODAY), repository.findAll().get(0).schedule());
        });
    }

    @Test
    void editingUsesTodayWhenTheTimesChange() {
        Helper original = organizer("Organizar", tempDir.resolve("A"), 1, true);
        repository.with(original);
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(new RecordingOrganizerForm(item(original), saveHelper).createView());
            ((ScheduleEditor) view.lookup("#scheduleEditor")).setTimes(List.of(LocalTime.of(9, 0), LocalTime.of(18, 0)));

            save(view).fire();

            assertEquals(TODAY, repository.findAll().get(0).schedule().startDate());
        });
    }

    @Test
    void editingACleanerKeepsTheAgeLimitAndReference() {
        Helper original = new CleanerHelper(HelperId.newId(), new HelperName("Limpar"),
                new Schedule(7, List.of(LocalTime.of(20, 0)), OLD_START), true,
                new FolderTarget(tempDir.resolve("Temp"), AgeReference.CREATED), new AgeLimit(30));
        repository.with(original);
        JavaFx.run(() -> {
            RecordingCleanerForm form = new RecordingCleanerForm(item(original), saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Limpar mais");

            save(view).fire();

            assertTrue(form.saved());
            CleanerHelper saved = (CleanerHelper) repository.findAll().get(0);
            assertEquals(original.id(), saved.id());
            assertEquals(new AgeLimit(30), saved.ageLimit());
            assertEquals(new FolderTarget(tempDir.resolve("Temp"), AgeReference.CREATED), saved.target());
            assertEquals(OLD_START, saved.schedule().startDate());
        });
    }

    @Test
    void turningAHelperOffUnregistersItFromTheScheduler() {
        Helper original = organizer("Organizar", tempDir.resolve("A"), 1, true);
        repository.with(original);
        scheduler.register(original);
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(new RecordingOrganizerForm(item(original), saveHelper).createView());
            ((ToggleSwitch) view.lookup("#enabledSwitch")).setSelected(false);

            save(view).fire();

            assertFalse(repository.findAll().get(0).enabled());
            assertFalse(scheduler.isRegistered(original.id()));
        });
    }

    @Test
    void schedulerFailureKeepsTheFormOpenShowsTheMessageAndSavesNothing() {
        scheduler.failNextRegisterWith(new UncheckedIOException(new IOException("schtasks falhou")));
        JavaFx.run(() -> {
            RecordingOrganizerForm form = new RecordingOrganizerForm(null, saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Organizar");
            folder(view).setFolder(tempDir.resolve("A"));

            save(view).fire();

            assertFalse(form.saved());
            assertEquals(0, form.closed);
            Label error = (Label) view.lookup("#saveError");
            assertTrue(error.isVisible());
            assertTrue(error.getText().contains("schtasks falhou"));
            assertTrue(repository.findAll().isEmpty());
            assertEquals("Organizar", name(view).getText());
            assertEquals(tempDir.resolve("A"), folder(view).getFolder());
        });
    }

    @Test
    void repeatedNameShowsTheDomainMessageAndKeepsTheFormOpen() {
        repository.with(organizer("Organizar", tempDir.resolve("A"), 1, true));
        JavaFx.run(() -> {
            RecordingCleanerForm form = new RecordingCleanerForm(null, saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("organizar");
            ((ToggleButton) view.lookup("#binChoice")).fire();

            save(view).fire();

            assertFalse(form.saved());
            assertEquals(0, form.closed);
            Label error = (Label) view.lookup("#saveError");
            assertTrue(error.isVisible());
            assertFalse(error.getText().isBlank());
            assertEquals(1, repository.findAll().size());
        });
    }

    @Test
    void successfulRetryHidesThePreviousError() {
        scheduler.failNextRegisterWith(new IllegalStateException("falha"));
        JavaFx.run(() -> {
            RecordingOrganizerForm form = new RecordingOrganizerForm(null, saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Organizar");
            folder(view).setFolder(tempDir.resolve("A"));
            save(view).fire();
            assertTrue(view.lookup("#saveError").isVisible());

            save(view).fire();

            assertFalse(view.lookup("#saveError").isVisible());
            assertTrue(form.saved());
        });
    }

    @Test
    void formWithoutServicesDoesNotSaveNorClose() {
        JavaFx.run(() -> {
            FolderOrganizerFormController form = new FolderOrganizerFormController(null) {
                @Override
                protected void onClose() {
                    throw new AssertionError("não deveria fechar");
                }
            };
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Organizar");
            folder(view).setFolder(tempDir.resolve("A"));

            save(view).fire();

            assertFalse(form.saved());
            assertTrue(view.lookup("#saveError").isVisible());
        });
    }

    @Test
    void saveErrorStartsHidden() {
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(new RecordingOrganizerForm(null, saveHelper).createView());

            assertFalse(view.lookup("#saveError").isVisible());
        });
    }

    private Helper organizer(String name, Path folder, int interval, boolean enabled) {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName(name),
                new Schedule(interval, List.of(LocalTime.of(9, 0)), OLD_START), enabled, folder);
    }

    private static HelperItem item(Helper helper) {
        return new HelperItemMapper(CLOCK).toItem(helper);
    }

    private static Button save(Parent view) {
        return (Button) view.lookup("#saveButton");
    }

    private static TextField name(Parent view) {
        return (TextField) view.lookup("#nameField");
    }

    private static FolderField folder(Parent view) {
        return (FolderField) view.lookup("#folderField");
    }
}
