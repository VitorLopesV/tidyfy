package com.tidyfy.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.application.fake.FakeTaskScheduler;
import com.tidyfy.application.usecase.SaveHelperUseCase;
import com.tidyfy.domain.DesktopArrangerHelper;
import com.tidyfy.domain.DesktopLayout;
import com.tidyfy.domain.HelperRules;
import com.tidyfy.domain.ProtectedFolders;
import com.tidyfy.presentation.model.HelperItem;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import org.junit.jupiter.api.Test;

/**
 * Testa o formulário do Organizador da área de trabalho.
 */
class DesktopArrangerFormControllerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZoneOffset.UTC);

    private static final ProtectedFolders PROTECTED = ProtectedFolders.forWindows(
            Path.of("C:\\Windows"), Path.of("C:\\Program Files"), Path.of("C:\\Program Files (x86)"),
            Path.of("C:\\ProgramData"), Path.of("C:\\Users\\nome"));

    private final FakeHelperRepository repository = new FakeHelperRepository();

    private final FakeTaskScheduler scheduler = new FakeTaskScheduler();

    private final SaveHelperUseCase saveHelper =
            new SaveHelperUseCase(repository, scheduler, new HelperRules(PROTECTED));

    /** Formulário que não tenta fechar a janela, que não existe nos testes. */
    private final class QuietForm extends DesktopArrangerFormController {
        QuietForm(HelperItem editing) {
            super(editing, saveHelper, CLOCK);
        }

        @Override
        protected void onClose() {
        }
    }

    @Test
    void showsOneOptionPerLayoutAndStartsOnTheLeft() {
        JavaFx.run(() -> {
            DesktopArrangerFormController form = new DesktopArrangerFormController(null);
            Parent view = JavaFx.mount(form.createView());

            assertEquals(DesktopLayout.values().length, options(view).getChildren().size());
            assertEquals(DesktopLayout.LEFT, form.selectedLayout());
            assertEquals("Esquerda", ((Label) view.lookup("#previewName")).getText());
        });
    }

    @Test
    void choosingAnOptionChangesTheLayoutAndThePreview() {
        JavaFx.run(() -> {
            DesktopArrangerFormController form = new DesktopArrangerFormController(null);
            Parent view = JavaFx.mount(form.createView());

            option(view, DesktopLayout.BORDERS).fire();

            assertEquals(DesktopLayout.BORDERS, form.selectedLayout());
            assertEquals("Bordas", ((Label) view.lookup("#previewName")).getText());
        });
    }

    @Test
    void selectedOptionCannotBeTurnedOff() {
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(new DesktopArrangerFormController(null).createView());

            option(view, DesktopLayout.LEFT).fire();

            assertTrue(option(view, DesktopLayout.LEFT).isSelected());
        });
    }

    @Test
    void editingSelectsTheHelperLayout() {
        JavaFx.run(() -> {
            DesktopArrangerFormController form =
                    new DesktopArrangerFormController(SampleHelpers.desktopArranger("d1", "Arrumar"));
            Parent view = JavaFx.mount(form.createView());

            assertEquals(DesktopLayout.BORDERS, form.selectedLayout());
            assertEquals("Arrumar", ((TextField) view.lookup("#nameField")).getText());
            assertEquals("Editar ajudante", ((Label) view.lookup("#titleLabel")).getText());
        });
    }

    @Test
    void savingNeedsOnlyAName() {
        JavaFx.run(() -> {
            Parent view = JavaFx.mount(new DesktopArrangerFormController(null).createView());
            assertTrue(save(view).isDisabled());

            ((TextField) view.lookup("#nameField")).setText("Arrumar");

            assertFalse(save(view).isDisabled());
        });
    }

    @Test
    void savingCreatesTheHelperWithTheChosenLayout() {
        JavaFx.run(() -> {
            QuietForm form = new QuietForm(null);
            Parent view = JavaFx.mount(form.createView());
            ((TextField) view.lookup("#nameField")).setText("Arrumar");
            option(view, DesktopLayout.TOP).fire();

            save(view).fire();

            assertTrue(form.saved());
            DesktopArrangerHelper saved = assertInstanceOf(DesktopArrangerHelper.class, repository.findAll().get(0));
            assertEquals(DesktopLayout.TOP, saved.layout());
            assertEquals(saved, scheduler.registeredHelper(saved.id()));
        });
    }

    @Test
    void secondDesktopArrangerShowsTheRuleErrorInTheFooter() {
        JavaFx.run(() -> {
            QuietForm first = new QuietForm(null);
            Parent firstView = JavaFx.mount(first.createView());
            ((TextField) firstView.lookup("#nameField")).setText("Primeiro");
            save(firstView).fire();

            QuietForm second = new QuietForm(null);
            Parent secondView = JavaFx.mount(second.createView());
            ((TextField) secondView.lookup("#nameField")).setText("Segundo");
            save(secondView).fire();

            assertFalse(second.saved());
            assertFalse(((Label) secondView.lookup("#saveError")).getText().isBlank());
            assertEquals(1, repository.findAll().size());
        });
    }

    private static FlowPane options(Parent view) {
        return (FlowPane) view.lookup("#layoutOptions");
    }

    private static ToggleButton option(Parent view, DesktopLayout layout) {
        return options(view).getChildren().stream().map(node -> (ToggleButton) node)
                .filter(button -> button.getUserData() == layout).findFirst().orElseThrow();
    }

    private static Button save(Parent view) {
        return (Button) view.lookup("#saveButton");
    }
}
