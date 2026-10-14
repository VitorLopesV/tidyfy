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
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.HelperRules;
import com.tidyfy.domain.ProtectedFolders;
import com.tidyfy.domain.RecycleBinTarget;
import com.tidyfy.domain.Schedule;
import com.tidyfy.presentation.component.FolderField;
import com.tidyfy.presentation.model.HelperItem;
import com.tidyfy.presentation.model.HelperItemMapper;
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
 * Testa os campos de limite de idade e de data de referência do formulário de Limpeza.
 */
class CleanerAgeLimitTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZoneOffset.UTC);

    private static final ProtectedFolders PROTECTED = ProtectedFolders.forWindows(
            Path.of("C:\\Windows"), Path.of("C:\\Program Files"), Path.of("C:\\Program Files (x86)"),
            Path.of("C:\\ProgramData"), Path.of("C:\\Users\\nome"));

    @TempDir
    Path tempDir;

    private final FakeHelperRepository repository = new FakeHelperRepository();

    private final SaveHelperUseCase saveHelper =
            new SaveHelperUseCase(repository, new FakeTaskScheduler(), new HelperRules(PROTECTED));

    @Test
    void newCleanerStartsCleaningEverythingAndSavesNoAgeLimit() {
        JavaFx.run(() -> {
            Parent view = form(null);
            name(view).setText("Limpar");
            folder(view).setFolder(tempDir.resolve("Temp"));

            assertTrue(toggle(view, "allChoice").isSelected());
            assertTrue(view.lookup("#ageStepperRow").isDisabled());
            assertFalse(view.lookup("#referenceSection").isVisible());
            save(view).fire();

            assertNull(saved().ageLimit());
        });
    }

    @Test
    void choosingOlderThanSavesTheDefaultAgeLimit() {
        JavaFx.run(() -> {
            Parent view = form(null);
            name(view).setText("Limpar");
            folder(view).setFolder(tempDir.resolve("Temp"));

            toggle(view, "olderChoice").fire();

            assertFalse(view.lookup("#ageStepperRow").isDisabled());
            assertEquals("30", label(view, "ageDaysLabel").getText());
            save(view).fire();
            assertEquals(new AgeLimit(30), saved().ageLimit());
        });
    }

    @Test
    void stepperChangesTheDaysAndStopsAtOne() {
        JavaFx.run(() -> {
            Parent view = form(null);
            name(view).setText("Limpar");
            folder(view).setFolder(tempDir.resolve("Temp"));
            toggle(view, "olderChoice").fire();

            button(view, "increaseAge").fire();
            assertEquals("31", label(view, "ageDaysLabel").getText());
            for (int i = 0; i < 40; i++) {
                if (!button(view, "decreaseAge").isDisabled()) {
                    button(view, "decreaseAge").fire();
                }
            }

            assertEquals("1", label(view, "ageDaysLabel").getText());
            assertTrue(button(view, "decreaseAge").isDisabled());
            assertEquals("dia", label(view, "ageUnitLabel").getText());
            save(view).fire();
            assertEquals(new AgeLimit(1), saved().ageLimit());
        });
    }

    @Test
    void agePreviewDescribesTheLimit() {
        JavaFx.run(() -> {
            Parent view = form(null);

            assertTrue(label(view, "folderAgeSummary").getText().contains("tudo"));
            toggle(view, "olderChoice").fire();

            assertEquals("Remove só o que tem mais de 30 dias.", label(view, "folderAgeSummary").getText());
        });
    }

    @Test
    void referenceShowsOnlyForAFolderWithTheLimitOn() {
        JavaFx.run(() -> {
            Parent view = form(null);
            Parent referenceSection = (Parent) view.lookup("#referenceSection");
            assertFalse(referenceSection.isVisible());

            toggle(view, "olderChoice").fire();
            assertTrue(referenceSection.isVisible());
            assertTrue(toggle(view, "modifiedChoice").isSelected());

            toggle(view, "binChoice").fire();
            assertFalse(referenceSection.isVisible());
            assertTrue(label(view, "ageHelp").isVisible());

            toggle(view, "folderChoice").fire();
            assertTrue(referenceSection.isVisible());
            assertFalse(label(view, "ageHelp").isVisible());

            toggle(view, "allChoice").fire();
            assertFalse(referenceSection.isVisible());
        });
    }

    @Test
    void createdAndModifiedReachTheFolderTarget() {
        JavaFx.run(() -> {
            Parent view = form(null);
            name(view).setText("Limpar");
            folder(view).setFolder(tempDir.resolve("Temp"));
            toggle(view, "olderChoice").fire();
            toggle(view, "createdChoice").fire();
            save(view).fire();
            assertEquals(new FolderTarget(tempDir.resolve("Temp"), AgeReference.CREATED), saved().target());
        });
        repository.delete(saved().id());
        JavaFx.run(() -> {
            Parent view = form(null);
            name(view).setText("Limpar");
            folder(view).setFolder(tempDir.resolve("Temp"));
            toggle(view, "olderChoice").fire();
            toggle(view, "createdChoice").fire();
            toggle(view, "modifiedChoice").fire();
            save(view).fire();
            assertEquals(new FolderTarget(tempDir.resolve("Temp"), AgeReference.MODIFIED), saved().target());
        });
    }

    @Test
    void recycleBinKeepsTheAgeLimit() {
        JavaFx.run(() -> {
            Parent view = form(null);
            name(view).setText("Esvaziar");
            toggle(view, "olderChoice").fire();
            button(view, "increaseAge").fire();
            toggle(view, "binChoice").fire();

            save(view).fire();

            assertEquals(new RecycleBinTarget(), saved().target());
            assertEquals(new AgeLimit(31), saved().ageLimit());
        });
    }

    @Test
    void editingLoadsTheLimitAndTheReferenceAndSavingKeepsThem() {
        Helper original = new CleanerHelper(HelperId.newId(), new HelperName("Limpar"),
                new Schedule(7, List.of(LocalTime.of(20, 0)), LocalDate.of(2026, 1, 5)), true,
                new FolderTarget(tempDir.resolve("Temp"), AgeReference.CREATED), new AgeLimit(12));
        repository.with(original);
        JavaFx.run(() -> {
            Parent view = form(new HelperItemMapper(CLOCK).toItem(original));

            assertTrue(toggle(view, "olderChoice").isSelected());
            assertEquals("12", label(view, "ageDaysLabel").getText());
            assertTrue(toggle(view, "createdChoice").isSelected());
            assertTrue(view.lookup("#referenceSection").isVisible());
            save(view).fire();

            CleanerHelper saved = (CleanerHelper) repository.findAll().get(0);
            assertEquals(new AgeLimit(12), saved.ageLimit());
            assertEquals(new FolderTarget(tempDir.resolve("Temp"), AgeReference.CREATED), saved.target());
        });
    }

    @Test
    void editingWithoutLimitSelectsEverythingAndModifiedByDefault() {
        JavaFx.run(() -> {
            HelperItem item = SampleHelpers.folderCleaner("h2", "Limpar");
            Parent view = form(item);

            assertTrue(toggle(view, "allChoice").isSelected());
            assertTrue(toggle(view, "modifiedChoice").isSelected());
            assertEquals("30", label(view, "ageDaysLabel").getText());
        });
    }

    @Test
    void selectedAmountCannotBeTurnedOff() {
        JavaFx.run(() -> {
            Parent view = form(null);

            toggle(view, "allChoice").fire();

            assertTrue(toggle(view, "allChoice").isSelected());
        });
    }

    private CleanerHelper saved() {
        return assertInstanceOf(CleanerHelper.class, repository.findAll().get(0));
    }

    private Parent form(HelperItem editing) {
        return JavaFx.mount(new CleanerFormController(editing, saveHelper, CLOCK).createView());
    }

    private static ToggleButton toggle(Parent view, String id) {
        return (ToggleButton) view.lookup("#" + id);
    }

    private static Button button(Parent view, String id) {
        return (Button) view.lookup("#" + id);
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
