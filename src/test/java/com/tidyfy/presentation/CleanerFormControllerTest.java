package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.presentation.component.FolderField;
import com.tidyfy.presentation.model.HelperItem;
import java.nio.file.Path;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testa o formulário do ajudante de Limpeza.
 */
class CleanerFormControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void newCleanerStartsTargetingAFolder() {
        JavaFx.run(() -> {
            Parent view = form(null);

            assertTrue(choice(view, "#folderChoice").isSelected());
            assertTrue(view.lookup("#folderSection").isVisible());
            assertTrue(view.lookup("#folderPreview").isVisible());
        });
    }

    @Test
    void folderTargetNeedsAFolderBeforeSaving() {
        JavaFx.run(() -> {
            Parent view = form(null);
            name(view).setText("Limpar temporários");
            assertTrue(save(view).isDisabled());

            folder(view).setFolder(tempDir.resolve("Temp"));

            assertFalse(save(view).isDisabled());
        });
    }

    @Test
    void recycleBinTargetNeedsNoFolder() {
        JavaFx.run(() -> {
            Parent view = form(null);
            name(view).setText("Esvaziar lixeira");

            choice(view, "#binChoice").fire();

            assertFalse(save(view).isDisabled());
            assertFalse(view.lookup("#folderSection").isVisible());
            assertTrue(view.lookup("#binPreview").isVisible());
        });
    }

    @Test
    void selectedTargetCannotBeTurnedOff() {
        JavaFx.run(() -> {
            Parent view = form(null);
            ToggleButton folderChoice = choice(view, "#folderChoice");

            folderChoice.fire();

            assertTrue(folderChoice.isSelected());
        });
    }

    @Test
    void editingARecycleBinCleanerSelectsTheRecycleBin() {
        JavaFx.run(() -> {
            Parent view = form(SampleHelpers.recycleBinCleaner("h3", "Esvaziar lixeira"));

            assertTrue(choice(view, "#binChoice").isSelected());
            assertNull(folder(view).getFolder());
            assertEquals("Editar ajudante", ((Label) view.lookup("#titleLabel")).getText());
        });
    }

    @Test
    void editingAFolderCleanerFillsTheFolder() {
        JavaFx.run(() -> {
            Parent view = form(SampleHelpers.folderCleaner("h2", "Limpar temporários"));

            assertTrue(choice(view, "#folderChoice").isSelected());
            assertEquals(Path.of("C:\\Temp"), folder(view).getFolder());
            assertEquals("Temp", ((Label) view.lookup("#previewFolderName")).getText());
        });
    }

    private static Parent form(HelperItem editing) {
        return JavaFx.mount(new CleanerFormController(editing).createView());
    }

    private static ToggleButton choice(Parent view, String selector) {
        return (ToggleButton) view.lookup(selector);
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
