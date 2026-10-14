package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.usecase.ListHelpersUseCase;
import com.tidyfy.infrastructure.persistence.CorruptedStorageException;
import com.tidyfy.presentation.model.HelperItemMapper;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;

/**
 * Testa a exibição de erros pela lista de ajudantes, sem abrir janelas de verdade.
 */
class HelperListErrorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZoneOffset.UTC);

    /** Lista que guarda os diálogos de erro em vez de abri-los. */
    private static final class RecordingList extends HelperListController {

        final List<ErrorDialogController> presented = new ArrayList<>();

        RecordingList(ListHelpersUseCase useCase) {
            super(useCase, new HelperItemMapper(CLOCK));
        }

        @Override
        void presentError(ErrorDialogController dialog) {
            presented.add(dialog);
        }
    }

    private static RecordingList failingList() {
        return new RecordingList(new ListHelpersUseCase(new HelperListLoadingTest.FailingRepository(
                new CorruptedStorageException(Path.of("helpers.txt"), new RuntimeException("x")))));
    }

    @Test
    void showErrorBuildsTheDialogWithTheMappedMessage() {
        JavaFx.run(() -> {
            RecordingList list = failingList();
            list.setErrorHandler(error -> { });
            JavaFx.mount(list.createView());

            list.showError(new IllegalStateException("quebrou"));

            assertEquals(1, list.presented.size());
            Parent dialog = list.presented.get(0).createView();
            assertEquals("Algo deu errado", ((Label) dialog.lookup("#titleLabel")).getText());
        });
    }

    @Test
    void failureWhileOpeningShowsTheErrorDialogAndKeepsTheScreenWorking() {
        RecordingList list = failingList();
        list.setErrorHandler(list::showError);
        Parent[] view = new Parent[1];

        JavaFx.run(() -> view[0] = JavaFx.mount(list.createView()));
        JavaFx.run(() -> { });

        assertEquals(1, list.presented.size());
        JavaFx.run(() -> {
            Parent dialog = list.presented.get(0).createView();
            assertEquals("Arquivo corrompido", ((Label) dialog.lookup("#titleLabel")).getText());
            assertTrue(view[0].lookup("#emptyState").isVisible());
        });
    }
}
