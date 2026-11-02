package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeDesktopIcons;
import com.tidyfy.application.fake.FakeExecutionHistoryRepository;
import com.tidyfy.application.fake.FakeFileSystem;
import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.application.fake.FakeRecycleBin;
import com.tidyfy.application.fake.FakeTaskScheduler;
import com.tidyfy.application.usecase.ArrangeDesktopUseCase;
import com.tidyfy.application.usecase.CleanupUseCase;
import com.tidyfy.application.usecase.ListHelpersUseCase;
import com.tidyfy.application.usecase.OrganizeFolderUseCase;
import com.tidyfy.application.usecase.RunHelperUseCase;
import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.DesktopLayoutPlanner;
import com.tidyfy.domain.FolderOrganizationPlanner;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

/**
 * Testa a ação "Executar agora" da lista: execução fora da thread da interface, indicação de "executando",
 * bloqueio de execução repetida, histórico e aviso de sucesso ou falha.
 */
class HelperListRunTest {

    private static final Instant NOW = Instant.parse("2026-03-10T10:00:00Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final Schedule SCHEDULE =
            new Schedule(3, List.of(LocalTime.of(8, 0)), LocalDate.of(2026, 3, 1));

    private static final Path FOLDER = Path.of("C:\\Users\\nome\\Downloads");

    private final FakeHelperRepository repository = new FakeHelperRepository();

    private final FakeExecutionHistoryRepository history = new FakeExecutionHistoryRepository();

    private final FakeFileSystem fileSystem = new FakeFileSystem();

    private final FakeTaskScheduler scheduler = new FakeTaskScheduler();

    /** Tarefas que iriam para a thread de fundo, executadas só quando o teste mandar. */
    private final Queue<Runnable> pending = new ArrayDeque<>();

    /** Lista que guarda os diálogos em vez de abri-los. */
    private static final class RecordingList extends HelperListController {

        final List<RunResultDialogController> successes = new ArrayList<>();

        final List<ErrorDialogController> errors = new ArrayList<>();

        RecordingList(ListHelpersUseCase useCase) {
            super(useCase, new HelperItemMapper(CLOCK));
        }

        @Override
        void presentRunResult(RunResultDialogController dialog) {
            successes.add(dialog);
        }

        @Override
        void presentError(ErrorDialogController dialog) {
            errors.add(dialog);
        }
    }

    private FolderOrganizerHelper organizer(boolean enabled) {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName("Organizar"), SCHEDULE, enabled, FOLDER);
    }

    private RunHelperUseCase runHelper() {
        return new RunHelperUseCase(repository,
                new OrganizeFolderUseCase(fileSystem, new FolderOrganizationPlanner()),
                new CleanupUseCase(fileSystem, new FakeRecycleBin(), CLOCK),
                new ArrangeDesktopUseCase(new FakeDesktopIcons(), new DesktopLayoutPlanner()), history, CLOCK);
    }

    private RecordingList list() {
        RecordingList list = new RecordingList(new ListHelpersUseCase(repository));
        list.setRunService(runHelper());
        list.setExecutors(pending::add, Runnable::run);
        return list;
    }

    private static HelperCard card(Parent view) {
        return (HelperCard) ((Pane) view.lookup("#cardGrid")).getChildren().get(0);
    }

    private static Button runButton(Parent view) {
        return (Button) card(view).lookup("#runButton");
    }

    private void runPending() {
        while (!pending.isEmpty()) {
            pending.remove().run();
        }
    }

    @Test
    void runningADisabledHelperRecordsHistoryAndShowsSuccessWithoutTouchingTheScheduler() {
        FolderOrganizerHelper off = organizer(false);
        repository.with(off);
        fileSystem.addFolder(FOLDER).addFile(FOLDER.resolve("foto.png"), "x", NOW, NOW);
        RecordingList list = list();
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(list.createView(), 1280, 900);

            runButton(view).fire();
            runPending();

            assertEquals(1, history.all().size());
            assertEquals(ActionType.MOVED, history.all().get(0).actions().get(0).type());
            assertEquals(1, list.successes.size());
            assertTrue(list.errors.isEmpty());
            assertEquals(List.of(off), repository.findAll());
            assertEquals(0, repository.saveCount());
            assertTrue(scheduler.registerCalls().isEmpty());
            assertTrue(scheduler.unregisterCalls().isEmpty());
        });
    }

    @Test
    void cardShowsRunningAndBlocksASecondRunUntilItFinishes() {
        repository.with(organizer(true));
        fileSystem.addFolder(FOLDER);
        RecordingList list = list();
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(list.createView(), 1280, 900);

            runButton(view).fire();

            assertTrue(card(view).isRunning());
            assertTrue(runButton(view).isDisable());
            assertEquals(1, pending.size());

            runPending();

            assertFalse(card(view).isRunning());
            assertFalse(runButton(view).isDisable());
            assertEquals(1, history.all().size());
        });
    }

    @Test
    void secondRequestWhileRunningIsIgnored() {
        repository.with(organizer(true));
        fileSystem.addFolder(FOLDER);
        RecordingList list = list();
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(list.createView(), 1280, 900);

            runButton(view).fire();
            runButton(view).fire();

            assertEquals(1, pending.size());
        });
    }

    @Test
    void runningStateSurvivesTheListBeingRedrawn() {
        repository.with(organizer(true));
        fileSystem.addFolder(FOLDER);
        RecordingList list = list();
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(list.createView(), 1280, 900);
            runButton(view).fire();

            list.reload();

            assertTrue(card(view).isRunning());
            runPending();
            assertFalse(card(view).isRunning());
        });
    }

    @Test
    void failedActionShowsTheErrorDialogWithTheFailureDetails() {
        repository.with(organizer(true));
        RecordingList list = list();
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(list.createView(), 1280, 900);

            runButton(view).fire();
            runPending();

            assertTrue(list.successes.isEmpty());
            assertEquals(1, list.errors.size());
            Parent dialog = list.errors.get(0).createView();
            assertEquals("A execução falhou", ((Label) dialog.lookup("#titleLabel")).getText());
            assertEquals(ActionType.FAILED, history.all().get(0).actions().get(0).type());
            assertFalse(card(view).isRunning());
        });
    }

    @Test
    void unexpectedExceptionShowsTheErrorDialogAndFreesTheCard() {
        repository.with(organizer(true));
        history.failNextAddWith(new UncheckedIOException(new IOException("disco cheio")));
        fileSystem.addFolder(FOLDER);
        RecordingList list = list();
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(list.createView(), 1280, 900);

            runButton(view).fire();
            runPending();

            assertEquals(1, list.errors.size());
            assertTrue(list.successes.isEmpty());
            assertFalse(card(view).isRunning());
        });
    }

    @Test
    void helperRemovedMeanwhileReloadsTheListAndShowsAnError() {
        FolderOrganizerHelper helper = organizer(true);
        repository.with(helper);
        RecordingList list = list();
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(list.createView(), 1280, 900);
            runButton(view).fire();
            repository.delete(helper.id());

            runPending();

            assertEquals(1, list.errors.size());
            assertTrue(((Pane) view.lookup("#cardGrid")).getChildren().isEmpty());
            assertTrue(history.all().isEmpty());
        });
    }

    @Test
    void withoutRunServiceTheButtonDoesNothing() {
        repository.with(organizer(true));
        RecordingList list = new RecordingList(new ListHelpersUseCase(repository));
        list.setExecutors(pending::add, Runnable::run);
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(list.createView(), 1280, 900);

            runButton(view).fire();

            assertTrue(pending.isEmpty());
            assertFalse(card(view).isRunning());
        });
    }

    @Test
    void defaultExecutorsRunOutsideTheInterfaceThreadAndReturnToIt() throws Exception {
        repository.with(organizer(true));
        fileSystem.addFolder(FOLDER);
        RecordingList list = new RecordingList(new ListHelpersUseCase(repository));
        list.setRunService(runHelper());
        JavaFx.run(() -> runButton(JavaFx.mount(list.createView(), 1280, 900)).fire());

        for (int i = 0; i < 100 && list.successes.isEmpty(); i++) {
            Thread.sleep(50);
        }
        JavaFx.run(() -> { });

        assertEquals(1, list.successes.size());
        assertEquals(1, history.all().size());
    }
}
