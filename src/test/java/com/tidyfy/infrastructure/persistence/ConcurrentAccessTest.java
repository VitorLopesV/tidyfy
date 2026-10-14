package com.tidyfy.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes de acesso simultâneo aos arquivos JSON: threads de um processo, várias instâncias sobre a mesma
 * pasta, a espera limitada pela trava e processos JVM de verdade.
 */
class ConcurrentAccessTest {

    /** Quantidade de threads dos testes de concorrência */
    private static final int THREADS = 8;

    /** Gravações por thread */
    private static final int PER_THREAD = 5;

    /** Gravações por processo filho no teste com processos */
    private static final int PER_PROCESS = 15;

    /** Instante fixo dos registros de histórico */
    private static final Instant NOW = Instant.parse("2026-10-14T15:30:45Z");

    /** Pasta temporária do teste */
    @TempDir
    Path tempDir;

    /**
     * Cria um ajudante com o nome informado.
     */
    private static Helper organizer(String name) {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName(name),
                new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 10, 14)), true,
                Path.of("C:\\Users\\ana\\" + name));
    }

    /**
     * Cria um registro de histórico com o nome informado.
     */
    private static ExecutionRecord record(String name) {
        return new ExecutionRecord(HelperId.newId(), name, NOW,
                List.of(new HelperAction(ActionType.MOVED, Path.of("C:\\a.txt"), Path.of("C:\\Docs\\a.txt"), null)));
    }

    /**
     * Executa a tarefa em {@code THREADS} threads que largam juntas, cada uma com {@code PER_THREAD} chamadas.
     */
    private static void runConcurrently(IntConsumer task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                int thread = t;
                futures.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < PER_THREAD; i++) {
                        task.accept(thread * PER_THREAD + i);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Threads gravando ajudantes diferentes no mesmo repositório não perdem nenhum.
     */
    @Test
    void concurrentSavesOnSameRepositoryLoseNothing() throws Exception {
        JsonHelperRepository repository = new JsonHelperRepository(new StorageLocation(tempDir.resolve("t")),
                new HelperJsonMapper());

        runConcurrently(n -> repository.save(organizer("H" + n)));

        assertEquals(THREADS * PER_THREAD, repository.findAll().size());
    }

    /**
     * Instâncias diferentes sobre a mesma pasta (como a interface e a execução agendada) não perdem gravações.
     */
    @Test
    void concurrentSavesOnSeparateInstancesLoseNothing() throws Exception {
        StorageLocation location = new StorageLocation(tempDir.resolve("t"));

        runConcurrently(n -> new JsonHelperRepository(location, new HelperJsonMapper()).save(organizer("H" + n)));

        assertEquals(THREADS * PER_THREAD,
                new JsonHelperRepository(location, new HelperJsonMapper()).findAll().size());
    }

    /**
     * Threads acrescentando ao histórico (instâncias diferentes) não perdem nenhum registro.
     */
    @Test
    void concurrentHistoryAddsLoseNothing() throws Exception {
        StorageLocation location = new StorageLocation(tempDir.resolve("t"));

        runConcurrently(n -> new JsonExecutionHistoryRepository(location, new ExecutionHistoryJsonMapper())
                .add(record("R" + n)));

        JsonExecutionHistoryRepository repository =
                new JsonExecutionHistoryRepository(location, new ExecutionHistoryJsonMapper());
        // O arquivo é JSON válido (a leitura não falha); contamos os registros pelo nome do ajudante
        String content = Files.readString(location.historyFile());
        for (int n = 0; n < THREADS * PER_THREAD; n++) {
            assertTrue(content.contains("\"R" + n + "\""), "faltou o registro R" + n);
        }
        repository.deleteExpired(NOW);
    }

    /**
     * Sem concorrência o comportamento é o de antes: grava, relê e o arquivo de trava fica ao lado.
     */
    @Test
    void singleThreadBehaviorIsUnchanged() throws IOException {
        StorageLocation location = new StorageLocation(tempDir.resolve("t"));
        JsonHelperRepository repository = new JsonHelperRepository(location, new HelperJsonMapper());
        Helper helper = organizer("A");

        repository.save(helper);
        repository.save(helper);

        assertEquals(List.of(helper), repository.findAll());
        repository.delete(helper.id());
        assertTrue(repository.findAll().isEmpty());
        assertTrue(Files.exists(location.directory().resolve("helpers.txt.lock")));
    }

    /**
     * Com outro detentor segurando a trava, a gravação espera o limite e falha com mensagem clara;
     * depois que a trava é solta, funciona.
     */
    @Test
    void saveFailsClearlyWhenLockIsHeldLongerThanTimeout() throws IOException {
        StorageLocation location = new StorageLocation(tempDir.resolve("t"));
        JsonHelperRepository repository = new JsonHelperRepository(location, new HelperJsonMapper(),
                Clock.systemDefaultZone(), Duration.ofMillis(150));
        Path lockFile = location.directory().resolve("helpers.txt.lock");

        try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock ignored = channel.lock()) {
            UncheckedIOException error = assertThrows(UncheckedIOException.class,
                    () -> repository.save(organizer("A")));
            assertTrue(error.getMessage().contains("acesso exclusivo"), error.getMessage());
            assertTrue(error.getMessage().contains("helpers.txt"), error.getMessage());
        }

        repository.save(organizer("B"));
        assertEquals(1, repository.findAll().size());
    }

    /**
     * O mesmo vale para o histórico.
     */
    @Test
    void historyAddFailsClearlyWhenLockIsHeldLongerThanTimeout() throws IOException {
        StorageLocation location = new StorageLocation(tempDir.resolve("t"));
        JsonExecutionHistoryRepository repository = new JsonExecutionHistoryRepository(location,
                new ExecutionHistoryJsonMapper(), Clock.systemDefaultZone(), Duration.ofMillis(150));
        Path lockFile = location.directory().resolve("history.txt.lock");

        try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock ignored = channel.lock()) {
            UncheckedIOException error = assertThrows(UncheckedIOException.class,
                    () -> repository.add(record("R")));
            assertTrue(error.getMessage().contains("history.txt"), error.getMessage());
        }
    }

    /**
     * Dois processos JVM de verdade e o processo do teste gravando ao mesmo tempo na mesma pasta:
     * nenhuma gravação se perde.
     */
    @Test
    void separateJvmProcessesDoNotLoseSaves() throws Exception {
        Path folder = tempDir.resolve("t");
        new StorageLocation(folder).directory();
        List<Process> processes = new ArrayList<>();
        for (int p = 0; p < 2; p++) {
            processes.add(new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", System.getProperty("java.class.path"), ConcurrentAccessTest.class.getName(),
                    folder.toString(), "P" + p, String.valueOf(PER_PROCESS))
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start());
        }
        writeHelpers(folder, "Own", PER_PROCESS);
        for (Process process : processes) {
            assertEquals(0, process.waitFor());
        }

        assertEquals(3 * PER_PROCESS,
                new JsonHelperRepository(new StorageLocation(folder), new HelperJsonMapper()).findAll().size());
    }

    /**
     * Ponto de entrada dos processos filhos do teste com processos.
     *
     * @param args pasta, prefixo dos nomes e quantidade de gravações
     */
    public static void main(String[] args) {
        writeHelpers(Path.of(args[0]), args[1], Integer.parseInt(args[2]));
    }

    /**
     * Grava ajudantes distintos, um a um, com uma instância própria do repositório.
     */
    private static void writeHelpers(Path folder, String prefix, int count) {
        JsonHelperRepository repository = new JsonHelperRepository(new StorageLocation(folder),
                new HelperJsonMapper());
        for (int i = 0; i < count; i++) {
            repository.save(organizer(prefix + i));
        }
    }
}
