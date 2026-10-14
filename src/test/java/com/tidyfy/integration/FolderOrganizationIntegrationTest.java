package com.tidyfy.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.FolderOrganizerHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes de integração da organização de pasta: rodam de verdade no disco (pasta temporária), com a lixeira
 * simulada por um executor de comandos dublê.
 */
class FolderOrganizationIntegrationTest {

    private static final List<String> ALL_FOLDERS =
            List.of("Compactados", "Documentos", "Executáveis", "Imagens", "Outros", "Pastas", "Vídeos");

    @TempDir
    Path tempDir;

    private IntegrationStack stack;

    private Path folder;

    private FolderOrganizerHelper helper;

    @BeforeEach
    void setUp() {
        stack = new IntegrationStack(tempDir.resolve("storage"));
        folder = tempDir.resolve("Downloads");
        helper = stack.saveOrganizer(folder);
    }

    private static List<String> namesIn(Path directory) {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.map(path -> path.getFileName().toString()).sorted().toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String readString(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void createMixedFolder() {
        IntegrationStack.write(folder.resolve("foto.JPG"), "foto");
        IntegrationStack.write(folder.resolve("relatorio.pdf"), "relatorio");
        IntegrationStack.write(folder.resolve("setup.exe"), "setup");
        IntegrationStack.write(folder.resolve("backup.zip"), "backup");
        IntegrationStack.write(folder.resolve("filme.mp4"), "filme");
        IntegrationStack.write(folder.resolve("musica.mp3"), "musica");
        IntegrationStack.write(folder.resolve("LEIAME"), "sem extensao");
        IntegrationStack.write(folder.resolve("Projeto").resolve("codigo.pdf"), "dentro da subpasta");
        IntegrationStack.write(folder.resolve("Projeto").resolve("fundo").resolve("a.txt"), "mais fundo");
    }

    @Test
    void sendsEachItemToTheRightFolder() {
        createMixedFolder();

        stack.run(helper);

        assertEquals(ALL_FOLDERS, namesIn(folder));
        assertEquals(List.of("backup.zip"), namesIn(folder.resolve("Compactados")));
        assertEquals(List.of("relatorio.pdf"), namesIn(folder.resolve("Documentos")));
        assertEquals(List.of("setup.exe"), namesIn(folder.resolve("Executáveis")));
        assertEquals(List.of("foto.JPG"), namesIn(folder.resolve("Imagens")));
        assertEquals(List.of("filme.mp4"), namesIn(folder.resolve("Vídeos")));
        assertEquals(List.of("LEIAME", "musica.mp3"), namesIn(folder.resolve("Outros")));
        assertEquals(List.of("Projeto"), namesIn(folder.resolve("Pastas")));
    }

    @Test
    void movesTheSubfolderWholeWithoutLookingInside() {
        createMixedFolder();

        stack.run(helper);

        Path moved = folder.resolve("Pastas").resolve("Projeto");
        assertEquals("dentro da subpasta", readString(moved.resolve("codigo.pdf")));
        assertEquals("mais fundo", readString(moved.resolve("fundo").resolve("a.txt")));
        assertFalse(Files.exists(folder.resolve("Documentos").resolve("codigo.pdf")));
    }

    @Test
    void doesNothingOnTheSecondRun() {
        createMixedFolder();
        stack.run(helper);

        ExecutionRecord second = stack.run(helper).orElseThrow();

        assertTrue(second.actions().isEmpty());
        assertEquals(ALL_FOLDERS, namesIn(folder));
        assertEquals(List.of("Projeto"), namesIn(folder.resolve("Pastas")));
    }

    @Test
    void sendsTheOutsideFileToTheTrashWhenTheContentIsDuplicated() {
        IntegrationStack.write(folder.resolve("Documentos").resolve("teste.pdf"), "igual");
        Path outside = IntegrationStack.write(folder.resolve("teste.pdf"), "igual");

        ExecutionRecord record = stack.run(helper).orElseThrow();

        assertFalse(Files.exists(outside));
        assertEquals(List.of(outside), stack.trashed());
        assertEquals(List.of("teste.pdf"), namesIn(folder.resolve("Documentos")));
        assertEquals(ActionType.DUPLICATE_TRASHED, record.actions().get(0).type());
    }

    @Test
    void numbersTheFileWhenTheNameConflictsWithDifferentContent() {
        IntegrationStack.write(folder.resolve("Documentos").resolve("teste.pdf"), "original");
        IntegrationStack.write(folder.resolve("teste.pdf"), "outro conteudo");

        stack.run(helper);
        IntegrationStack.write(folder.resolve("teste.pdf"), "terceiro conteudo");
        stack.run(helper);

        Path documents = folder.resolve("Documentos");
        assertEquals(List.of("teste(1).pdf", "teste(2).pdf", "teste.pdf"), namesIn(documents));
        assertEquals("original", readString(documents.resolve("teste.pdf")));
        assertEquals("outro conteudo", readString(documents.resolve("teste(1).pdf")));
        assertEquals("terceiro conteudo", readString(documents.resolve("teste(2).pdf")));
        assertTrue(stack.trashed().isEmpty());
    }

    @Test
    void numbersTheFolderWhenItsNameConflictsInsideFolders() {
        IntegrationStack.write(folder.resolve("Pastas").resolve("Fotos").resolve("velha.jpg"), "velha");
        IntegrationStack.write(folder.resolve("Fotos").resolve("nova.jpg"), "nova");

        stack.run(helper);

        Path folders = folder.resolve("Pastas");
        assertEquals(List.of("Fotos", "Fotos(1)"), namesIn(folders));
        assertEquals(List.of("velha.jpg"), namesIn(folders.resolve("Fotos")));
        assertEquals(List.of("nova.jpg"), namesIn(folders.resolve("Fotos(1)")));
    }

    @Test
    void keepsTheHelperOwnFoldersInPlaceAndReorganizesOnlyNewFiles() {
        createMixedFolder();
        stack.run(helper);
        IntegrationStack.write(folder.resolve("novo.png"), "novo");

        ExecutionRecord second = stack.run(helper).orElseThrow();

        assertEquals(1, second.actions().size());
        assertEquals(List.of("foto.JPG", "novo.png"), namesIn(folder.resolve("Imagens")));
        assertTrue(Files.isDirectory(folder.resolve("Pastas").resolve("Projeto")));
    }
}
