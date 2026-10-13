package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.presentation.model.HelperKind;
import java.util.Optional;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;

/**
 * Testa o diálogo de escolha do tipo de ajudante.
 */
class HelperTypeChooserControllerTest {

    @Test
    void bothTypesAreAvailableBelowTheLimit() {
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(new HelperTypeChooserController(4, 2).createView());

            assertFalse(view.lookup("#organizerCard").isDisabled());
            assertFalse(view.lookup("#cleanerCard").isDisabled());
            assertFalse(view.lookup("#cleanerLimitBadge").isVisible());
            assertEquals("4", ((Label) view.lookup("#organizerCount")).getText());
        });
    }

    @Test
    void typeAtTheLimitCannotBeChosen() {
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(new HelperTypeChooserController(3, HelperKind.LIMIT_PER_TYPE).createView());

            assertTrue(view.lookup("#cleanerCard").isDisabled());
            assertTrue(view.lookup("#cleanerLimitBadge").isVisible());
            assertEquals("Você já tem 20 ajudantes de limpeza. Remova um deles para criar outro.",
                    ((Label) view.lookup("#cleanerDescription")).getText());
            assertFalse(view.lookup("#organizerCard").isDisabled());
        });
    }

    @Test
    void remembersTheChosenType() {
        JavaFx.run(() -> {
            HelperTypeChooserController chooser = new HelperTypeChooserController(0, 0);
            Parent view = JavaFx.mount(chooser.createView());

            ((Button) view.lookup("#cleanerCard")).fire();

            assertEquals(Optional.of(HelperKind.CLEANER), chooser.chosenKind());
        });
    }

    @Test
    void closingWithoutChoosingChoosesNothing() {
        JavaFx.run(() -> {
            HelperTypeChooserController chooser = new HelperTypeChooserController(0, 0);
            JavaFx.mount(chooser.createView());

            assertEquals(Optional.empty(), chooser.chosenKind());
        });
    }
}
