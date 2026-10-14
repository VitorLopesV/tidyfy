package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.presentation.model.ErrorMessage;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;

/**
 * Testa o diálogo de erro.
 */
class ErrorDialogControllerTest {

    private static final ErrorMessage ERROR = new ErrorMessage("Título", "Mensagem clara", "java.lang.X: y");

    private static Parent dialog() {
        return JavaFx.mount(new ErrorDialogController(ERROR).createView());
    }

    @Test
    void showsTitleAndMessage() {
        JavaFx.run(() -> {
            Parent view = dialog();

            assertEquals("Título", ((Label) view.lookup("#titleLabel")).getText());
            assertEquals("Mensagem clara", ((Label) view.lookup("#messageLabel")).getText());
            assertEquals("java.lang.X: y", ((Label) view.lookup("#detailsLabel")).getText());
        });
    }

    @Test
    void detailsStartCollapsedAndToggleWithTheButton() {
        JavaFx.run(() -> {
            Parent view = dialog();
            Button toggle = (Button) view.lookup("#detailsButton");

            assertFalse(view.lookup("#detailsBox").isVisible());
            assertEquals("Mostrar detalhes", toggle.getText());

            toggle.fire();
            assertTrue(view.lookup("#detailsBox").isVisible());
            assertEquals("Ocultar", toggle.getText());

            toggle.fire();
            assertFalse(view.lookup("#detailsBox").isVisible());
            assertEquals("Mostrar detalhes", toggle.getText());
        });
    }

    @Test
    void closeButtonHidesTheWindow() {
        JavaFx.run(() -> {
            Parent view = dialog();

            ((Button) view.lookup("#closeButton")).fire();

            assertFalse(view.getScene().getWindow().isShowing());
        });
    }

    @Test
    void nullErrorIsRejected() {
        assertThrows(NullPointerException.class, () -> new ErrorDialogController(null));
    }
}
