package com.tidyfy.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeExecutionHistoryRepository;
import com.tidyfy.application.fake.FakeFileSystem;
import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.application.fake.FakeRecycleBin;
import com.tidyfy.application.port.RecycleBin;
import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.FolderOrganizationPlanner;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.RecycleBinTarget;
import com.tidyfy.domain.Schedule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
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
 * Testes de {@link RunHelperUseCase}, com casos de uso reais sobre fakes das portas.
 */
class RunHelperUseCaseTest {

    /** Instante fixo do relógio dos testes. */
    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    /** Pasta usada pelos ajudantes de teste. */
    private static final Path FOLDER = Path.of("C:\\Users\\nome\\Downloads");

    /** Frequência qualquer dos ajudantes de teste. */
    private static final Schedule SCHEDULE =
            new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    /** Relógio fixo. */
    private Clock clock;

    /** Sistema de arquivos falso. */
    private FakeFileSystem fileSystem;

    /** Lixeira falsa. */
    private FakeRecycleBin bin;

    /** Repositório de ajudantes falso. */
    private FakeHelperRepository repository;

    /** Histórico falso. */
    private FakeExecutionHistoryRepository history;

    /** Caso de uso sob teste. */
    private RunHelperUseCase useCase;

    /** Monta o caso de uso com fakes e relógio fixo. */
    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        fileSystem = new FakeFileSystem();
        bin = new FakeRecycleBin();
        repository = new FakeHelperRepository();
        history = new FakeExecutionHistoryRepository();
        useCase = new RunHelperUseCase(repository,
                new OrganizeFolderUseCase(fileSystem, new FolderOrganizationPlanner()),
                new CleanupUseCase(fileSystem, bin, clock), history, clock);
    }

    /** Cria um organizador de pasta. */
    private static FolderOrganizerHelper organizer(boolean enabled) {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName("Organizar Downloads"),
                SCHEDULE, enabled, FOLDER);
    }

    /** Cria uma limpeza de pasta. */
    private static CleanerHelper folderCleaner() {
        return new CleanerHelper(HelperId.newId(), new HelperName("Limpar Downloads"), SCHEDULE, true,
                new FolderTarget(FOLDER, AgeReference.CREATED), null);
    }

    /** Cria uma limpeza da lixeira. */
    private static CleanerHelper binCleaner() {
        return new CleanerHelper(HelperId.newId(), new HelperName("Limpar lixeira"), SCHEDULE, true,
                new RecycleBinTarget(), null);
    }

    @Test
    void organizerRunsAndRecordsActions() {
        FolderOrganizerHelper helper = organizer(true);
        repository.with(helper);
        Path photo = FOLDER.resolve("foto.png");
        fileSystem.addFolder(FOLDER).addFile(photo, "x", NOW, NOW);

        useCase.execute(helper.id());

        assertEquals(1, history.all().size());
        assertEquals(List.of(new HelperAction(ActionType.MOVED, photo,
                FOLDER.resolve("Imagens").resolve("foto.png"), null)), history.all().get(0).actions());
    }

    @Test
    void folderCleanerRunsAndRecordsActions() {
        CleanerHelper helper = folderCleaner();
        repository.with(helper);
        Path file = FOLDER.resolve("velho.txt");
        fileSystem.addFolder(FOLDER).addFile(file, "x", NOW, NOW);

        useCase.execute(helper.id());

        assertEquals(List.of(file), fileSystem.trashed());
        assertEquals(List.of(new HelperAction(ActionType.TRASHED, file, null, null)),
                history.all().get(0).actions());
    }

    @Test
    void recycleBinCleanerRunsAndRecordsActions() {
        CleanerHelper helper = binCleaner();
        repository.with(helper);
        bin.addItem(NOW.minus(Duration.ofDays(5)));

        useCase.execute(helper.id());

        assertEquals(1, bin.emptyAllCalls());
        assertEquals(1, history.all().size());
        assertEquals(ActionType.RECYCLE_BIN_EMPTIED, history.all().get(0).actions().get(0).type());
    }

    @Test
    void disabledHelperDoesNothingAndRecordsNothing() {
        FolderOrganizerHelper helper = organizer(false);
        repository.with(helper);
        fileSystem.addFolder(FOLDER).addFile(FOLDER.resolve("foto.png"), "x", NOW, NOW);

        useCase.execute(helper.id());

        assertEquals(List.of(), history.all());
        assertEquals(List.of(), fileSystem.moves());
    }

    @Test
    void missingHelperDoesNothingAndRecordsNothing() {
        useCase.execute(HelperId.newId());

        assertEquals(List.of(), history.all());
    }

    @Test
    void failureListingFolderBecomesFailedActionWithoutPropagating() {
        FolderOrganizerHelper helper = organizer(true);
        repository.with(helper);

        useCase.execute(helper.id());

        List<HelperAction> actions = history.all().get(0).actions();
        assertEquals(1, actions.size());
        assertEquals(ActionType.FAILED, actions.get(0).type());
        assertNull(actions.get(0).source());
        assertNull(actions.get(0).target());
        assertTrue(actions.get(0).message().contains(FOLDER.toString()));
    }

    @Test
    void failureWithoutMessageUsesExceptionName() {
        CleanerHelper helper = binCleaner();
        repository.with(helper);
        RecycleBin broken = new RecycleBin() {
            @Override
            public int emptyAll() {
                throw new IllegalStateException();
            }

            @Override
            public int emptyOlderThan(Instant cutoff) {
                throw new IllegalStateException();
            }
        };
        RunHelperUseCase failing = new RunHelperUseCase(repository,
                new OrganizeFolderUseCase(fileSystem, new FolderOrganizationPlanner()),
                new CleanupUseCase(fileSystem, broken, clock), history, clock);

        failing.execute(helper.id());

        assertEquals(List.of(new HelperAction(ActionType.FAILED, null, null, "IllegalStateException")),
                history.all().get(0).actions());
    }

    @Test
    void recordHasHelperIdNameAndClockInstant() {
        FolderOrganizerHelper helper = organizer(true);
        repository.with(helper);
        fileSystem.addFolder(FOLDER);

        useCase.execute(helper.id());

        ExecutionRecord record = history.all().get(0);
        assertEquals(helper.id(), record.helperId());
        assertEquals("Organizar Downloads", record.helperName());
        assertEquals(NOW, record.executedAt());
    }

    @Test
    void deletesRecordsOlderThanThirtyDaysAndKeepsRecentOnes() {
        FolderOrganizerHelper helper = organizer(true);
        repository.with(helper);
        fileSystem.addFolder(FOLDER);
        history.add(new ExecutionRecord(helper.id(), "velho", NOW.minus(Duration.ofDays(31)), List.of()));
        history.add(new ExecutionRecord(helper.id(), "recente", NOW.minus(Duration.ofDays(29)), List.of()));

        useCase.execute(helper.id());

        assertEquals(List.of("recente", "Organizar Downloads"),
                history.all().stream().map(ExecutionRecord::helperName).toList());
    }

    @Test
    void historyFailureIsPropagated() {
        FolderOrganizerHelper helper = organizer(true);
        repository.with(helper);
        fileSystem.addFolder(FOLDER);
        history.failNextAddWith(new UncheckedIOException(new IOException("disco cheio")));

        assertThrows(UncheckedIOException.class, () -> useCase.execute(helper.id()));
    }

    @Test
    void executeReturnsTheRecordThatWasSaved() {
        FolderOrganizerHelper helper = organizer(true);
        repository.with(helper);
        fileSystem.addFolder(FOLDER);

        ExecutionRecord returned = useCase.execute(helper.id()).orElseThrow();

        assertEquals(history.all().get(0), returned);
    }

    @Test
    void executeReturnsEmptyForDisabledOrMissingHelper() {
        FolderOrganizerHelper helper = organizer(false);
        repository.with(helper);

        assertTrue(useCase.execute(helper.id()).isEmpty());
        assertTrue(useCase.execute(HelperId.newId()).isEmpty());
    }

    @Test
    void nullIdIsRejected() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }
}
