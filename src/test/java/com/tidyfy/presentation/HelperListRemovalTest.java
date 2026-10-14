package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeExecutionHistoryRepository;
import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.application.fake.FakeTaskScheduler;
import com.tidyfy.application.usecase.ListHelpersUseCase;
import com.tidyfy.application.usecase.RemoveHelperUseCase;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import com.tidyfy.presentation.component.HelperCard;
import com.tidyfy.presentation.model.HelperItem;
import com.tidyfy.presentation.model.HelperItemMapper;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testa a remoção de ajudantes pela confirmação, com o {@link RemoveHelperUseCase} real sobre dublês das
 * portas: a lista é atualizada, cancelar não remove nada, falhas recarregam a lista e mostram o erro, e o
 * histórico do ajudante removido permanece.
 */
class HelperListRemovalTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZoneOffset.UTC);

    private static final Schedule SCHEDULE =
            new Schedule(1, List.of(LocalTime.of(18, 0)), LocalDate.of(2026, 3, 1));

    private FakeHelperRepository repository;
    private FakeTaskScheduler scheduler;
    private FakeExecutionHistoryRepository history;
    private final List<ErrorDialogController> shownErrors = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repository = new FakeHelperRepository();
        scheduler = new FakeTaskScheduler();
        history = new FakeExecutionHistoryRepository();
        shownErrors.clear();
    }

    private Helper saved(String name) {
        Helper helper = new FolderOrganizerHelper(HelperId.newId(), new HelperName(name), SCHEDULE, true,
                Path.of("C:\\Users\\ana\\" + name));
        repository.with(helper);
        scheduler.register(helper);
        return helper;
    }

    private HelperListController controller() {
        HelperListController controller = new HelperListController(new ListHelpersUseCase(repository),
                new HelperItemMapper(CLOCK), error -> { }) {
            @Override
            void presentError(ErrorDialogController dialog) {
                shownErrors.add(dialog);
            }
        };
        controller.setRemoveService(new RemoveHelperUseCase(repository, scheduler));
        return controller;
    }

    private static Parent mount(HelperListController controller) {
        Parent view = JavaFx.mount(controller.createView(), 1280, 900);
        view.applyCss();
        view.layout();
        return view;
    }

    private static List<HelperCard> cards(Parent view) {
        return ((Pane) view.lookup("#cardGrid")).getChildren().stream().map(HelperCard.class::cast).toList();
    }

    private static String text(Parent view, String selector) {
        return ((Label) view.lookup(selector)).getText();
    }

    private static HelperItem item(Helper helper) {
        return new HelperItemMapper(CLOCK).toItem(helper);
    }

    private static RemoveConfirmationController confirmed(List<HelperItem> items) {
        RemoveConfirmationController confirmation = new RemoveConfirmationController(items);
        Parent view = JavaFx.mount(confirmation.createView());
        ((Button) view.lookup("#confirmButton")).fire();
        return confirmation;
    }

    private static RemoveConfirmationController notConfirmed(List<HelperItem> items) {
        RemoveConfirmationController confirmation = new RemoveConfirmationController(items);
        JavaFx.mount(confirmation.createView());
        return confirmation;
    }

    @Test
    void removesOneConfirmedHelperFromTheRepositoryTheSchedulerAndTheList() {
        Helper downloads = saved("Downloads");
        Helper photos = saved("Fotos");
        JavaFx.run(() -> {
            HelperListController controller = controller();
            Parent view = mount(controller);
            cards(view).get(0).selectedProperty().set(true);

            controller.afterRemoveConfirmation(confirmed(List.of(item(downloads))), List.of(item(downloads)));

            assertTrue(repository.findById(downloads.id()).isEmpty());
            assertTrue(repository.findById(photos.id()).isPresent());
            assertFalse(scheduler.isRegistered(downloads.id()));
            assertTrue(scheduler.isRegistered(photos.id()));
            assertEquals(1, cards(view).size());
            assertEquals("1", text(view, "#organizerCount"));
            assertTrue(shownErrors.isEmpty());
        });
    }

    @Test
    void removesSeveralHelpersAtOnceAndClearsTheSelection() {
        Helper downloads = saved("Downloads");
        Helper photos = saved("Fotos");
        Helper docs = saved("Documentos");
        JavaFx.run(() -> {
            HelperListController controller = controller();
            Parent view = mount(controller);
            cards(view).get(0).selectedProperty().set(true);
            cards(view).get(1).selectedProperty().set(true);
            List<HelperItem> chosen = List.of(item(downloads), item(photos));

            controller.afterRemoveConfirmation(confirmed(chosen), chosen);

            assertEquals(List.of(docs), repository.findAll());
            assertEquals(List.of(docs.id()), scheduler.registered());
            assertEquals(1, cards(view).size());
            assertFalse(view.lookup("#selectionActions").isVisible());
            assertTrue(cards(view).stream().noneMatch(card -> card.selectedProperty().get()));
        });
    }

    @Test
    void removingTheLastHelperShowsTheEmptyState() {
        Helper downloads = saved("Downloads");
        JavaFx.run(() -> {
            HelperListController controller = controller();
            Parent view = mount(controller);

            controller.afterRemoveConfirmation(confirmed(List.of(item(downloads))), List.of(item(downloads)));

            assertTrue(view.lookup("#emptyState").isVisible());
            assertFalse(view.lookup("#listSection").isVisible());
            assertEquals("0", text(view, "#organizerCount"));
            assertEquals("0", text(view, "#enabledCount"));
        });
    }

    @Test
    void cancellingRemovesNothing() {
        Helper downloads = saved("Downloads");
        JavaFx.run(() -> {
            HelperListController controller = controller();
            Parent view = mount(controller);
            cards(view).get(0).selectedProperty().set(true);

            controller.afterRemoveConfirmation(notConfirmed(List.of(item(downloads))), List.of(item(downloads)));

            assertTrue(repository.findById(downloads.id()).isPresent());
            assertTrue(scheduler.isRegistered(downloads.id()));
            assertTrue(scheduler.unregisterCalls().isEmpty());
            assertEquals(1, cards(view).size());
            assertTrue(cards(view).get(0).selectedProperty().get());
        });
    }

    @Test
    void withoutTheRemoveServiceConfirmingRemovesNothing() {
        Helper downloads = saved("Downloads");
        JavaFx.run(() -> {
            HelperListController controller = new HelperListController(new ListHelpersUseCase(repository),
                    new HelperItemMapper(CLOCK), error -> { });
            Parent view = mount(controller);

            controller.afterRemoveConfirmation(confirmed(List.of(item(downloads))), List.of(item(downloads)));

            assertTrue(repository.findById(downloads.id()).isPresent());
            assertEquals(1, cards(view).size());
        });
    }

    @Test
    void aSchedulerFailureReloadsTheListAndShowsTheError() {
        Helper downloads = saved("Downloads");
        Helper photos = saved("Fotos");
        JavaFx.run(() -> {
            HelperListController controller = controller();
            Parent view = mount(controller);
            cards(view).get(0).selectedProperty().set(true);
            cards(view).get(1).selectedProperty().set(true);
            List<HelperItem> chosen = List.of(item(downloads), item(photos));
            scheduler.failNextUnregisterWith(new IllegalStateException("schtasks falhou"));

            controller.afterRemoveConfirmation(confirmed(chosen), chosen);

            assertEquals(1, shownErrors.size());
            assertEquals(2, cards(view).size());
            assertTrue(repository.findById(downloads.id()).isPresent());
            assertTrue(cards(view).stream().noneMatch(card -> card.selectedProperty().get()));
        });
    }

    @Test
    void theHistoryOfARemovedHelperStays() {
        Helper downloads = saved("Downloads");
        ExecutionRecord record = new ExecutionRecord(downloads.id(), "Downloads", CLOCK.instant(), List.of());
        history.add(record);
        JavaFx.run(() -> {
            HelperListController controller = controller();
            mount(controller);

            controller.afterRemoveConfirmation(confirmed(List.of(item(downloads))), List.of(item(downloads)));

            assertTrue(repository.findById(downloads.id()).isEmpty());
            assertEquals(1, history.findByHelper(downloads.id()).size());
            assertSame(record, history.findByHelper(downloads.id()).get(0));
        });
    }
}
