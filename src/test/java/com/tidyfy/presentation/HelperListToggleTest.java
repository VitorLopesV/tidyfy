package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.application.fake.FakeTaskScheduler;
import com.tidyfy.application.usecase.ListHelpersUseCase;
import com.tidyfy.application.usecase.SaveHelperUseCase;
import com.tidyfy.domain.AgeLimit;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.HelperRules;
import com.tidyfy.domain.ProtectedFolders;
import com.tidyfy.domain.Schedule;
import com.tidyfy.presentation.component.HelperCard;
import com.tidyfy.presentation.model.HelperItemMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

/**
 * Testa o interruptor do cartão: ligar e desligar salvam pelo {@link SaveHelperUseCase} (que registra ou
 * remove a tarefa do agendador) e, em caso de erro, o interruptor volta ao estado anterior.
 */
class HelperListToggleTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZoneOffset.UTC);

    private static final Schedule SCHEDULE =
            new Schedule(3, List.of(LocalTime.of(8, 0), LocalTime.of(18, 0)), LocalDate.of(2026, 3, 1));

    private static final ProtectedFolders PROTECTED = ProtectedFolders.forWindows(
            Path.of("C:\\Windows"), Path.of("C:\\Program Files"), Path.of("C:\\Program Files (x86)"),
            Path.of("C:\\ProgramData"), Path.of("C:\\Users\\nome"));

    private final FakeHelperRepository repository = new FakeHelperRepository();

    private final FakeTaskScheduler scheduler = new FakeTaskScheduler();

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

    private static FolderOrganizerHelper organizer(boolean enabled) {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName("Organizar"), SCHEDULE, enabled,
                Path.of("C:\\Users\\nome\\Downloads"));
    }

    private static CleanerHelper cleaner(boolean enabled) {
        return new CleanerHelper(HelperId.newId(), new HelperName("Limpar"), SCHEDULE, enabled,
                new FolderTarget(Path.of("C:\\Users\\nome\\Temp"), AgeReference.CREATED), new AgeLimit(15));
    }

    private RecordingList list() {
        RecordingList list = new RecordingList(new ListHelpersUseCase(repository));
        list.setSaveServices(new SaveHelperUseCase(repository, scheduler, new HelperRules(PROTECTED)), CLOCK);
        return list;
    }

    private static List<HelperCard> cards(Parent view) {
        return ((Pane) view.lookup("#cardGrid")).getChildren().stream().map(HelperCard.class::cast).toList();
    }

    private static String text(Parent view, String selector) {
        return ((Label) view.lookup(selector)).getText();
    }

    @Test
    void turningAnOrganizerOnSavesItAndRegistersTheTask() {
        Helper off = organizer(false);
        repository.with(off);
        RecordingList list = list();
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(list.createView(), 1280, 900);
            assertEquals("0 de 1", text(view, "#enabledCount"));

            cards(view).get(0).enabledProperty().set(true);

            assertEquals(off.withEnabled(true), repository.findAll().get(0));
            assertEquals(off.withEnabled(true), scheduler.registeredHelper(off.id()));
            assertEquals("1 de 1", text(view, "#enabledCount"));
            assertTrue(cards(view).get(0).enabledProperty().get());
            assertTrue(list.presented.isEmpty());
        });
    }

    @Test
    void turningACleanerOffSavesItAndUnregistersTheTask() {
        Helper on = cleaner(true);
        repository.with(on);
        scheduler.register(on);
        RecordingList list = list();
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(list.createView(), 1280, 900);

            cards(view).get(0).enabledProperty().set(false);

            assertEquals(on.withEnabled(false), repository.findAll().get(0));
            assertFalse(scheduler.isRegistered(on.id()));
            assertEquals(List.of(on.id()), scheduler.unregisterCalls());
            assertEquals("0 de 1", text(view, "#enabledCount"));
            assertFalse(cards(view).get(0).enabledProperty().get());
        });
    }

    @Test
    void turningACleanerOnKeepsTheScheduleAndTheCleaningSettings() {
        CleanerHelper off = cleaner(false);
        repository.with(off);
        RecordingList list = list();
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(list.createView(), 1280, 900);

            cards(view).get(0).enabledProperty().set(true);

            assertEquals(off.withEnabled(true), repository.findAll().get(0));
            assertEquals(SCHEDULE, repository.findAll().get(0).schedule());
        });
    }

    @Test
    void schedulerFailureRevertsTheSwitchSavesNothingAndShowsTheError() {
        Helper off = organizer(false);
        repository.with(off);
        scheduler.failNextRegisterWith(new UncheckedIOException(new IOException("schtasks falhou")));
        RecordingList list = list();
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(list.createView(), 1280, 900);
            int saves = repository.saveCount();

            cards(view).get(0).enabledProperty().set(true);

            assertFalse(cards(view).get(0).enabledProperty().get());
            assertEquals(off, repository.findAll().get(0));
            assertEquals(saves, repository.saveCount());
            assertFalse(scheduler.isRegistered(off.id()));
            assertEquals("0 de 1", text(view, "#enabledCount"));
        });
        JavaFx.run(() -> { });
        assertEquals(1, list.presented.size());
        JavaFx.run(() -> {
            Parent dialog = list.presented.get(0).createView();
            assertEquals("Não foi possível acessar os arquivos", ((Label) dialog.lookup("#titleLabel")).getText());
        });
    }

    @Test
    void revertingTheSwitchDoesNotSaveAgain() {
        Helper on = organizer(true);
        repository.with(on);
        scheduler.register(on);
        RecordingList list = list();
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(list.createView(), 1280, 900);
            repository.failNextSaveWith(new IllegalStateException("disco cheio"));

            cards(view).get(0).enabledProperty().set(false);

            assertTrue(cards(view).get(0).enabledProperty().get());
            assertEquals(0, repository.saveCount());
            assertEquals(1, scheduler.unregisterCalls().size());
            assertEquals(on, repository.findAll().get(0));
        });
    }

    @Test
    void withoutSaveServicesTheSwitchOnlyChangesTheScreen() {
        repository.with(organizer(false));
        RecordingList list = new RecordingList(new ListHelpersUseCase(repository));
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(list.createView(), 1280, 900);

            cards(view).get(0).enabledProperty().set(true);

            assertEquals("1 de 1", text(view, "#enabledCount"));
            assertFalse(repository.findAll().get(0).enabled());
            assertTrue(scheduler.registered().isEmpty());
        });
    }
}
