package com.tidyfy.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeDesktopIcons;
import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.DesktopArrangerHelper;
import com.tidyfy.domain.DesktopGrid;
import com.tidyfy.domain.DesktopIcon;
import com.tidyfy.domain.DesktopLayout;
import com.tidyfy.domain.DesktopLayoutPlanner;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.IconPosition;
import com.tidyfy.domain.Schedule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link ArrangeDesktopUseCase}, com a área de trabalho em memória.
 */
class ArrangeDesktopUseCaseTest {

    private static final Schedule SCHEDULE = new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    private FakeDesktopIcons desktop;

    private ArrangeDesktopUseCase useCase;

    @BeforeEach
    void setUp() {
        desktop = new FakeDesktopIcons();
        useCase = new ArrangeDesktopUseCase(desktop, new DesktopLayoutPlanner());
    }

    private static DesktopArrangerHelper helper(DesktopLayout layout) {
        return new DesktopArrangerHelper(HelperId.newId(), new HelperName("Área"), SCHEDULE, true, layout);
    }

    private static DesktopIcon icon(String name, int x, int y) {
        return new DesktopIcon(name, new IconPosition(x, y));
    }

    @Test
    void movesTheIconsToTheLayoutAndRecordsArranged() {
        desktop.with(icon("b.txt", 555, 333)).with(icon("a.txt", 421, 77));

        List<HelperAction> actions = useCase.execute(helper(DesktopLayout.LEFT));

        assertEquals(1, actions.size());
        assertEquals(ActionType.ARRANGED, actions.get(0).type());
        assertEquals("2 ícones", actions.get(0).message());
        assertEquals(2, desktop.moved().size());
        assertEquals("a.txt", desktop.moved().get(0).name());
        assertEquals(new IconPosition(0, 0), desktop.moved().get(0).position());
    }

    @Test
    void singularMessageForOneIcon() {
        desktop.with(icon("a.txt", 5, 5));

        assertEquals("1 ícone", useCase.execute(helper(DesktopLayout.TOP)).get(0).message());
    }

    @Test
    void emptyDesktopIsArrangedWithZeroIcons() {
        List<HelperAction> actions = useCase.execute(helper(DesktopLayout.BORDERS));

        assertEquals("0 ícones", actions.get(0).message());
        assertTrue(desktop.moved().isEmpty());
    }

    @Test
    void recordsAFailureForIconsThatDidNotFit() {
        desktop.withGrid(new DesktopGrid(0, 0, 100, 100, 100, 100))
                .with(icon("a.txt", 1, 1)).with(icon("b.txt", 2, 2));

        List<HelperAction> actions = useCase.execute(helper(DesktopLayout.LEFT));

        assertEquals(List.of(ActionType.ARRANGED, ActionType.FAILED),
                actions.stream().map(HelperAction::type).toList());
        assertEquals("1 ícone", actions.get(0).message());
        assertTrue(actions.get(1).message().contains("b.txt"));
    }

    @Test
    void propagatesPortFailures() {
        desktop.with(icon("a.txt", 1, 1));
        desktop.failNextWith(new UncheckedIOException("sem sessão", new IOException("x")));

        assertThrows(UncheckedIOException.class, () -> useCase.execute(helper(DesktopLayout.LEFT)));
        assertEquals(0, desktop.moveCalls());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new ArrangeDesktopUseCase(null, new DesktopLayoutPlanner()));
        assertThrows(NullPointerException.class, () -> new ArrangeDesktopUseCase(desktop, null));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }
}
