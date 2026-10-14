package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link FolderOrganizationPlanner}.
 */
class FolderOrganizationPlannerTest {

    private static final Instant DATE = Instant.parse("2025-01-01T10:00:00Z");
    private final Path folder = Path.of("C:\\Downloads");
    private final FolderOrganizationPlanner planner = new FolderOrganizationPlanner();

    private FolderEntry file(String name) {
        return new FolderEntry(folder.resolve(name), false, DATE, DATE);
    }

    private FolderEntry dir(String name) {
        return new FolderEntry(folder.resolve(name), true, DATE, DATE);
    }

    private FileMove move(String name, String subfolder) {
        return new FileMove(folder.resolve(name), folder.resolve(subfolder).resolve(name));
    }

    @Test
    void movesKnownFileToItsCategoryFolder() {
        assertEquals(List.of(move("Foto.JPG", "Imagens")), planner.plan(folder, List.of(file("Foto.JPG"))));
    }

    @Test
    void movesUnknownExtensionToOthers() {
        assertEquals(List.of(move("musica.mp3", "Outros")), planner.plan(folder, List.of(file("musica.mp3"))));
    }

    @Test
    void movesFileWithoutExtensionToOthers() {
        assertEquals(List.of(move("LEIAME", "Outros")), planner.plan(folder, List.of(file("LEIAME"))));
    }

    @Test
    void movesUserFolderToFoldersFolder() {
        assertEquals(List.of(move("Trabalho", "Pastas")), planner.plan(folder, List.of(dir("Trabalho"))));
    }

    @Test
    void keepsHelperFoldersInPlace() {
        List<FolderEntry> entries = List.of(dir("Imagens"), dir("Pastas"), dir("Outros"), dir("Documentos"));

        assertTrue(planner.plan(folder, entries).isEmpty());
    }

    @Test
    void treatsHelperFolderNamesWithoutCaseSensitivity() {
        List<FolderEntry> entries = List.of(dir("imagens"), dir("PASTAS"), dir("outros"), dir("COMPACTADOS"));

        assertTrue(planner.plan(folder, entries).isEmpty());
    }

    @Test
    void handlesAccentedFolderNames() {
        List<FolderEntry> entries = List.of(file("setup.exe"), file("filme.mp4"), dir("Executáveis"), dir("vídeos"));

        assertEquals(
            List.of(move("setup.exe", "Executáveis"), move("filme.mp4", "Vídeos")),
            planner.plan(folder, entries));
    }

    @Test
    void movesFileNamedLikeHelperFolderToOthers() {
        assertEquals(List.of(move("Imagens", "Outros")), planner.plan(folder, List.of(file("Imagens"))));
    }

    @Test
    void returnsNothingForEmptyList() {
        assertTrue(planner.plan(folder, List.of()).isEmpty());
    }

    @Test
    void keepsInputOrderAndSkipsHelperFolders() {
        List<FolderEntry> entries = List.of(file("b.pdf"), dir("Imagens"), dir("Trabalho"), file("a.zip"), file("c.xyz"));

        assertEquals(
            List.of(move("b.pdf", "Documentos"), move("Trabalho", "Pastas"),
                move("a.zip", "Compactados"), move("c.xyz", "Outros")),
            planner.plan(folder, entries));
    }
}
