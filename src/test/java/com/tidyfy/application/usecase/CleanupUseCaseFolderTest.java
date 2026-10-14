package com.tidyfy.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeFileSystem;
import com.tidyfy.application.fake.FakeRecycleBin;
import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.AgeLimit;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
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
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link CleanupUseCase} para o alvo de pasta, com o sistema de arquivos em memória.
 */
class CleanupUseCaseFolderTest {

    private static final Path FOLDER = Path.of("C:\\Users\\nome\\Downloads");

    private static final Instant NOW = Instant.parse("2026-03-31T12:00:00Z");

    private static final Instant OLD = NOW.minus(Duration.ofDays(40));

    private static final Instant RECENT = NOW.minus(Duration.ofDays(5));

    private final FakeFileSystem fileSystem = new FakeFileSystem();

    private final CleanupUseCase useCase =
            new CleanupUseCase(fileSystem, new FakeRecycleBin(), Clock.fixed(NOW, ZoneOffset.UTC));

    private static CleanerHelper helper(AgeReference reference, AgeLimit limit) {
        return new CleanerHelper(HelperId.newId(), new HelperName("Limpar Downloads"),
                new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1)), true,
                new FolderTarget(FOLDER, reference), limit);
    }

    private Path file(String name, Instant createdAt, Instant modifiedAt) {
        Path path = FOLDER.resolve(name);
        fileSystem.addFile(path, "x", createdAt, modifiedAt);
        return path;
    }

    private static HelperAction trashed(Path path) {
        return new HelperAction(ActionType.TRASHED, path, null, null);
    }

    @Test
    void cleansAllLooseFilesWhenThereIsNoAgeLimit() {
        Path first = file("a.txt", RECENT, RECENT);
        Path second = file("b.txt", OLD, OLD);

        List<HelperAction> actions = useCase.execute(helper(AgeReference.MODIFIED, null));

        assertEquals(List.of(trashed(first), trashed(second)), actions);
        assertEquals(List.of(first, second), fileSystem.trashed());
    }

    @Test
    void leavesSubfoldersUntouched() {
        Path subfolder = FOLDER.resolve("Antiga");
        fileSystem.addFolder(subfolder, OLD, OLD);
        Path inside = subfolder.resolve("dentro.txt");
        fileSystem.addFile(inside, "x", OLD, OLD);
        Path loose = file("solto.txt", OLD, OLD);

        List<HelperAction> actions = useCase.execute(helper(AgeReference.MODIFIED, new AgeLimit(30)));

        assertEquals(List.of(trashed(loose)), actions);
        assertTrue(fileSystem.exists(subfolder));
        assertTrue(fileSystem.exists(inside));
    }

    @Test
    void cleansOnlyFilesOlderThanLimitByModificationDate() {
        Path old = file("velho.txt", RECENT, OLD);
        file("novo.txt", OLD, RECENT);

        List<HelperAction> actions = useCase.execute(helper(AgeReference.MODIFIED, new AgeLimit(30)));

        assertEquals(List.of(trashed(old)), actions);
        assertEquals(List.of(old), fileSystem.trashed());
    }

    @Test
    void cleansOnlyFilesOlderThanLimitByCreationDate() {
        Path old = file("velho.txt", OLD, RECENT);
        file("novo.txt", RECENT, OLD);

        List<HelperAction> actions = useCase.execute(helper(AgeReference.CREATED, new AgeLimit(30)));

        assertEquals(List.of(trashed(old)), actions);
        assertEquals(List.of(old), fileSystem.trashed());
    }

    @Test
    void keepsFileExactlyAtTheLimit() {
        file("limite.txt", NOW.minus(Duration.ofDays(30)), NOW.minus(Duration.ofDays(30)));

        List<HelperAction> actions = useCase.execute(helper(AgeReference.MODIFIED, new AgeLimit(30)));

        assertTrue(actions.isEmpty());
        assertTrue(fileSystem.trashed().isEmpty());
    }

    @Test
    void returnsOneTrashedActionPerFileWithoutTarget() {
        Path first = file("a.txt", OLD, OLD);
        Path second = file("b.txt", OLD, OLD);

        List<HelperAction> actions = useCase.execute(helper(AgeReference.CREATED, new AgeLimit(30)));

        assertEquals(2, actions.size());
        assertEquals(List.of(first, second), actions.stream().map(HelperAction::source).toList());
        assertTrue(actions.stream().allMatch(a -> a.type() == ActionType.TRASHED));
        assertTrue(actions.stream().allMatch(a -> a.target() == null && a.message() == null));
    }

    @Test
    void recordsFailureAndContinuesWithNextFile() {
        Path first = file("a.txt", OLD, OLD);
        Path second = file("b.txt", OLD, OLD);
        fileSystem.failNextWriteWith(new UncheckedIOException(new IOException("sem permissão")));

        List<HelperAction> actions = useCase.execute(helper(AgeReference.MODIFIED, null));

        assertEquals(2, actions.size());
        assertEquals(ActionType.FAILED, actions.get(0).type());
        assertEquals(first, actions.get(0).source());
        assertNull(actions.get(0).target());
        assertTrue(actions.get(0).message().contains("sem permissão"));
        assertEquals(trashed(second), actions.get(1));
    }

    @Test
    void emptyFolderProducesNoActions() {
        fileSystem.addFolder(FOLDER);

        assertTrue(useCase.execute(helper(AgeReference.MODIFIED, new AgeLimit(1))).isEmpty());
    }

    @Test
    void usesInjectedClockToMeasureAge() {
        Path path = file("a.txt", NOW, NOW);
        CleanupUseCase later = new CleanupUseCase(fileSystem, new FakeRecycleBin(),
                Clock.fixed(NOW.plus(Duration.ofDays(2)), ZoneOffset.UTC));

        assertTrue(useCase.execute(helper(AgeReference.MODIFIED, new AgeLimit(1))).isEmpty());
        assertEquals(List.of(trashed(path)), later.execute(helper(AgeReference.MODIFIED, new AgeLimit(1))));
    }

    @Test
    void propagatesFailureToListTheFolder() {
        assertThrows(UncheckedIOException.class, () -> useCase.execute(helper(AgeReference.MODIFIED, null)));
    }
}
