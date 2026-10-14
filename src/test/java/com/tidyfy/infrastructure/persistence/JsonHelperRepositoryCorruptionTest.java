package com.tidyfy.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.HelperType;
import com.tidyfy.domain.Schedule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes do {@link JsonHelperRepository} diante de arquivo corrompido ou de versão desconhecida.
 */
class JsonHelperRepositoryCorruptionTest {

    /** Pasta temporária do teste */
    @TempDir
    Path tempDir;

    /** Relógio fixo em 2026-10-14 15:30:45 UTC */
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-14T15:30:45Z"), ZoneOffset.UTC);

    /** Arquivo de configurações */
    private Path file;

    /** Repositório testado */
    private JsonHelperRepository repository;

    /**
     * Prepara a pasta e o repositório.
     */
    @BeforeEach
    void setUp() throws IOException {
        Path folder = tempDir.resolve("tidyfy");
        Files.createDirectories(folder);
        StorageLocation location = new StorageLocation(folder);
        file = location.helpersFile();
        repository = new JsonHelperRepository(location, new HelperJsonMapper(), clock);
    }

    /**
     * Repositório com o relógio deslocado.
     */
    private JsonHelperRepository repositoryLater(Duration offset) {
        return new JsonHelperRepository(new StorageLocation(file.getParent()), new HelperJsonMapper(),
                Clock.offset(clock, offset));
    }

    /**
     * Nomes dos arquivos da pasta, ordenados.
     */
    private List<String> names() throws IOException {
        try (Stream<Path> files = Files.list(file.getParent())) {
            return files.map(path -> path.getFileName().toString()).filter(name -> !name.endsWith(".lock")).sorted().toList();
        }
    }

    /**
     * Ajudante de exemplo.
     */
    private Helper organizer() {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName("A"),
                new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 10, 14)), true,
                Path.of("C:\\Users\\ana\\A"));
    }

    /**
     * JSON inválido: mensagem com caminho e motivo, causa preservada.
     */
    @Test
    void invalidJsonFailsWithClearMessageAndCause() throws IOException {
        Files.writeString(file, "{ nao e json");

        CorruptedStorageException e = assertThrows(CorruptedStorageException.class, repository::findAll);

        assertTrue(e.getMessage().contains(file.toString()));
        assertTrue(e.getMessage().contains("corrompido"));
        assertInstanceOf(HelperJsonException.class, e.getCause());
        assertTrue(e.getMessage().contains(e.getCause().getMessage()));
    }

    /**
     * Versão desconhecida também gera o erro.
     */
    @Test
    void unknownVersionFails() throws IOException {
        Files.writeString(file, "{\"version\": 99, \"helpers\": []}");

        CorruptedStorageException e = assertThrows(CorruptedStorageException.class, repository::findAll);

        assertInstanceOf(HelperJsonException.class, e.getCause());
    }

    /**
     * O original fica intacto e a cópia tem o carimbo esperado e o mesmo conteúdo.
     */
    @Test
    void keepsOriginalAndCreatesStampedBackup() throws IOException {
        byte[] bytes = "{ nao e json ç".getBytes(StandardCharsets.UTF_8);
        Files.write(file, bytes);

        assertThrows(CorruptedStorageException.class, repository::findAll);

        assertArrayEquals(bytes, Files.readAllBytes(file));
        assertEquals(List.of("helpers.txt", "helpers.txt.bak-20261014-153045"), names());
        assertArrayEquals(bytes, Files.readAllBytes(file.resolveSibling("helpers.txt.bak-20261014-153045")));
    }

    /**
     * Chamadas repetidas com o mesmo conteúdo não criam cópias duplicadas, mesmo com outro horário.
     */
    @Test
    void repeatedCallsDoNotDuplicateBackups() throws IOException {
        Files.writeString(file, "lixo");

        assertThrows(CorruptedStorageException.class, repository::findAll);
        assertThrows(CorruptedStorageException.class, repositoryLater(Duration.ofHours(1))::findAll);
        assertThrows(CorruptedStorageException.class, repository::findAll);

        assertEquals(List.of("helpers.txt", "helpers.txt.bak-20261014-153045"), names());
    }

    /**
     * Conteúdo corrompido diferente gera uma nova cópia.
     */
    @Test
    void differentCorruptedContentCreatesNewBackup() throws IOException {
        Files.writeString(file, "lixo 1");
        assertThrows(CorruptedStorageException.class, repository::findAll);
        Files.writeString(file, "lixo 2");

        assertThrows(CorruptedStorageException.class, repositoryLater(Duration.ofSeconds(5))::findAll);

        assertEquals(List.of("helpers.txt", "helpers.txt.bak-20261014-153045", "helpers.txt.bak-20261014-153050"),
                names());
    }

    /**
     * Operações que gravam ou consultam falham e não alteram o original.
     */
    @Test
    void writingAndQueryOperationsFailWithoutChangingOriginal() throws IOException {
        byte[] bytes = "{ quebrado".getBytes(StandardCharsets.UTF_8);
        Files.write(file, bytes);

        assertThrows(CorruptedStorageException.class, () -> repository.save(organizer()));
        assertThrows(CorruptedStorageException.class, () -> repository.delete(HelperId.newId()));
        assertThrows(CorruptedStorageException.class, () -> repository.countByType(HelperType.CLEANER));

        assertArrayEquals(bytes, Files.readAllBytes(file));
        assertEquals(List.of("helpers.txt", "helpers.txt.bak-20261014-153045"), names());
    }

    /**
     * Arquivo ausente, vazio ou válido continua funcionando e não gera cópia.
     */
    @Test
    void validAbsentOrEmptyFilesCreateNoBackup() throws IOException {
        assertTrue(repository.findAll().isEmpty());

        Files.writeString(file, "");
        assertTrue(repository.findAll().isEmpty());

        Helper helper = organizer();
        repository.save(helper);
        assertEquals(Optional.of(helper), repository.findById(helper.id()));
        assertEquals(List.of("helpers.txt"), names());
    }
}
