package com.tidyfy.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tidyfy.application.fake.FakeFileSystem;
import com.tidyfy.application.fake.FakeRecycleBin;
import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.AgeLimit;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.RecycleBinTarget;
import com.tidyfy.domain.Schedule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testes da limpeza da lixeira feita pelo {@link CleanupUseCase}.
 */
class CleanupUseCaseRecycleBinTest {

    /** Instante fixo do relógio dos testes. */
    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    /** Frequência qualquer dos ajudantes de teste. */
    private static final Schedule SCHEDULE =
        new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    /** Lixeira falsa. */
    private FakeRecycleBin bin;

    /** Caso de uso sob teste. */
    private CleanupUseCase useCase;

    /** Cria o caso de uso com fakes e relógio fixo. */
    @BeforeEach
    void setUp() {
        bin = new FakeRecycleBin();
        useCase = new CleanupUseCase(new FakeFileSystem(), bin, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /** Cria um ajudante de limpeza da lixeira com o limite informado (pode ser nulo). */
    private static CleanerHelper helper(AgeLimit limit) {
        return new CleanerHelper(HelperId.newId(), new HelperName("Lixeira"), SCHEDULE, true,
            new RecycleBinTarget(), limit);
    }

    @Test
    void withoutAgeLimitEmptiesEverything() {
        bin.addItem(NOW.minus(Duration.ofDays(1))).addItem(NOW.minus(Duration.ofDays(100)));

        useCase.execute(helper(null));

        assertEquals(1, bin.emptyAllCalls());
        assertEquals(0, bin.size());
        assertEquals(List.of(), bin.cutoffs());
    }

    @Test
    void withAgeLimitCallsEmptyOlderThanWithCorrectCutoff() {
        useCase.execute(helper(new AgeLimit(30)));

        assertEquals(List.of(NOW.minus(Duration.ofDays(30))), bin.cutoffs());
        assertEquals(0, bin.emptyAllCalls());
    }

    @Test
    void withAgeLimitRemovesOnlyOlderItems() {
        bin.addItem(NOW.minus(Duration.ofDays(40))).addItem(NOW.minus(Duration.ofDays(31)))
            .addItem(NOW.minus(Duration.ofDays(29))).addItem(NOW);

        useCase.execute(helper(new AgeLimit(30)));

        assertEquals(2, bin.size());
        assertEquals(2, bin.removedCount());
    }

    @Test
    void itemExactlyAtTheLimitIsKept() {
        bin.addItem(NOW.minus(Duration.ofDays(30)));

        useCase.execute(helper(new AgeLimit(30)));

        assertEquals(1, bin.size());
    }

    @Test
    void recordsRemovedQuantity() {
        bin.addItem(NOW).addItem(NOW).addItem(NOW);

        List<HelperAction> actions = useCase.execute(helper(null));

        assertEquals(1, actions.size());
        HelperAction action = actions.get(0);
        assertEquals(ActionType.RECYCLE_BIN_EMPTIED, action.type());
        assertEquals("3 itens removidos", action.message());
        assertNull(action.source());
        assertNull(action.target());
    }

    @Test
    void emptyBinRecordsZeroRemoved() {
        List<HelperAction> actions = useCase.execute(helper(null));

        assertEquals(1, actions.size());
        assertEquals(ActionType.RECYCLE_BIN_EMPTIED, actions.get(0).type());
        assertEquals("0 itens removidos", actions.get(0).message());
    }

    @Test
    void failureWithoutAgeLimitBecomesFailedAction() {
        bin.addItem(NOW);
        bin.failNextWith(new UncheckedIOException("sem permissão", new IOException("x")));

        List<HelperAction> actions = useCase.execute(helper(null));

        assertEquals(1, actions.size());
        assertEquals(ActionType.FAILED, actions.get(0).type());
        assertEquals("sem permissão", actions.get(0).message());
        assertEquals(1, bin.size());
    }

    @Test
    void failureWithAgeLimitBecomesFailedAction() {
        bin.failNextWith(new UncheckedIOException("sem permissão", new IOException("x")));

        List<HelperAction> actions = useCase.execute(helper(new AgeLimit(5)));

        assertEquals(List.of(ActionType.FAILED), actions.stream().map(HelperAction::type).toList());
    }

    @Test
    void usesInjectedClockForCutoff() {
        Instant other = Instant.parse("2030-01-10T00:00:00Z");
        CleanupUseCase later = new CleanupUseCase(new FakeFileSystem(), bin, Clock.fixed(other, ZoneOffset.UTC));

        later.execute(helper(new AgeLimit(10)));

        assertEquals(List.of(Instant.parse("2029-12-31T00:00:00Z")), bin.cutoffs());
    }

    @Test
    void rejectsNullHelper() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }
}
