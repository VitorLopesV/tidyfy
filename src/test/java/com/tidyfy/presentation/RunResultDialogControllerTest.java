package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tidyfy.presentation.model.RunResult;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;

/**
 * Testa o diálogo de sucesso da execução manual.
 */
class RunResultDialogControllerTest {

    private static final RunResult RESULT = new RunResult(true, "Execução concluída", "Organizar rodou.", null);

    @Test
    void showsTitleAndMessage() {
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(new RunResultDialogController(RESULT).createView());

            assertEquals("Execução concluída", ((Label) view.lookup("#titleLabel")).getText());
            assertEquals("Organizar rodou.", ((Label) view.lookup("#messageLabel")).getText());
        });
    }

    @Test
    void closeButtonHidesTheWindow() {
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(new RunResultDialogController(RESULT).createView());

            ((Button) view.lookup("#closeButton")).fire();

            assertFalse(view.getScene().getWindow().isShowing());
        });
    }

    @Test
    void nullResultIsRejected() {
        assertThrows(NullPointerException.class, () -> new RunResultDialogController(null));
    }
}
