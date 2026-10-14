package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.application.fake.FakeTaskScheduler;
import com.tidyfy.application.usecase.SaveHelperUseCase;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.HelperRules;
import com.tidyfy.domain.HelperType;
import com.tidyfy.domain.ProtectedFolders;
import com.tidyfy.domain.RecycleBinTarget;
import com.tidyfy.domain.Schedule;
import com.tidyfy.presentation.component.FolderField;
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
 * Testa se cada erro de regra do domínio aparece no campo certo dos formulários, com o
 * {@link SaveHelperUseCase} real sobre os fakes do repositório e do agendador.
 */
class HelperFormRuleErrorsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZoneOffset.UTC);

    private static final Schedule SCHEDULE = new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 5));

    private static final ProtectedFolders PROTECTED = ProtectedFolders.forWindows(
            Path.of("C:\\Windows"), Path.of("C:\\Program Files"), Path.of("C:\\Program Files (x86)"),
            Path.of("C:\\ProgramData"), Path.of("C:\\Users\\nome"));

    @TempDir
    Path tempDir;

    private final FakeHelperRepository repository = new FakeHelperRepository();

    private final SaveHelperUseCase saveHelper =
            new SaveHelperUseCase(repository, new FakeTaskScheduler(), new HelperRules(PROTECTED));

    /** Formulário de organização que conta quantas vezes foi fechado. */
    private static final class RecordingOrganizerForm extends FolderOrganizerFormController {
        int closed;

        RecordingOrganizerForm(SaveHelperUseCase saveHelper) {
            super(null, saveHelper, CLOCK);
        }

        @Override
        protected void onClose() {
            closed++;
        }
    }

    /** Formulário de limpeza que conta quantas vezes foi fechado. */
    private static final class RecordingCleanerForm extends CleanerFormController {
        int closed;

        RecordingCleanerForm(SaveHelperUseCase saveHelper) {
            super(null, saveHelper, CLOCK);
        }

        @Override
        protected void onClose() {
            closed++;
        }
    }

    @Test
    void repeatedNameShowsTheErrorInTheNameFieldOnly() {
        repository.with(organizer("Organizar", tempDir.resolve("A")));
        JavaFx.run(() -> {
            RecordingOrganizerForm form = new RecordingOrganizerForm(saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("ORGANIZAR");
            folder(view).setFolder(tempDir.resolve("B"));

            save(view).fire();

            assertEquals(0, form.closed);
            assertFalse(form.saved());
            assertTrue(name(view).getStyleClass().contains("invalid"));
            Label help = label(view, "nameHelp");
            assertTrue(help.getStyleClass().contains("error"));
            assertTrue(help.getText().contains("Já existe um ajudante"));
            assertFalse(label(view, "folderError").isVisible());
            assertFalse(label(view, "saveError").isVisible());
            assertEquals("ORGANIZAR", name(view).getText());
        });
    }

    @Test
    void editingTheNameClearsTheNameError() {
        repository.with(organizer("Organizar", tempDir.resolve("A")));
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(new RecordingOrganizerForm(saveHelper).createView());
            name(view).setText("Organizar");
            folder(view).setFolder(tempDir.resolve("B"));
            save(view).fire();
            assertTrue(name(view).getStyleClass().contains("invalid"));

            name(view).setText("Organizar 2");

            assertFalse(name(view).getStyleClass().contains("invalid"));
            assertFalse(label(view, "nameHelp").getStyleClass().contains("error"));
            assertFalse(label(view, "nameHelp").getText().contains("Já existe"));
        });
    }

    @Test
    void fixingTheNameAndSavingAgainClosesTheForm() {
        repository.with(organizer("Organizar", tempDir.resolve("A")));
        JavaFx.run(() -> {
            RecordingOrganizerForm form = new RecordingOrganizerForm(saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Organizar");
            folder(view).setFolder(tempDir.resolve("B"));
            save(view).fire();

            name(view).setText("Outro nome");
            save(view).fire();

            assertTrue(form.saved());
            assertEquals(1, form.closed);
            assertEquals(2, repository.findAll().size());
        });
    }

    @Test
    void folderUsedByAnotherOrganizerShowsTheErrorBelowTheFolderField() {
        repository.with(organizer("Organizar", tempDir.resolve("A")));
        JavaFx.run(() -> {
            RecordingOrganizerForm form = new RecordingOrganizerForm(saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Outro");
            folder(view).setFolder(tempDir.resolve("A"));

            save(view).fire();

            assertEquals(0, form.closed);
            assertFolderError(view, "já está em uso");
            assertFalse(name(view).getStyleClass().contains("invalid"));
            assertFalse(label(view, "saveError").isVisible());
            assertEquals(tempDir.resolve("A"), folder(view).getFolder());
        });
    }

    @Test
    void nestedFolderShowsTheErrorBelowTheFolderField() {
        repository.with(organizer("Organizar", tempDir.resolve("A")));
        JavaFx.run(() -> {
            RecordingOrganizerForm form = new RecordingOrganizerForm(saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Outro");
            folder(view).setFolder(tempDir.resolve("A").resolve("Sub"));

            save(view).fire();

            assertEquals(0, form.closed);
            assertFolderError(view, "aninhada");
        });
    }

    @Test
    void protectedFolderShowsTheErrorBelowTheFolderField() {
        JavaFx.run(() -> {
            RecordingOrganizerForm form = new RecordingOrganizerForm(saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Sistema");
            folder(view).setFolder(Path.of("C:\\Windows\\System32"));

            save(view).fire();

            assertEquals(0, form.closed);
            assertFolderError(view, "protegida");
            assertTrue(repository.findAll().isEmpty());
        });
    }

    @Test
    void cleanerFolderInUseShowsTheErrorBelowTheFolderField() {
        repository.with(cleanerOn("Limpar", tempDir.resolve("T")));
        JavaFx.run(() -> {
            RecordingCleanerForm form = new RecordingCleanerForm(saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Limpar 2");
            folder(view).setFolder(tempDir.resolve("T"));

            save(view).fire();

            assertEquals(0, form.closed);
            assertFolderError(view, "já está em uso");
        });
    }

    @Test
    void secondRecycleBinCleanerShowsTheErrorInTheTargetErrorLabel() {
        repository.with(new CleanerHelper(HelperId.newId(), new HelperName("Lixeira"), SCHEDULE, true,
                new RecycleBinTarget(), null));
        JavaFx.run(() -> {
            RecordingCleanerForm form = new RecordingCleanerForm(saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Lixeira 2");
            ((ToggleButton) view.lookup("#binChoice")).fire();

            save(view).fire();

            assertEquals(0, form.closed);
            Label error = label(view, "folderError");
            assertTrue(error.isVisible());
            assertTrue(error.isManaged());
            assertTrue(error.getStyleClass().contains("error"));
            assertTrue(error.getText().contains("lixeira"));
            assertFalse(label(view, "saveError").isVisible());
        });
    }

    @Test
    void switchingTheTargetClearsTheRecycleBinError() {
        repository.with(new CleanerHelper(HelperId.newId(), new HelperName("Lixeira"), SCHEDULE, true,
                new RecycleBinTarget(), null));
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(new RecordingCleanerForm(saveHelper).createView());
            name(view).setText("Lixeira 2");
            ((ToggleButton) view.lookup("#binChoice")).fire();
            save(view).fire();
            assertTrue(label(view, "folderError").isVisible());

            ((ToggleButton) view.lookup("#folderChoice")).fire();

            assertFalse(label(view, "folderError").isVisible());
        });
    }

    @Test
    void choosingAnotherFolderClearsTheFolderErrorAndSavingThenClosesTheForm() {
        repository.with(organizer("Organizar", tempDir.resolve("A")));
        JavaFx.run(() -> {
            RecordingOrganizerForm form = new RecordingOrganizerForm(saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Outro");
            folder(view).setFolder(tempDir.resolve("A"));
            save(view).fire();
            assertTrue(label(view, "folderError").isVisible());

            folder(view).setFolder(tempDir.resolve("B"));

            assertFalse(label(view, "folderError").isVisible());
            assertFalse(folder(view).getStyleClass().contains("invalid"));
            save(view).fire();
            assertTrue(form.saved());
            assertEquals(1, form.closed);
        });
    }

    @Test
    void limitReachedShowsTheErrorInTheFooter() {
        for (int i = 0; i < HelperType.MAX_PER_TYPE; i++) {
            repository.with(organizer("Organizar " + i, tempDir.resolve("P" + i)));
        }
        JavaFx.run(() -> {
            RecordingOrganizerForm form = new RecordingOrganizerForm(saveHelper);
            Parent view = JavaFx.mount(form.createView());
            name(view).setText("Mais um");
            folder(view).setFolder(tempDir.resolve("Novo"));

            save(view).fire();

            assertEquals(0, form.closed);
            Label error = label(view, "saveError");
            assertTrue(error.isVisible());
            assertTrue(error.getText().contains("limite"));
            assertFalse(label(view, "folderError").isVisible());
            assertFalse(name(view).getStyleClass().contains("invalid"));
            assertEquals(HelperType.MAX_PER_TYPE, repository.findAll().size());
        });
    }

    @Test
    void folderErrorStartsHidden() {
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(new RecordingOrganizerForm(saveHelper).createView());

            assertFalse(label(view, "folderError").isVisible());
            assertFalse(label(view, "folderError").isManaged());
        });
    }

    private static void assertFolderError(Parent view, String text) {
        Label error = label(view, "folderError");
        assertTrue(error.isVisible());
        assertTrue(error.getStyleClass().contains("error"));
        assertTrue(error.getText().contains(text), error.getText());
        assertTrue(folder(view).getStyleClass().contains("invalid"));
    }

    private static FolderOrganizerHelper organizer(String name, Path folder) {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName(name), SCHEDULE, true, folder);
    }

    private static CleanerHelper cleanerOn(String name, Path folder) {
        return new CleanerHelper(HelperId.newId(), new HelperName(name), SCHEDULE, true,
                new FolderTarget(folder, AgeReference.MODIFIED), null);
    }

    private static Label label(Parent view, String id) {
        return (Label) view.lookup("#" + id);
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
