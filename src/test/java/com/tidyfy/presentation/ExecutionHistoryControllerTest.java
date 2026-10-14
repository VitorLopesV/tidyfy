package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.presentation.model.ExecutionAction;
import com.tidyfy.presentation.model.ExecutionItem;
import java.util.List;
import java.util.stream.IntStream;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

/**
 * Testa o diálogo de histórico de execuções.
 */
class ExecutionHistoryControllerTest {

    @Test
    void showsTheHelperNameInTheTitle() {
        JavaFx.run(() -> {
            Parent view = history("Organizar Downloads", List.of(item("05/03/2026 12:30", 1)));

            assertEquals("Histórico de Organizar Downloads", ((Label) view.lookup("#titleLabel")).getText());
            assertEquals("1 execução registrada", ((Label) view.lookup("#subtitleLabel")).getText());
        });
    }

    @Test
    void listsTheExecutionsInTheGivenOrderWithDateSummaryAndActions() {
        JavaFx.run(() -> {
            Parent view = history("Limpar", List.of(item("05/03/2026 12:30", 2), item("04/03/2026 09:00", 0)));

            Pane itemsBox = (Pane) view.lookup("#itemsBox");
            assertEquals(2, itemsBox.getChildren().size());
            assertEquals("2 execuções registradas", ((Label) view.lookup("#subtitleLabel")).getText());
            assertEquals(List.of("05/03/2026 12:30", "04/03/2026 09:00"), labelsOf(itemsBox, "history-date"));
            assertEquals(List.of("2 ações", "Nada a fazer"), labelsOf(itemsBox, "history-summary"));
            assertEquals(List.of("ação 0", "ação 1"), labelsOf(itemsBox, "history-action"));
            assertTrue(view.lookup("#historyScroll").isVisible());
            assertFalse(view.lookup("#emptyBox").isVisible());
            assertFalse(view.lookup("#emptyBox").isManaged());
        });
    }

    @Test
    void highlightsFailures() {
        JavaFx.run(() -> {
            ExecutionItem failed = new ExecutionItem("05/03/2026 12:30", "1 ação, 1 com falha",
                    List.of(new ExecutionAction("Falha em a.txt: Acesso negado", null, true)));
            Parent view = history("Limpar", List.of(failed));

            Pane itemsBox = (Pane) view.lookup("#itemsBox");
            assertNotNull(itemsBox.lookup(".history-item-failed"));
            assertNotNull(itemsBox.lookup(".history-action-failed"));
            assertNotNull(itemsBox.lookup(".history-summary-failed"));
        });
    }

    @Test
    void showsOnlyTheFirstActionsOfBigExecutionsUntilExpanded() {
        JavaFx.run(() -> {
            Parent view = history("Organizar", List.of(item("05/03/2026 12:30", 8)));
            Pane itemsBox = (Pane) view.lookup("#itemsBox");

            assertEquals(ExecutionHistoryController.VISIBLE_ACTIONS, labelsOf(itemsBox, "history-action").size());
            Button toggle = (Button) itemsBox.lookup(".history-toggle");
            assertEquals("Ver todas as 8 ações", toggle.getText());

            toggle.fire();
            assertEquals(8, labelsOf(itemsBox, "history-action").size());
            assertEquals("Mostrar menos", toggle.getText());

            toggle.fire();
            assertEquals(ExecutionHistoryController.VISIBLE_ACTIONS, labelsOf(itemsBox, "history-action").size());
        });
    }

    @Test
    void smallExecutionsHaveNoToggle() {
        JavaFx.run(() -> {
            Parent view = history("Organizar", List.of(item("05/03/2026 12:30", ExecutionHistoryController.VISIBLE_ACTIONS)));

            assertEquals(null, view.lookup(".history-toggle"));
        });
    }

    @Test
    void manyExecutionsScrollInsteadOfGrowingTheDialog() {
        JavaFx.run(() -> {
            List<ExecutionItem> many = IntStream.range(0, 40).mapToObj(i -> item("05/03/2026 12:30", 3)).toList();
            Parent view = history("Organizar", many);

            ScrollPane scroll = (ScrollPane) view.lookup("#historyScroll");
            VBox itemsBox = (VBox) view.lookup("#itemsBox");
            assertEquals(40, itemsBox.getChildren().size());
            assertTrue(scroll.getHeight() < itemsBox.getHeight());
        });
    }

    @Test
    void emptyHistoryShowsTheMessageAndTheRetention() {
        JavaFx.run(() -> {
            Parent view = history("Organizar", List.of());

            assertTrue(view.lookup("#emptyBox").isVisible());
            assertFalse(view.lookup("#historyScroll").isVisible());
            assertEquals("Este ajudante ainda não foi executado.", ((Label) view.lookup("#emptyLabel")).getText());
            assertEquals("Nenhuma execução registrada", ((Label) view.lookup("#subtitleLabel")).getText());
            assertEquals("O histórico guarda as execuções dos últimos 30 dias.",
                    ((Label) view.lookup("#retentionLabel")).getText());
        });
    }

    @Test
    void retentionIsShownEvenWithExecutions() {
        JavaFx.run(() -> {
            Parent view = history("Organizar", List.of(item("05/03/2026 12:30", 1)));

            assertEquals("O histórico guarda as execuções dos últimos 30 dias.",
                    ((Label) view.lookup("#retentionLabel")).getText());
        });
    }

    @Test
    void closeButtonsCloseWithoutError() {
        JavaFx.run(() -> {
            Parent view = history("Organizar", List.of());

            ((Button) view.lookup("#closeButton")).fire();
            ((Button) view.lookup("#closeIconButton")).fire();
        });
    }

    @Test
    void dialogHasADragHandle() {
        JavaFx.run(() -> assertNotNull(history("Organizar", List.of()).lookup(".drag-handle")));
    }

    private static Parent history(String helperName, List<ExecutionItem> items) {
        return JavaFx.mount(new ExecutionHistoryController(helperName, items).createView(), 700, 640);
    }

    private static ExecutionItem item(String date, int actions) {
        List<ExecutionAction> list = IntStream.range(0, actions)
                .mapToObj(i -> new ExecutionAction("ação " + i, null, false)).toList();
        return new ExecutionItem(date, actions == 0 ? "Nada a fazer" : actions + " ações", list);
    }

    private static List<String> labelsOf(Node parent, String styleClass) {
        return parent.lookupAll("." + styleClass).stream()
                .filter(Label.class::isInstance).map(node -> ((Label) node).getText()).toList();
    }
}
