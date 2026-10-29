package com.tidyfy.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.DesktopArrangerHelper;
import com.tidyfy.domain.DesktopGrid;
import com.tidyfy.domain.DesktopIcon;
import com.tidyfy.domain.DesktopLayout;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.IconPosition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes de integração do Organizador da área de trabalho, com a área de trabalho em memória
 * ({@code FakeDesktopIcons}) e a configuração e o histórico em JSON no disco. Não toca a área de trabalho real.
 */
class DesktopArrangerIntegrationTest {

    @TempDir
    Path tempDir;

    private IntegrationStack stack;

    @BeforeEach
    void setUp() {
        stack = new IntegrationStack(tempDir.resolve("storage"));
        stack.desktopIcons()
                .with(new DesktopIcon("Zebra.txt", new IconPosition(700, 500)))
                .with(new DesktopIcon("abacate.txt", new IconPosition(333, 111)));
    }

    @Test
    void repositionsTheIconsAndRecordsArrangedInTheHistory() {
        DesktopArrangerHelper helper = stack.saveDesktopArranger(DesktopLayout.LEFT);

        ExecutionRecord record = stack.run(helper).orElseThrow();

        assertEquals(List.of(new HelperAction(ActionType.ARRANGED, null, null, "2 ícones")), record.actions());
        assertEquals(List.of("abacate.txt", "Zebra.txt"),
                stack.desktopIcons().moved().stream().map(DesktopIcon::name).toList());
        assertEquals(new IconPosition(0, 0), stack.desktopIcons().moved().get(0).position());
        assertEquals(new IconPosition(0, 100), stack.desktopIcons().moved().get(1).position());
        assertEquals(record, stack.listHistory().execute(helper.id()).get(0));
    }

    @Test
    void helperSavedInHelpersFileIsLoadedBackAndExecuted() {
        DesktopArrangerHelper helper = stack.saveDesktopArranger(DesktopLayout.TOP);

        stack.run(helper);

        assertEquals(new IconPosition(0, 0), stack.desktopIcons().moved().get(0).position());
        assertEquals(new IconPosition(100, 0), stack.desktopIcons().moved().get(1).position());
    }

    @Test
    void iconsThatDoNotFitAreRecordedAsFailure() {
        stack.desktopIcons().withGrid(new DesktopGrid(0, 0, 100, 100, 100, 100));
        DesktopArrangerHelper helper = stack.saveDesktopArranger(DesktopLayout.LEFT);

        ExecutionRecord record = stack.run(helper).orElseThrow();

        assertEquals(List.of(ActionType.ARRANGED, ActionType.FAILED),
                record.actions().stream().map(HelperAction::type).toList());
    }

    @Test
    void portFailureIsRecordedAsFailed() {
        DesktopArrangerHelper helper = stack.saveDesktopArranger(DesktopLayout.BORDERS);
        stack.desktopIcons().failNextWith(new UncheckedIOException("sem sessão gráfica", new IOException("x")));

        ExecutionRecord record = stack.run(helper).orElseThrow();

        assertEquals(List.of(new HelperAction(ActionType.FAILED, null, null, "sem sessão gráfica")),
                record.actions());
    }
}
