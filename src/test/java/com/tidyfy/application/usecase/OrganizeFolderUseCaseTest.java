package com.tidyfy.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeFileSystem;
import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.FileMove;
import com.tidyfy.domain.FolderOrganizationPlanner;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link OrganizeFolderUseCase}, com o sistema de arquivos em memória.
 */
class OrganizeFolderUseCaseTest {

    private static final Path FOLDER = Path.of("C:\\Users\\nome\\Downloads");

    private static final Instant TIME = Instant.parse("2026-01-01T00:00:00Z");

    private static final FolderOrganizerHelper HELPER = new FolderOrganizerHelper(HelperId.newId(),
            new HelperName("Organizar Downloads"),
            new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1)), true, FOLDER);

    private final FakeFileSystem fileSystem = new FakeFileSystem();

    private final OrganizeFolderUseCase useCase =
            new OrganizeFolderUseCase(fileSystem, new FolderOrganizationPlanner());

    private Path file(String relative, String content) {
        Path path = FOLDER.resolve(relative);
        fileSystem.addFile(path, content, TIME, TIME);
        return path;
    }

    private static HelperAction moved(Path source, Path target) {
        return new HelperAction(ActionType.MOVED, source, target, null);
    }

    @Test
    void movesFileToItsCategoryFolder() {
        fileSystem.addFolder(FOLDER);
        Path photo = file("foto.png", "x");

        List<HelperAction> actions = useCase.execute(HELPER);

        Path target = FOLDER.resolve("Imagens").resolve("foto.png");
        assertEquals(List.of(moved(photo, target)), actions);
        assertEquals(List.of(new FileMove(photo, target)), fileSystem.moves());
        assertTrue(fileSystem.exists(target));
    }

    @Test
    void movesUnknownExtensionToOthers() {
        Path file = file("dados.xyz", "x");

        List<HelperAction> actions = useCase.execute(HELPER);

        assertEquals(List.of(moved(file, FOLDER.resolve("Outros").resolve("dados.xyz"))), actions);
    }

    @Test
    void movesLooseFolderToFoldersFolder() {
        Path loose = FOLDER.resolve("Fotos de viagem");
        fileSystem.addFolder(loose);

        List<HelperAction> actions = useCase.execute(HELPER);

        assertEquals(List.of(moved(loose, FOLDER.resolve("Pastas").resolve("Fotos de viagem"))), actions);
    }

    @Test
    void doesNotMoveTheHelperOwnFolders() {
        fileSystem.addFolder(FOLDER.resolve("Imagens"));
        fileSystem.addFolder(FOLDER.resolve("Pastas"));
        fileSystem.addFolder(FOLDER.resolve("Outros"));

        assertTrue(useCase.execute(HELPER).isEmpty());
        assertTrue(fileSystem.moves().isEmpty());
    }

    @Test
    void emptyFolderProducesNoActions() {
        fileSystem.addFolder(FOLDER);

        assertTrue(useCase.execute(HELPER).isEmpty());
    }

    @Test
    void sendsOutsideFileToTrashWhenDestinationHasSameContent() {
        Path outside = file("teste.pdf", "igual");
        Path existing = file("Documentos\\teste.pdf", "igual");

        List<HelperAction> actions = useCase.execute(HELPER);

        assertEquals(List.of(new HelperAction(ActionType.DUPLICATE_TRASHED, outside, null, null)), actions);
        assertEquals(List.of(outside), fileSystem.trashed());
        assertTrue(fileSystem.moves().isEmpty());
        assertTrue(fileSystem.exists(existing));
    }

    @Test
    void numbersNameWhenDestinationHasDifferentContent() {
        Path outside = file("teste.pdf", "novo");
        file("Documentos\\teste.pdf", "antigo");

        List<HelperAction> actions = useCase.execute(HELPER);

        Path target = FOLDER.resolve("Documentos").resolve("teste(1).pdf");
        assertEquals(List.of(moved(outside, target)), actions);
        assertTrue(fileSystem.trashed().isEmpty());
    }

    @Test
    void usesNextNumberWhenNumberedNamesAlreadyExist() {
        Path outside = file("teste.pdf", "novo");
        file("Documentos\\teste.pdf", "a");
        file("Documentos\\teste(1).pdf", "b");

        List<HelperAction> actions = useCase.execute(HELPER);

        assertEquals(List.of(moved(outside, FOLDER.resolve("Documentos").resolve("teste(2).pdf"))), actions);
    }

    @Test
    void numbersFolderOnConflictInsteadOfTreatingItAsDuplicate() {
        Path loose = FOLDER.resolve("Fotos");
        fileSystem.addFolder(loose);
        fileSystem.addFolder(FOLDER.resolve("Pastas").resolve("Fotos"));

        List<HelperAction> actions = useCase.execute(HELPER);

        assertEquals(List.of(moved(loose, FOLDER.resolve("Pastas").resolve("Fotos(1)"))), actions);
        assertTrue(fileSystem.trashed().isEmpty());
    }

    @Test
    void failureOnOneItemIsRecordedAndTheRestContinues() {
        Path first = file("a.pdf", "x");
        Path second = file("b.png", "y");
        fileSystem.failNextWriteWith(new UncheckedIOException(new IOException("sem permissão")));

        List<HelperAction> actions = useCase.execute(HELPER);

        assertEquals(2, actions.size());
        HelperAction failure = actions.get(0);
        assertEquals(ActionType.FAILED, failure.type());
        assertEquals(first, failure.source());
        assertEquals(FOLDER.resolve("Documentos").resolve("a.pdf"), failure.target());
        assertTrue(failure.message().contains("sem permissão"));
        assertEquals(moved(second, FOLDER.resolve("Imagens").resolve("b.png")), actions.get(1));
    }

    @Test
    void returnsActionsInTheOrderOfTheFolderItems() {
        Path pdf = file("a.pdf", "1");
        Path png = file("b.png", "2");
        Path other = file("c.xyz", "3");

        List<HelperAction> actions = useCase.execute(HELPER);

        assertEquals(List.of(
                moved(pdf, FOLDER.resolve("Documentos").resolve("a.pdf")),
                moved(png, FOLDER.resolve("Imagens").resolve("b.png")),
                moved(other, FOLDER.resolve("Outros").resolve("c.xyz"))), actions);
        assertNull(actions.get(0).message());
    }

    @Test
    void propagatesFailureWhenTheFolderCannotBeListed() {
        assertThrows(UncheckedIOException.class, () -> useCase.execute(HELPER));
    }
}
