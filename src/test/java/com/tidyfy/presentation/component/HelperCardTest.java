package com.tidyfy.presentation.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.presentation.JavaFx;
import com.tidyfy.presentation.SampleHelpers;
import com.tidyfy.presentation.model.HelperItem;
import java.time.LocalTime;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.Test;

/**
 * Testa o cartão de ajudante: textos, seleção e estado ligado/desligado.
 */
class HelperCardTest {

    @Test
    void showsNameTypeAndFolder() {
        JavaFx.run(() -> {
            HelperCard card = new HelperCard(SampleHelpers.organizer("h1", "Organizar Downloads"));

            assertEquals("Organizar Downloads", text(card, "#nameLabel"));
            assertEquals("ORGANIZAÇÃO", text(card, "#kindLabel"));
            assertEquals("Downloads", text(card, "#targetName"));
            assertEquals("C:\\Users\\ana\\Downloads", text(card, "#targetDetail"));
            assertTrue(card.getStyleClass().contains("kind-org"));
        });
    }

    @Test
    void recycleBinHelperShowsTheRecycleBinAsTarget() {
        JavaFx.run(() -> {
            HelperCard card = new HelperCard(SampleHelpers.recycleBinCleaner("h2", "Esvaziar lixeira"));

            assertEquals("Lixeira do Windows", text(card, "#targetName"));
        });
    }

    @Test
    void showsScheduleWithOneChipPerTime() {
        JavaFx.run(() -> {
            HelperCard card = new HelperCard(SampleHelpers.organizer("h1", "Organizar Downloads"));

            assertEquals("Todo dia", text(card, "#everyLabel"));
            assertEquals(2, card.lookupAll(".time-chip").size());
        });
    }

    @Test
    void showsAtMostTwoTimesAndCountsTheRest() {
        JavaFx.run(() -> {
            HelperItem base = SampleHelpers.organizer("h1", "Organizar Downloads");
            HelperCard card = new HelperCard(new HelperItem(base.id(), base.name(), base.kind(), base.folder(), 1,
                    List.of(LocalTime.of(7, 0), LocalTime.of(12, 0), LocalTime.of(18, 0), LocalTime.of(22, 0)),
                    true, base.nextRun(), base.nextRunProgress()));

            List<String> chips = card.lookupAll(".time-chip").stream().map(node -> ((Label) node).getText()).toList();
            assertEquals(List.of("07:00", "12:00", "+2"), chips);
        });
    }

    @Test
    void enabledHelperShowsTheNextRun() {
        JavaFx.run(() -> {
            HelperCard card = new HelperCard(SampleHelpers.organizer("h1", "Organizar Downloads"));

            assertTrue(card.lookup("#nextRunBox").isVisible());
            assertEquals("Hoje, 18:00", text(card, "#nextRunLabel"));
            assertFalse(card.lookup("#pausedBox").isVisible());
        });
    }

    @Test
    void pausedHelperShowsItIsPaused() {
        JavaFx.run(() -> {
            HelperCard card = new HelperCard(SampleHelpers.folderCleaner("h3", "Limpar temporários"));

            assertFalse(card.enabledProperty().get());
            assertTrue(card.lookup("#pausedBox").isVisible());
            assertFalse(card.lookup("#nextRunBox").isVisible());
        });
    }

    @Test
    void turningTheSwitchOffPausesTheCard() {
        JavaFx.run(() -> {
            HelperCard card = new HelperCard(SampleHelpers.organizer("h1", "Organizar Downloads"));

            card.enabledProperty().set(false);

            assertTrue(card.lookup("#pausedBox").isVisible());
            assertTrue(card.getPseudoClassStates().stream().anyMatch(state -> state.getPseudoClassName().equals("paused")));
        });
    }

    @Test
    void clickingAnywhereOnTheCardTogglesSelection() {
        JavaFx.run(() -> {
            HelperCard card = new HelperCard(SampleHelpers.organizer("h1", "Organizar Downloads"));

            card.fireEvent(click());
            assertTrue(card.selectedProperty().get());

            card.fireEvent(click());
            assertFalse(card.selectedProperty().get());
        });
    }

    private static String text(HelperCard card, String selector) {
        return ((Label) card.lookup(selector)).getText();
    }

    private static MouseEvent click() {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, true, null);
    }
}
