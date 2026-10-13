package com.tidyfy.presentation.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.presentation.JavaFx;
import java.nio.file.Path;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testa o campo de escolha de pasta.
 */
class FolderFieldTest {

    @TempDir
    Path tempDir;

    @Test
    void displayNameIsTheFolderName() {
        assertEquals("Downloads", FolderField.displayName(tempDir.resolve("Downloads")));
    }

    @Test
    void displayNameOfADriveIsTheWholePath() {
        Path drive = tempDir.getRoot();

        assertEquals(drive.toString(), FolderField.displayName(drive));
    }

    @Test
    void withoutFolderInvitesToChooseOne() {
        JavaFx.run(() -> {
            FolderField field = new FolderField();

            assertTrue(labels(field).contains("Nenhuma pasta escolhida"));
            assertEquals("Escolher pasta…", button(field).getText());
        });
    }

    @Test
    void showsTheChosenFolderNameAndPath() {
        Path folder = tempDir.resolve("Downloads");
        JavaFx.run(() -> {
            FolderField field = new FolderField();
            field.setFolder(folder);

            assertTrue(labels(field).contains("Downloads"));
            assertTrue(labels(field).contains(folder.toString()));
            assertEquals("Alterar…", button(field).getText());
        });
    }

    private static List<String> labels(FolderField field) {
        return field.lookupAll(".label").stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(Node::isVisible)
                .map(Label::getText)
                .toList();
    }

    private static Button button(FolderField field) {
        return (Button) field.getChildren().getLast();
    }
}
