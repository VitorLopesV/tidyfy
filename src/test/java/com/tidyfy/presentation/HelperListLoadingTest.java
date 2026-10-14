package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.application.usecase.ListHelpersUseCase;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import com.tidyfy.infrastructure.persistence.CorruptedStorageException;
import com.tidyfy.presentation.component.HelperCard;
import com.tidyfy.presentation.model.HelperItemMapper;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testa a carga da lista de ajudantes pelo {@link ListHelpersUseCase}: ao abrir, em {@code reload()} e
 * quando a leitura falha.
 */
class HelperListLoadingTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZoneOffset.UTC);

    private static final Schedule SCHEDULE =
            new Schedule(1, List.of(LocalTime.of(18, 0)), LocalDate.of(2026, 3, 1));

    private FakeHelperRepository repository;
    private final List<Throwable> errors = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repository = new FakeHelperRepository();
        errors.clear();
    }

    private static Helper helper(String name) {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName(name), SCHEDULE, true,
                Path.of("C:\\Users\\ana\\" + name));
    }

    private HelperListController controller() {
        return new HelperListController(new ListHelpersUseCase(repository), new HelperItemMapper(CLOCK), errors::add);
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

    @Test
    void showsTheSavedHelpersWhenTheScreenOpens() {
        repository.with(helper("Downloads")).with(helper("Fotos"));

        JavaFx.run(() -> {
            Parent view = mount(controller());

            assertEquals(2, cards(view).size());
            assertEquals("2", text(view, "#organizerCount"));
            assertEquals("2 de 2", text(view, "#enabledCount"));
            assertFalse(view.lookup("#emptyState").isVisible());
            assertTrue(errors.isEmpty());
        });
    }

    @Test
    void showsTheEmptyStateWhenThereAreNoSavedHelpers() {
        JavaFx.run(() -> {
            Parent view = mount(controller());

            assertTrue(view.lookup("#emptyState").isVisible());
            assertFalse(view.lookup("#listSection").isVisible());
            assertTrue(cards(view).isEmpty());
        });
    }

    @Test
    void reloadShowsAHelperAddedAfterTheScreenOpened() {
        repository.with(helper("Downloads"));
        JavaFx.run(() -> {
            HelperListController controller = controller();
            Parent view = mount(controller);
            assertEquals(1, cards(view).size());

            repository.with(helper("Fotos"));
            controller.reload();

            assertEquals(2, cards(view).size());
            assertEquals("2", text(view, "#filterAllCount"));
        });
    }

    @Test
    void reloadDropsARemovedHelper() {
        Helper downloads = helper("Downloads");
        repository.with(downloads).with(helper("Fotos"));
        JavaFx.run(() -> {
            HelperListController controller = controller();
            Parent view = mount(controller);

            repository.delete(downloads.id());
            controller.reload();

            assertEquals(1, cards(view).size());
        });
    }

    @Test
    void failureToReadShowsAnEmptyListAndNotifiesTheErrorHandler() {
        HelperListController controller = new HelperListController(
                new ListHelpersUseCase(new FailingRepository(new CorruptedStorageException(Path.of("helpers.txt"),
                        new RuntimeException("x")))), new HelperItemMapper(CLOCK), errors::add);

        JavaFx.run(() -> {
            Parent view = mount(controller);

            assertTrue(cards(view).isEmpty());
            assertTrue(view.lookup("#emptyState").isVisible());
            assertEquals(1, errors.size());
            assertTrue(errors.get(0) instanceof CorruptedStorageException);
        });
    }

    @Test
    void ioFailureOnReloadEmptiesTheListWithoutThrowing() {
        repository.with(helper("Downloads"));
        FailingRepository failing = new FailingRepository(new UncheckedIOException(new java.io.IOException("x")));
        failing.delegate = repository;
        failing.failing = false;
        HelperListController controller = new HelperListController(new ListHelpersUseCase(failing),
                new HelperItemMapper(CLOCK), errors::add);

        JavaFx.run(() -> {
            Parent view = mount(controller);
            assertEquals(1, cards(view).size());

            failing.failing = true;
            controller.reload();

            assertTrue(cards(view).isEmpty());
            assertEquals(1, errors.size());
        });
    }

    @Test
    void countersAndFiltersWorkWithLoadedData() {
        repository.with(helper("Downloads"));
        JavaFx.run(() -> {
            Parent view = mount(controller());

            ((ToggleButton) view.lookup("#filterCleaner")).setSelected(true);
            assertTrue(cards(view).isEmpty());
            assertTrue(view.lookup("#noResults").isVisible());

            ((ToggleButton) view.lookup("#filterOrganizer")).setSelected(true);
            assertEquals(1, cards(view).size());
        });
    }

    @Test
    void rejectsNullArguments() {
        ListHelpersUseCase useCase = new ListHelpersUseCase(repository);
        HelperItemMapper mapper = new HelperItemMapper(CLOCK);

        assertThrows(NullPointerException.class, () -> new HelperListController(null, mapper));
        assertThrows(NullPointerException.class, () -> new HelperListController(useCase, null));
        assertThrows(NullPointerException.class, () -> new HelperListController(useCase, mapper, null));
    }

    @Test
    void uiServicesRejectNullComponents() {
        assertThrows(NullPointerException.class, () -> new UiServices(null, null, null, null, CLOCK));
        assertNotNull(CLOCK);
    }

    /** Repositório que lança ao listar, desde o início ou a partir de {@code failing = true}. */
    static final class FailingRepository implements com.tidyfy.application.port.HelperRepository {

        private final RuntimeException failure;
        private FakeHelperRepository delegate;
        private boolean failing;

        FailingRepository(RuntimeException failure) {
            this.failure = failure;
            this.failing = true;
        }

        @Override
        public void save(Helper helper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Optional<Helper> findById(HelperId id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Helper> findAll() {
            if (failing || delegate == null) {
                throw failure;
            }
            return delegate.findAll();
        }

        @Override
        public int countByType(com.tidyfy.domain.HelperType type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(HelperId id) {
            throw new UnsupportedOperationException();
        }
    }
}
