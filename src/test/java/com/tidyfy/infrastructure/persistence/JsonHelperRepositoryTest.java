package com.tidyfy.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.AgeLimit;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.HelperType;
import com.tidyfy.domain.RecycleBinTarget;
import com.tidyfy.domain.Schedule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes do {@link JsonHelperRepository}, sempre em pasta temporária.
 */
class JsonHelperRepositoryTest {

    /** Pasta temporária do teste */
    @TempDir
    Path tempDir;

    /** Frequência de exemplo */
    private final Schedule schedule = new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 10, 14));

    /**
     * Cria o repositório apontando para a pasta "tidyfy" dentro da temporária (ainda inexistente).
     */
    private JsonHelperRepository repository() {
        return new JsonHelperRepository(location(), new HelperJsonMapper());
    }

    /**
     * Local de armazenamento do teste.
     */
    private StorageLocation location() {
        return new StorageLocation(tempDir.resolve("tidyfy"));
    }

    /**
     * Cria um organizador com o nome informado.
     */
    private Helper organizer(String name) {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName(name), schedule, true,
                Path.of("C:\\Users\\ana\\" + name));
    }

    /**
     * Cria um limpador de pasta com o nome informado.
     */
    private Helper cleaner(String name) {
        return new CleanerHelper(HelperId.newId(), new HelperName(name), schedule, true,
                new FolderTarget(Path.of("C:\\Temp\\" + name), AgeReference.CREATED), new AgeLimit(7));
    }

    /**
     * Lista os arquivos da pasta do Tidyfy.
     */
    private List<String> filesInFolder() throws IOException {
        try (Stream<Path> files = Files.list(tempDir.resolve("tidyfy"))) {
            return files.map(path -> path.getFileName().toString()).toList();
        }
    }

    /**
     * Salva ajudantes dos dois tipos e recarrega com outra instância.
     */
    @Test
    void savesAndReloadsBothTypes() {
        Helper organizer = organizer("Downloads");
        Helper folderCleaner = cleaner("Temp");
        Helper binCleaner = new CleanerHelper(HelperId.newId(), new HelperName("Lixeira"), schedule, false,
                new RecycleBinTarget(), null);
        JsonHelperRepository repository = repository();
        repository.save(organizer);
        repository.save(folderCleaner);
        repository.save(binCleaner);

        assertEquals(List.of(organizer, folderCleaner, binCleaner), repository().findAll());
        assertEquals(Optional.of(folderCleaner), repository().findById(folderCleaner.id()));
        assertEquals(Optional.empty(), repository().findById(HelperId.newId()));
    }

    /**
     * Sem arquivo, a lista é vazia e nada é criado na leitura.
     */
    @Test
    void returnsEmptyListWhenFileIsMissing() {
        assertEquals(List.of(), repository().findAll());
        assertFalse(Files.exists(tempDir.resolve("tidyfy")));
    }

    /**
     * Arquivo vazio ou em branco também dá lista vazia.
     */
    @Test
    void returnsEmptyListWhenFileIsEmptyOrBlank() throws IOException {
        Files.createDirectories(tempDir.resolve("tidyfy"));
        Path file = location().helpersFile();
        Files.writeString(file, "");
        assertEquals(List.of(), repository().findAll());
        Files.writeString(file, "  \n ");
        assertEquals(List.of(), repository().findAll());
    }

    /**
     * Cria a pasta e o arquivo quando faltam.
     */
    @Test
    void createsFolderAndFileOnFirstSave() {
        repository().save(organizer("A"));
        assertTrue(Files.isRegularFile(location().helpersFile()));
    }

    /**
     * Editar mantém a posição; criar adiciona no fim.
     */
    @Test
    void upsertKeepsPositionOnEditAndAppendsOnCreate() {
        Helper first = organizer("A");
        Helper second = cleaner("B");
        JsonHelperRepository repository = repository();
        repository.save(first);
        repository.save(second);
        Helper edited = new FolderOrganizerHelper(first.id(), new HelperName("Renomeado"), schedule, false,
                Path.of("C:\\Users\\ana\\Outra"));
        repository.save(edited);
        Helper third = organizer("C");
        repository.save(third);

        assertEquals(List.of(edited, second, third), repository.findAll());
    }

    /**
     * Conta os ajudantes de cada tipo.
     */
    @Test
    void countsByType() {
        JsonHelperRepository repository = repository();
        assertEquals(0, repository.countByType(HelperType.CLEANER));
        repository.save(organizer("A"));
        repository.save(cleaner("B"));
        repository.save(cleaner("C"));
        assertEquals(1, repository.countByType(HelperType.FOLDER_ORGANIZER));
        assertEquals(2, repository.countByType(HelperType.CLEANER));
    }

    /**
     * Remove o ajudante e ignora id inexistente.
     */
    @Test
    void deletesHelperAndIgnoresUnknownId() {
        Helper first = organizer("A");
        Helper second = cleaner("B");
        JsonHelperRepository repository = repository();
        repository.save(first);
        repository.save(second);

        repository.delete(HelperId.newId());
        assertEquals(List.of(first, second), repository.findAll());
        repository.delete(first.id());
        assertEquals(List.of(second), repository.findAll());
    }

    /**
     * Delete em repositório sem arquivo não cria nada.
     */
    @Test
    void deleteWithoutFileDoesNothing() {
        repository().delete(HelperId.newId());
        assertFalse(Files.exists(tempDir.resolve("tidyfy")));
    }

    /**
     * Depois de gravar, só sobra o helpers.txt.
     */
    @Test
    void leavesNoTemporaryFilesAfterSaving() throws IOException {
        JsonHelperRepository repository = repository();
        Helper first = organizer("A");
        repository.save(first);
        repository.save(cleaner("B"));
        repository.delete(first.id());
        assertEquals(List.of("helpers.txt"), filesInFolder());
    }

    /**
     * Se o destino não puder ser substituído, falha com UncheckedIOException, sem temporários
     * e sem alterar o que já existia no destino.
     */
    @Test
    void failedWriteThrowsAndLeavesNoTemporaryFile() throws IOException {
        Path file = location().helpersFile();
        Files.createDirectories(file);
        Path inside = Files.writeString(file.resolve("marker.txt"), "x");

        assertThrows(UncheckedIOException.class, () -> repository().save(organizer("A")));
        assertTrue(Files.exists(inside));
        assertEquals(List.of("helpers.txt"), filesInFolder());
    }

    /**
     * JSON inválido propaga a exceção do mapeador e não altera o arquivo.
     */
    @Test
    void invalidJsonPropagatesWithoutChangingFile() throws IOException {
        Files.createDirectories(tempDir.resolve("tidyfy"));
        Path file = location().helpersFile();
        Files.writeString(file, "{ isso nao e json");

        JsonHelperRepository repository = repository();
        assertThrows(HelperJsonException.class, repository::findAll);
        assertThrows(HelperJsonException.class, () -> repository.save(organizer("A")));
        assertThrows(HelperJsonException.class, () -> repository.delete(HelperId.newId()));
        assertEquals("{ isso nao e json", Files.readString(file));
        assertEquals(List.of("helpers.txt"), filesInFolder());
    }
}
