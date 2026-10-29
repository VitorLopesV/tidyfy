package com.tidyfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeDesktopIcons;
import com.tidyfy.application.fake.FakeExecutionHistoryRepository;
import com.tidyfy.application.fake.FakeFileSystem;
import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.application.fake.FakeRecycleBin;
import com.tidyfy.application.usecase.ArrangeDesktopUseCase;
import com.tidyfy.application.usecase.CleanupUseCase;
import com.tidyfy.application.usecase.OrganizeFolderUseCase;
import com.tidyfy.application.usecase.RunHelperUseCase;
import com.tidyfy.domain.DesktopArrangerHelper;
import com.tidyfy.domain.DesktopLayout;
import com.tidyfy.domain.DesktopLayoutPlanner;
import com.tidyfy.domain.FolderOrganizationPlanner;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import com.tidyfy.infrastructure.logging.ExecutionLog;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes de {@link HeadlessRunner} e do desacoplamento do JavaFX no modo {@code --run}, com fakes das portas
 * e log real em pasta temporária.
 */
class HeadlessRunnerTest {

    /** Instante fixo do relógio. */
    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    /** Pasta dos ajudantes de teste (no sistema de arquivos falso). */
    private static final Path FOLDER = Path.of("C:\\Users\\nome\\Downloads");

    /** Pasta de logs temporária. */
    @TempDir
    Path logs;

    private FakeFileSystem fileSystem;
    private FakeHelperRepository repository;
    private FakeDesktopIcons desktop;
    private FakeExecutionHistoryRepository history;
    private ByteArrayOutputStream errBytes;
    private HeadlessRunner runner;

    /** Monta o executor com fakes, relógio fixo e log real. */
    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        fileSystem = new FakeFileSystem();
        repository = new FakeHelperRepository();
        desktop = new FakeDesktopIcons();
        history = new FakeExecutionHistoryRepository();
        RunHelperUseCase useCase = new RunHelperUseCase(repository,
                new OrganizeFolderUseCase(fileSystem, new FolderOrganizationPlanner()),
                new CleanupUseCase(fileSystem, new FakeRecycleBin(), clock),
                new ArrangeDesktopUseCase(desktop, new DesktopLayoutPlanner()), history, clock);
        errBytes = new ByteArrayOutputStream();
        runner = new HeadlessRunner(() -> useCase, new ExecutionLog(logs, clock),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8));
    }

    private FolderOrganizerHelper organizer(boolean enabled) {
        FolderOrganizerHelper helper = new FolderOrganizerHelper(HelperId.newId(),
                new HelperName("Organizar Downloads"),
                new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1)), enabled, FOLDER);
        repository.with(helper);
        return helper;
    }

    private LaunchArguments run(HelperId id) {
        return new LaunchArguments(LaunchArguments.Mode.RUN, id, null);
    }

    private String logContent() throws IOException {
        try (Stream<Path> files = Files.list(logs)) {
            return files.map(file -> {
                try {
                    return Files.readString(file);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).collect(Collectors.joining());
        }
    }

    private String err() {
        return errBytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void successReturnsZeroAndLogsStartAndFinish() throws IOException {
        FolderOrganizerHelper helper = organizer(true);
        fileSystem.addFolder(FOLDER).addFile(FOLDER.resolve("foto.png"), "x", NOW, NOW);

        int code = runner.run(run(helper.id()));

        assertEquals(0, code);
        assertEquals(1, history.all().size());
        String log = logContent();
        assertTrue(log.contains("Execução iniciada"));
        assertTrue(log.contains("Execução concluída: 1 ação(ões)"));
        assertEquals("", err());
    }

    @Test
    void missingHelperReturnsZeroWithoutHistory() throws IOException {
        int code = runner.run(run(HelperId.newId()));

        assertEquals(0, code);
        assertEquals(List.of(), history.all());
        assertTrue(logContent().contains("Execução concluída: 0 ação(ões)"));
    }

    @Test
    void disabledHelperReturnsZeroWithoutHistory() {
        FolderOrganizerHelper helper = organizer(false);

        assertEquals(0, runner.run(run(helper.id())));
        assertEquals(List.of(), history.all());
    }

    @Test
    void escapingExceptionReturnsFailureAndLogsError() throws IOException {
        FolderOrganizerHelper helper = organizer(true);
        fileSystem.addFolder(FOLDER);
        history.failNextAddWith(new UncheckedIOException(new IOException("disco cheio")));

        int code = runner.run(run(helper.id()));

        assertEquals(1, code);
        String log = logContent();
        assertTrue(log.contains("ERROR"));
        assertTrue(log.contains("Execução falhou"));
        assertTrue(err().contains(helper.id().value().toString()));
    }

    @Test
    void helperThatFailedAndWasRecordedAsFailedReturnsFailure() throws IOException {
        FolderOrganizerHelper helper = organizer(true);

        int code = runner.run(run(helper.id()));

        assertEquals(1, code);
        assertEquals(1, history.all().size());
        assertTrue(logContent().contains("ERROR"));
    }

    @Test
    void setupFailureReturnsFailureAndLogsError() throws IOException {
        HeadlessRunner broken = new HeadlessRunner(() -> {
            throw new IllegalStateException("sem pasta");
        }, new ExecutionLog(logs, Clock.fixed(NOW, ZoneOffset.UTC)),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8));

        assertEquals(1, broken.run(run(HelperId.newId())));
        assertTrue(logContent().contains("sem pasta"));
    }

    @Test
    void invalidArgumentsReturnErrorCodeMessageAndLog() throws IOException {
        int code = runner.run(LaunchArguments.parse(new String[] {"--run"}));

        assertEquals(2, code);
        assertTrue(err().contains("--run <id>"));
        String log = logContent();
        assertTrue(log.contains("ERROR"));
        assertTrue(log.contains("Argumentos inválidos"));
    }

    @Test
    void invalidArgumentsDoNotBuildTheUseCase() {
        HeadlessRunner guarded = new HeadlessRunner(() -> {
            throw new AssertionError("não deveria montar o caso de uso");
        }, new ExecutionLog(logs, Clock.fixed(NOW, ZoneOffset.UTC)),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8));

        assertEquals(2, guarded.run(LaunchArguments.parse(new String[] {"--foo"})));
    }

    /**
     * O ramo {@code --run} não pode carregar o JavaFX. A verificação estática: as classes do caminho
     * {@code --run} (Main, LaunchArguments, HeadlessRunner) não referenciam JavaFX nem a camada de
     * apresentação; só {@code InterfaceLauncher} o faz.
     */
    @Test
    void runPathClassesDoNotReferenceJavaFxOrPresentation() throws IOException {
        for (Class<?> type : List.of(Main.class, LaunchArguments.class, LaunchArguments.Mode.class,
                HeadlessRunner.class)) {
            String bytes = classBytes(type);
            assertFalse(bytes.contains("javafx"), type.getName());
            assertFalse(bytes.contains("presentation"), type.getName());
        }
        assertTrue(classBytes(InterfaceLauncher.class).contains("javafx"));
    }

    private static String classBytes(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (var in = type.getResourceAsStream(resource)) {
            return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }

    @Test
    void exitsWithOneWhenTheDesktopPortFails() {
        DesktopArrangerHelper helper = new DesktopArrangerHelper(HelperId.newId(), new HelperName("Área"),
                new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1)), true, DesktopLayout.LEFT);
        repository.with(helper);
        desktop.failNextWith(new UncheckedIOException("sem sessão gráfica", new IOException("x")));

        assertEquals(1, runner.run(run(helper.id())));
        assertEquals(1, history.all().size());
    }
}
