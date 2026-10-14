package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.presentation.component.FolderField;
import com.tidyfy.presentation.component.ScheduleEditor;
import com.tidyfy.presentation.component.ToggleSwitch;
import com.tidyfy.presentation.model.HelperItem;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testa o formulário do ajudante de Organização de pasta.
 */
class FolderOrganizerFormControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void newHelperUsesCreationTexts() {
        JavaFx.run(() -> {
            Parent view = form(null);

            assertEquals("Novo ajudante", text(view, "#titleLabel"));
            assertEquals("Criar ajudante", save(view).getText());
            assertEquals("Ajudante ligado", text(view, "#enabledTitle"));
        });
    }

    @Test
    void newHelperStartsOnWithOneTime() {
        JavaFx.run(() -> {
            Parent view = form(null);

            assertTrue(((ToggleSwitch) view.lookup("#enabledSwitch")).isSelected());
            assertEquals(List.of(LocalTime.of(9, 0)), ((ScheduleEditor) view.lookup("#scheduleEditor")).getTimes());
        });
    }

    @Test
    void cannotSaveUntilNameAndFolderAreFilled() {
        JavaFx.run(() -> {
            Parent view = form(null);
            assertTrue(save(view).isDisabled());

            name(view).setText("Organizar Downloads");
            assertTrue(save(view).isDisabled());

            folder(view).setFolder(tempDir.resolve("Downloads"));
            assertFalse(save(view).isDisabled());
        });
    }

    @Test
    void blankNameCannotBeSaved() {
        JavaFx.run(() -> {
            Parent view = form(null);
            folder(view).setFolder(tempDir.resolve("Downloads"));

            name(view).setText("   ");

            assertTrue(save(view).isDisabled());
        });
    }

    @Test
    void nameErrorSurvivesGarbageCollection() {
        JavaFx.run(() -> {
            Parent view = form(null);

            for (int i = 0; i < 5; i++) {
                System.gc();
            }
            name(view).setText("a");
            name(view).setText("");

            assertTrue(name(view).getStyleClass().contains("invalid"));
        });
    }

    @Test
    void nameErrorAppearsOnlyAfterTyping() {
        JavaFx.run(() -> {
            Parent view = form(null);
            assertFalse(name(view).getStyleClass().contains("invalid"));

            name(view).setText("a");
            name(view).setText("");

            assertTrue(name(view).getStyleClass().contains("invalid"));
            assertEquals("O nome é obrigatório.", text(view, "#nameHelp"));
        });
    }

    @Test
    void editingFillsTheFormWithTheHelper() {
        JavaFx.run(() -> {
            Parent view = form(SampleHelpers.organizer("h1", "Organizar Downloads"));

            assertEquals("Editar ajudante", text(view, "#titleLabel"));
            assertEquals("Salvar alterações", save(view).getText());
            assertEquals("Organizar Downloads", name(view).getText());
            assertEquals(Path.of("C:\\Users\\ana\\Downloads"), folder(view).getFolder());
            assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    ((ScheduleEditor) view.lookup("#scheduleEditor")).getTimes());
            assertFalse(save(view).isDisabled());
        });
    }

    @Test
    void previewShowsTheChosenFolder() {
        JavaFx.run(() -> {
            Parent view = form(null);
            assertEquals("Sua pasta", text(view, "#rootFolderLabel"));

            folder(view).setFolder(tempDir.resolve("Downloads"));

            assertEquals("Downloads", text(view, "#rootFolderLabel"));
        });
    }

    @Test
    void turningTheHelperOffExplainsItLeavesTheTaskScheduler() {
        JavaFx.run(() -> {
            Parent view = form(null);

            ((ToggleSwitch) view.lookup("#enabledSwitch")).setSelected(false);

            assertEquals("Ajudante desligado", text(view, "#enabledTitle"));
            assertEquals("Não fica no Agendador e não executa.", text(view, "#enabledHelp"));
        });
    }

    private static Parent form(HelperItem editing) {
        return JavaFx.mount(new FolderOrganizerFormController(editing).createView());
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

    private static String text(Parent view, String selector) {
        return ((Label) view.lookup(selector)).getText();
    }
}
