package com.tidyfy.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.HelperId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes do {@link JsonExecutionHistoryRepository}.
 */
class JsonExecutionHistoryRepositoryTest {

    /** Pasta temporária do teste */
    @TempDir
    Path tempDir;

    /** Instante de referência */
    private final Instant now = Instant.parse("2026-10-14T15:30:45Z");

    /** Relógio fixo no instante de referência */
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    /** Arquivo de histórico */
    private Path file;

    /** Repositório testado */
    private JsonExecutionHistoryRepository repository;

    /**
     * Prepara a pasta e o repositório.
     */
    @BeforeEach
    void setUp() {
        StorageLocation location = new StorageLocation(tempDir.resolve("tidyfy"));
        file = location.historyFile();
        repository = new JsonExecutionHistoryRepository(location, new ExecutionHistoryJsonMapper(), clock);
    }

    private ExecutionRecord record(HelperId id, Instant at) {
        return new ExecutionRecord(id, "Ajudante", at,
                List.of(new HelperAction(ActionType.MOVED, Path.of("C:\\a.txt"), Path.of("C:\\Docs\\a.txt"), null)));
    }

    private List<String> names() throws IOException {
        try (Stream<Path> files = Files.list(file.getParent())) {
            return files.map(path -> path.getFileName().toString()).sorted().toList();
        }
    }

    /**
     * Sem arquivo, o histórico está vazio e nada é criado.
     */
    @Test
    void missingFileMeansEmptyHistory() {
        assertTrue(repository.findByHelper(HelperId.newId()).isEmpty());
        repository.deleteExpired(now);
        assertFalse(Files.exists(file));
    }

    /**
     * Arquivo vazio ou em branco é histórico vazio.
     */
    @Test
    void emptyFileMeansEmptyHistory() throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "  \n");

        assertTrue(repository.findByHelper(HelperId.newId()).isEmpty());
    }

    /**
     * Registros adicionados são encontrados pelo ajudante, filtrados e do mais recente para o mais antigo.
     */
    @Test
    void findByHelperFiltersAndSortsNewestFirst() {
        HelperId a = HelperId.newId();
        HelperId b = HelperId.newId();
        ExecutionRecord old = record(a, now.minus(Duration.ofDays(3)));
        ExecutionRecord recent = record(a, now.minus(Duration.ofHours(1)));
        ExecutionRecord middle = record(a, now.minus(Duration.ofDays(1)));
        ExecutionRecord other = record(b, now);
        repository.add(old);
        repository.add(recent);
        repository.add(other);
        repository.add(middle);

        assertEquals(List.of(recent, middle, old), repository.findByHelper(a));
        assertEquals(List.of(other), repository.findByHelper(b));
        assertTrue(repository.findByHelper(HelperId.newId()).isEmpty());
    }

    /**
     * Os registros persistem em um repositório novo (sem cache).
     */
    @Test
    void recordsSurviveNewRepositoryInstance() {
        HelperId id = HelperId.newId();
        ExecutionRecord saved = record(id, now);
        repository.add(saved);

        JsonExecutionHistoryRepository other = new JsonExecutionHistoryRepository(
                new StorageLocation(file.getParent()), new ExecutionHistoryJsonMapper());

        assertEquals(List.of(saved), other.findByHelper(id));
    }

    /**
     * deleteExpired remove só os com mais de 30 dias e preserva o resto.
     */
    @Test
    void deleteExpiredRemovesOnlyRecordsOlderThanThirtyDays() {
        HelperId id = HelperId.newId();
        ExecutionRecord expired = record(id, now.minus(Duration.ofDays(30)).minusSeconds(1));
        ExecutionRecord exactly30 = record(id, now.minus(Duration.ofDays(30)));
        ExecutionRecord recent = record(id, now.minus(Duration.ofDays(2)));
        ExecutionRecord otherExpired = record(HelperId.newId(), now.minus(Duration.ofDays(45)));
        repository.add(expired);
        repository.add(exactly30);
        repository.add(recent);
        repository.add(otherExpired);

        repository.deleteExpired(now);

        assertEquals(List.of(recent, exactly30), repository.findByHelper(id));
        assertTrue(repository.findByHelper(otherExpired.helperId()).isEmpty());
    }

    /**
     * Sem vencidos, deleteExpired não regrava o arquivo.
     */
    @Test
    void deleteExpiredDoesNotRewriteWhenNothingChanged() throws IOException {
        repository.add(record(HelperId.newId(), now));
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.fromMillis(1_000_000L));
        byte[] before = Files.readAllBytes(file);

        repository.deleteExpired(now);

        assertEquals(1_000_000L, Files.getLastModifiedTime(file).toMillis());
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    /**
     * A gravação não deixa arquivos temporários na pasta.
     */
    @Test
    void atomicWriteLeavesNoTemporaryFiles() throws IOException {
        repository.add(record(HelperId.newId(), now));
        repository.add(record(HelperId.newId(), now.minus(Duration.ofDays(40))));
        repository.deleteExpired(now);

        assertEquals(List.of("history.txt"), names());
    }

    /**
     * JSON inválido gera exceção, mantém o original e guarda cópia de segurança.
     */
    @Test
    void corruptedFileThrowsAndKeepsBackup() throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{quebrado", StandardCharsets.UTF_8);

        CorruptedStorageException e = assertThrows(CorruptedStorageException.class,
                () -> repository.findByHelper(HelperId.newId()));

        assertTrue(e.getMessage().contains(file.toString()));
        assertEquals("{quebrado", Files.readString(file));
        assertEquals(List.of("history.txt", "history.txt.bak-20261014-153045"), names());
        assertEquals("{quebrado", Files.readString(file.resolveSibling("history.txt.bak-20261014-153045")));
    }

    /**
     * Versão desconhecida também gera exceção e cópia, inclusive nas operações que gravam.
     */
    @Test
    void unknownVersionThrowsOnEveryOperationAndKeepsOriginal() throws IOException {
        Files.createDirectories(file.getParent());
        String content = "{\"version\":99,\"records\":[]}";
        Files.writeString(file, content, StandardCharsets.UTF_8);

        assertThrows(CorruptedStorageException.class, () -> repository.add(record(HelperId.newId(), now)));
        assertThrows(CorruptedStorageException.class, () -> repository.deleteExpired(now));
        assertThrows(CorruptedStorageException.class, () -> repository.findByHelper(HelperId.newId()));

        assertEquals(content, Files.readString(file));
        assertEquals(List.of("history.txt", "history.txt.bak-20261014-153045"), names());
    }
}
