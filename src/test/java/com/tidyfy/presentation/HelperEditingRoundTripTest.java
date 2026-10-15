package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.application.fake.FakeTaskScheduler;
import com.tidyfy.application.usecase.ListHelpersUseCase;
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
import com.tidyfy.presentation.model.HelperKind;
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
 * Testa a edição de ponta a ponta: o ajudante salvo passa pelo caso de uso de listar e pelo mapeador, abre
 * no formulário criado pela lista com todos os campos preenchidos e, salvo sem mexer, continua igual.
 */
class HelperEditingRoundTripTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZoneOffset.UTC);

    private static final LocalDate OLD_START = LocalDate.of(2026, 1, 5);

    private static final List<LocalTime> TIMES = List.of(LocalTime.of(8, 30), LocalTime.of(20, 0));

    private static final ProtectedFolders PROTECTED = ProtectedFolders.forWindows(
            Path.of("C:\\Windows"), Path.of("C:\\Program Files"), Path.of("C:\\Program Files (x86)"),
            Path.of("C:\\ProgramData"), Path.of("C:\\Users\\nome"));

    @TempDir
    Path tempDir;

    private final FakeHelperRepository repository = new FakeHelperRepository();

    private final SaveHelperUseCase saveHelper =
            new SaveHelperUseCase(repository, new FakeTaskScheduler(), new HelperRules(PROTECTED));

    @Test
    void organizerIsEditedWithItsFieldsAndSavedUnchanged() {
        Helper original = new FolderOrganizerHelper(HelperId.newId(), new HelperName("Organizar Downloads"),
                new Schedule(3, TIMES, OLD_START), false, tempDir.resolve("Downloads"));
        JavaFx.run(() -> {
            Parent view = openEditing(original, HelperKind.ORGANIZER);

            assertEquals("Editar ajudante", label(view, "titleLabel").getText());
            assertEquals("Salvar alterações", save(view).getText());
            assertFalse(save(view).isDisabled());
            assertCommonFields(view, original, false);
            assertEquals(tempDir.resolve("Downloads"), ((FolderField) view.lookup("#folderField")).getFolder());

            save(view).fire();

            assertEquals(original, onlySaved());
        });
    }

    @Test
    void folderCleanerWithLimitIsEditedWithItsFieldsAndSavedUnchanged() {
        Helper original = new CleanerHelper(HelperId.newId(), new HelperName("Limpar temporários"),
                new Schedule(7, TIMES, OLD_START), true,
                new FolderTarget(tempDir.resolve("Temp"), AgeReference.CREATED), new AgeLimit(30));
        JavaFx.run(() -> {
            Parent view = openEditing(original, HelperKind.CLEANER);

            assertEquals("Editar ajudante", label(view, "titleLabel").getText());
            assertFalse(save(view).isDisabled());
            assertCommonFields(view, original, true);
            assertTrue(toggle(view, "folderChoice").isSelected());
            assertEquals(tempDir.resolve("Temp"), ((FolderField) view.lookup("#folderField")).getFolder());
            assertTrue(toggle(view, "olderChoice").isSelected());
            assertEquals("30", label(view, "ageDaysLabel").getText());
            assertTrue(toggle(view, "createdChoice").isSelected());

            save(view).fire();

            assertEquals(original, onlySaved());
        });
    }

    @Test
    void recycleBinCleanerWithLimitIsEditedWithItsFieldsAndSavedUnchanged() {
        Helper original = new CleanerHelper(HelperId.newId(), new HelperName("Esvaziar lixeira"),
                new Schedule(1, TIMES, OLD_START), true, new RecycleBinTarget(), new AgeLimit(7));
        JavaFx.run(() -> {
            Parent view = openEditing(original, HelperKind.CLEANER);

            assertFalse(save(view).isDisabled());
            assertCommonFields(view, original, true);
            assertTrue(toggle(view, "binChoice").isSelected());
            assertTrue(toggle(view, "olderChoice").isSelected());
            assertEquals("7", label(view, "ageDaysLabel").getText());

            save(view).fire();

            assertEquals(original, onlySaved());
        });
    }

    @Test
    void cleanerWithoutLimitIsEditedAsEverythingAndSavedUnchanged() {
        Helper original = new CleanerHelper(HelperId.newId(), new HelperName("Limpar tudo"),
                new Schedule(2, TIMES, OLD_START), true,
                new FolderTarget(tempDir.resolve("Lixo"), AgeReference.MODIFIED), null);
        JavaFx.run(() -> {
            Parent view = openEditing(original, HelperKind.CLEANER);

            assertTrue(toggle(view, "allChoice").isSelected());
            assertTrue(toggle(view, "modifiedChoice").isSelected());

            save(view).fire();

            assertEquals(original, onlySaved());
        });
    }

    @Test
    void switchingAFolderCleanerToTheRecycleBinKeepsTheLimit() {
        Helper original = new CleanerHelper(HelperId.newId(), new HelperName("Limpar"),
                new Schedule(7, TIMES, OLD_START), true,
                new FolderTarget(tempDir.resolve("Temp"), AgeReference.CREATED), new AgeLimit(30));
        JavaFx.run(() -> {
            Parent view = openEditing(original, HelperKind.CLEANER);

            toggle(view, "binChoice").fire();
            save(view).fire();

            CleanerHelper saved = (CleanerHelper) onlySaved();
            assertEquals(new RecycleBinTarget(), saved.target());
            assertEquals(new AgeLimit(30), saved.ageLimit());
            assertEquals(OLD_START, saved.schedule().startDate());
        });
    }

    private void assertCommonFields(Parent view, Helper original, boolean enabled) {
        assertEquals(original.name().value(), ((TextField) view.lookup("#nameField")).getText());
        ScheduleEditor editor = (ScheduleEditor) view.lookup("#scheduleEditor");
        assertEquals(original.schedule().intervalDays(), editor.getIntervalDays());
        assertEquals(TIMES, editor.getTimes());
        assertEquals(enabled, ((ToggleSwitch) view.lookup("#enabledSwitch")).isSelected());
    }

    /** Salva o ajudante, lista pelo caso de uso, converte e abre o formulário pela criação da lista. */
    private Parent openEditing(Helper original, HelperKind kind) {
        saveHelper.execute(original);
        HelperItemMapper mapper = new HelperItemMapper(CLOCK);
        HelperItem item = mapper.toItems(new ListHelpersUseCase(repository).execute()).get(0);
        HelperListController list = new HelperListController(new ListHelpersUseCase(repository), mapper);
        list.setSaveServices(saveHelper, CLOCK);
        return JavaFx.mount(list.createForm(kind, item).createView());
    }

    private Helper onlySaved() {
        assertEquals(1, repository.findAll().size());
        return repository.findAll().get(0);
    }

    private static ToggleButton toggle(Parent view, String id) {
        return (ToggleButton) view.lookup("#" + id);
    }

    private static Label label(Parent view, String id) {
        return (Label) view.lookup("#" + id);
    }

    private static Button save(Parent view) {
        return (Button) view.lookup("#saveButton");
    }
}
