package com.tidyfy.presentation;

import static com.tidyfy.presentation.SampleHelpers.folderCleaner;
import static com.tidyfy.presentation.SampleHelpers.organizer;
import static com.tidyfy.presentation.SampleHelpers.recycleBinCleaner;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.presentation.component.HelperCard;
import com.tidyfy.presentation.model.HelperItem;
import com.tidyfy.presentation.model.HelperKind;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

/**
 * Testa a lista de ajudantes: contadores, filtros, seleção e ações.
 */
class HelperListControllerTest {

    private static final List<HelperItem> HELPERS = List.of(
            organizer("h1", "Organizar Downloads"),
            folderCleaner("h2", "Limpar temporários"),
            recycleBinCleaner("h3", "Esvaziar lixeira"));

    @Test
    void startsWithTheEmptyStateWhenThereAreNoHelpers() {
        JavaFx.run(() -> {
            Parent view = screen(List.of());

            assertTrue(view.lookup("#emptyState").isVisible());
            assertFalse(view.lookup("#listSection").isVisible());
            assertEquals("0", text(view, "#enabledCount"));
        });
    }

    @Test
    void showsOneCardPerHelper() {
        JavaFx.run(() -> {
            Parent view = screen(HELPERS);

            assertEquals(3, cards(view).size());
            assertFalse(view.lookup("#emptyState").isVisible());
        });
    }

    @Test
    void showsHowManyHelpersOfEachTypeExistAndHowManyAreOn() {
        JavaFx.run(() -> {
            Parent view = screen(HELPERS);

            assertEquals("1", text(view, "#organizerCount"));
            assertEquals("2", text(view, "#cleanerCount"));
            assertEquals("2 de 3", text(view, "#enabledCount"));
        });
    }

    @Test
    void nineHelpersFitInTheDefaultWindowWithoutAScrollBar() {
        JavaFx.run(() -> {
            Parent view = screen(SampleHelpers.many(9));

            assertFalse(cardsScrollBar(view).isVisible());
        });
    }

    @Test
    void emptyStateFitsInTheSmallestWindow() {
        JavaFx.run(() -> {
            Parent view = screen(List.of(), 960, 700);
            Region empty = (Region) view.lookup("#emptyState");

            Bounds inWindow = empty.localToScene(empty.getLayoutBounds());
            assertTrue(inWindow.getMaxY() <= 700);
            assertTrue(empty.getHeight() >= empty.prefHeight(empty.getWidth()));
        });
    }

    @Test
    void onlyTheCardsAreaScrollsWhenHelpersDoNotFit() {
        JavaFx.run(() -> {
            Parent view = screen(SampleHelpers.many(15));

            assertEquals(1, view.lookupAll(".scroll-pane").size());
            assertTrue(cardsScrollBar(view).isVisible());
        });
    }

    @Test
    void filterStaysVisibleUntilAHelperIsSelected() {
        JavaFx.run(() -> {
            Parent view = screen(HELPERS);

            assertTrue(view.lookup("#filterBar").isVisible());
            assertFalse(view.lookup("#selectionActions").isVisible());
        });
    }

    @Test
    void selectingOneHelperShowsEditAndRemoveInPlaceOfTheFilter() {
        JavaFx.run(() -> {
            Parent view = screen(HELPERS);

            cards(view).get(0).selectedProperty().set(true);

            assertTrue(view.lookup("#selectionActions").isVisible());
            assertFalse(view.lookup("#filterBar").isVisible());
            assertFalse(button(view, "#editButton").isDisabled());
            assertFalse(button(view, "#removeButton").isDisabled());
            assertEquals("Remover", button(view, "#removeButton").getText());
            assertFalse(view.lookup("#editHint").isVisible());
        });
    }

    @Test
    void severalSelectedHelpersCanBeRemovedButNotEdited() {
        JavaFx.run(() -> {
            Parent view = screen(HELPERS);

            cards(view).get(0).selectedProperty().set(true);
            cards(view).get(1).selectedProperty().set(true);

            assertTrue(button(view, "#editButton").isDisabled());
            assertFalse(button(view, "#removeButton").isDisabled());
            assertEquals("Remover 2", button(view, "#removeButton").getText());
            assertTrue(view.lookup("#editHint").isVisible());
            assertEquals("2", text(view, "#selectionCount"));
        });
    }

    @Test
    void editIsAvailableAgainWhenOnlyOneHelperRemainsSelected() {
        JavaFx.run(() -> {
            Parent view = screen(HELPERS);
            cards(view).get(0).selectedProperty().set(true);
            cards(view).get(1).selectedProperty().set(true);

            cards(view).get(1).selectedProperty().set(false);

            assertFalse(button(view, "#editButton").isDisabled());
            assertFalse(view.lookup("#editHint").isVisible());
        });
    }

    @Test
    void selectionDoesNotChangeTheHeightOfTheToolbar() {
        JavaFx.run(() -> {
            Parent view = screen(HELPERS);
            double before = view.lookup("#cardsScroll").getLayoutY();

            cards(view).get(0).selectedProperty().set(true);
            view.layout();

            assertEquals(before, view.lookup("#cardsScroll").getLayoutY(), 0.01);
        });
    }

    @Test
    void typeFilterShowsOnlyHelpersOfThatType() {
        JavaFx.run(() -> {
            Parent view = screen(HELPERS);

            ((ToggleButton) view.lookup("#filterCleaner")).fire();

            List<HelperCard> cards = cards(view);
            assertEquals(2, cards.size());
            assertTrue(cards.stream().allMatch(card -> card.item().kind() == HelperKind.CLEANER));
        });
    }

    @Test
    void selectedFilterCannotBeTurnedOff() {
        JavaFx.run(() -> {
            Parent view = screen(HELPERS);
            ToggleButton all = (ToggleButton) view.lookup("#filterAll");

            all.fire();

            assertTrue(all.isSelected());
            assertEquals(3, cards(view).size());
        });
    }

    @Test
    void changingTheFilterClearsTheSelection() {
        JavaFx.run(() -> {
            Parent view = screen(HELPERS);
            cards(view).get(0).selectedProperty().set(true);

            ((ToggleButton) view.lookup("#filterOrganizer")).fire();

            assertFalse(view.lookup("#selectionActions").isVisible());
            assertFalse(cards(view).get(0).selectedProperty().get());
        });
    }

    @Test
    void filterWithoutHelpersOfThatTypeSaysSo() {
        JavaFx.run(() -> {
            Parent view = screen(List.of(organizer("h1", "Organizar Downloads")));

            ((ToggleButton) view.lookup("#filterCleaner")).fire();

            assertTrue(cards(view).isEmpty());
            assertTrue(view.lookup("#noResults").isVisible());
        });
    }

    @Test
    void selectAllSelectsEveryVisibleHelper() {
        JavaFx.run(() -> {
            Parent view = screen(HELPERS);

            ((CheckBox) view.lookup("#selectAll")).fire();

            assertTrue(cards(view).stream().allMatch(card -> card.selectedProperty().get()));
            assertEquals("Remover 3", button(view, "#removeButton").getText());
        });
    }

    @Test
    void selectAllIsPartiallyCheckedWhenSomeHelpersAreSelected() {
        JavaFx.run(() -> {
            Parent view = screen(HELPERS);

            cards(view).get(0).selectedProperty().set(true);

            CheckBox selectAll = (CheckBox) view.lookup("#selectAll");
            assertTrue(selectAll.isIndeterminate());
            assertFalse(selectAll.isSelected());
        });
    }

    @Test
    void clearingTheSelectionBringsTheFilterBack() {
        JavaFx.run(() -> {
            Parent view = screen(HELPERS);
            cards(view).get(0).selectedProperty().set(true);
            cards(view).get(2).selectedProperty().set(true);

            button(view, "#clearSelectionButton").fire();

            assertFalse(view.lookup("#selectionActions").isVisible());
            assertTrue(view.lookup("#filterBar").isVisible());
            assertTrue(cards(view).stream().noneMatch(card -> card.selectedProperty().get()));
        });
    }

    @Test
    void turningAHelperOffUpdatesTheEnabledCount() {
        JavaFx.run(() -> {
            Parent view = screen(HELPERS);

            cards(view).get(0).enabledProperty().set(false);

            assertEquals("1 de 3", text(view, "#enabledCount"));
        });
    }

    private static Parent screen(List<HelperItem> helpers) {
        return screen(helpers, 1280, 900);
    }

    private static Parent screen(List<HelperItem> helpers, double width, double height) {
        HelperListController controller = new HelperListController();
        Parent view = JavaFx.mount(controller.createView(), width, height);
        controller.showHelpers(helpers);
        view.applyCss();
        view.layout();
        view.layout();
        return view;
    }

    private static ScrollBar cardsScrollBar(Parent view) {
        return view.lookup("#cardsScroll").lookupAll(".scroll-bar").stream()
                .map(ScrollBar.class::cast)
                .filter(bar -> bar.getOrientation() == Orientation.VERTICAL)
                .findFirst()
                .orElseThrow();
    }

    private static List<HelperCard> cards(Parent view) {
        return ((Pane) view.lookup("#cardGrid")).getChildren().stream().map(HelperCard.class::cast).toList();
    }

    private static Button button(Parent view, String selector) {
        return (Button) view.lookup(selector);
    }

    private static String text(Parent view, String selector) {
        return ((Label) view.lookup(selector)).getText();
    }
}
