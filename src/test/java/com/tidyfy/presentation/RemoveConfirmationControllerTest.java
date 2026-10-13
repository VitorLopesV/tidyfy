package com.tidyfy.presentation;

import static com.tidyfy.presentation.SampleHelpers.folderCleaner;
import static com.tidyfy.presentation.SampleHelpers.organizer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

/**
 * Testa o diálogo de confirmação de remoção.
 */
class RemoveConfirmationControllerTest {

    @Test
    void listsEveryHelperThatWillBeRemoved() {
        JavaFx.run(() -> {
            Parent view = confirmation(new RemoveConfirmationController(
                    List.of(organizer("h1", "Organizar Downloads"), folderCleaner("h2", "Limpar temporários"))));

            assertEquals("Remover 2 ajudantes?", ((Label) view.lookup("#titleLabel")).getText());
            assertEquals("Remover 2 ajudantes", ((Button) view.lookup("#confirmButton")).getText());
            assertEquals(2, ((Pane) view.lookup("#itemsBox")).getChildren().size());
        });
    }

    @Test
    void usesSingularTextsForOneHelper() {
        JavaFx.run(() -> {
            Parent view = confirmation(new RemoveConfirmationController(List.of(organizer("h1", "Organizar Downloads"))));

            assertEquals("Remover este ajudante?", ((Label) view.lookup("#titleLabel")).getText());
            assertEquals("Remover ajudante", ((Button) view.lookup("#confirmButton")).getText());
        });
    }

    @Test
    void confirmingIsRemembered() {
        JavaFx.run(() -> {
            RemoveConfirmationController controller =
                    new RemoveConfirmationController(List.of(organizer("h1", "Organizar Downloads")));
            Parent view = confirmation(controller);

            ((Button) view.lookup("#confirmButton")).fire();

            assertTrue(controller.isConfirmed());
        });
    }

    @Test
    void closingWithoutConfirmingRemovesNothing() {
        JavaFx.run(() -> {
            RemoveConfirmationController controller =
                    new RemoveConfirmationController(List.of(organizer("h1", "Organizar Downloads")));
            confirmation(controller);

            assertFalse(controller.isConfirmed());
        });
    }

    private static Parent confirmation(RemoveConfirmationController controller) {
        return JavaFx.mount(controller.createView());
    }
}
