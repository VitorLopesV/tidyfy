package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link HelperRules}.
 */
class HelperRulesTest {

    private static final Schedule SCHEDULE = new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    private static final ProtectedFolders PROTECTED = ProtectedFolders.forWindows(
            Path.of("C:\\Windows"), Path.of("C:\\Program Files"), Path.of("C:\\Program Files (x86)"),
            Path.of("C:\\ProgramData"), Path.of("C:\\Users\\nome"));

    private static final String DOWNLOADS = "C:\\Users\\nome\\Downloads";

    private final HelperRules rules = new HelperRules(PROTECTED);

    private static Helper helper(String name) {
        return organizer(name, "C:\\Users\\nome\\pasta-" + name.trim().toLowerCase());
    }

    private static Helper organizer(String name, String folder) {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName(name), SCHEDULE, true, Path.of(folder));
    }

    private static Helper cleaner(String name, CleanupTarget target, boolean enabled) {
        return new CleanerHelper(HelperId.newId(), new HelperName(name), SCHEDULE, enabled, target, null);
    }

    private static Helper desktopArranger(String name) {
        return new DesktopArrangerHelper(HelperId.newId(), new HelperName(name), SCHEDULE, true,
                DesktopLayout.LEFT);
    }

    private static CleanupTarget folderTarget(String folder) {
        return new FolderTarget(Path.of(folder), AgeReference.CREATED);
    }

    @Test
    void rejectsSameFolderForTwoOrganizers() {
        Helper existing = organizer("A", DOWNLOADS);

        assertThrows(FolderAlreadyInUseException.class,
                () -> rules.assertCanSave(organizer("B", DOWNLOADS), List.of(existing)));
    }

    @Test
    void rejectsSameFolderIgnoringCaseAndRedundantSegments() {
        Helper existing = organizer("A", DOWNLOADS);

        assertThrows(FolderAlreadyInUseException.class, () -> rules.assertCanSave(
                organizer("B", "c:\\users\\NOME\\x\\..\\downloads"), List.of(existing)));
    }

    @Test
    void rejectsNestedFolderInsideAnotherOfTheSameType() {
        Helper existing = organizer("A", DOWNLOADS);

        assertThrows(FolderAlreadyInUseException.class,
                () -> rules.assertCanSave(organizer("B", DOWNLOADS + "\\Imagens"), List.of(existing)));
    }

    @Test
    void rejectsFolderThatContainsAnotherOfTheSameType() {
        Helper existing = organizer("A", DOWNLOADS + "\\Imagens");

        assertThrows(FolderAlreadyInUseException.class,
                () -> rules.assertCanSave(organizer("B", DOWNLOADS), List.of(existing)));
    }

    @Test
    void acceptsSiblingFolderWithSamePrefix() {
        Helper existing = organizer("A", DOWNLOADS);

        assertDoesNotThrow(() -> rules.assertCanSave(organizer("B", DOWNLOADS + "2"), List.of(existing)));
    }

    @Test
    void rejectsNestedFolderForTwoCleaners() {
        Helper existing = cleaner("A", folderTarget(DOWNLOADS), true);

        assertThrows(FolderAlreadyInUseException.class, () -> rules.assertCanSave(
                cleaner("B", folderTarget(DOWNLOADS + "\\Velhos"), true), List.of(existing)));
    }

    @Test
    void acceptsOrganizerAndCleanerOnTheSameFolder() {
        Helper existingOrganizer = organizer("A", DOWNLOADS);
        Helper existingCleaner = cleaner("C", folderTarget(DOWNLOADS), true);

        assertDoesNotThrow(() -> rules.assertCanSave(
                cleaner("B", folderTarget(DOWNLOADS), true), List.of(existingOrganizer)));
        assertDoesNotThrow(() -> rules.assertCanSave(organizer("D", DOWNLOADS), List.of(existingCleaner)));
    }

    @Test
    void rejectsSecondCleanerOnTheRecycleBin() {
        Helper existing = cleaner("A", new RecycleBinTarget(), true);

        FolderAlreadyInUseException exception = assertThrows(FolderAlreadyInUseException.class,
                () -> rules.assertCanSave(cleaner("B", new RecycleBinTarget(), true), List.of(existing)));

        assertTrue(exception.getMessage().contains("\"A\""));
    }

    @Test
    void acceptsRecycleBinCleanerBesideFolderCleaner() {
        Helper existing = cleaner("A", folderTarget(DOWNLOADS), true);

        assertDoesNotThrow(() -> rules.assertCanSave(cleaner("B", new RecycleBinTarget(), true), List.of(existing)));
    }

    @Test
    void disabledHelpersStillCountForOverlap() {
        Helper disabled = cleaner("A", new RecycleBinTarget(), false);

        assertThrows(FolderAlreadyInUseException.class,
                () -> rules.assertCanSave(cleaner("B", new RecycleBinTarget(), true), List.of(disabled)));
    }

    @Test
    void acceptsEditingHelperWhenOthersExcludeIt() {
        assertDoesNotThrow(() -> rules.assertCanSave(organizer("A", DOWNLOADS), List.of()));
    }

    @Test
    void acceptsDesktopArrangerBesideFolderHelpers() {
        assertDoesNotThrow(() -> rules.assertCanSave(desktopArranger("Área de trabalho"),
                List.of(organizer("A", DOWNLOADS), cleaner("B", folderTarget(DOWNLOADS), true),
                        cleaner("C", new RecycleBinTarget(), true))));
    }

    @Test
    void rejectsDesktopArrangerNameUsedByAnotherHelper() {
        assertThrows(HelperNameAlreadyUsedException.class, () -> rules.assertCanSave(
                desktopArranger("downloads"), List.of(organizer("Downloads", DOWNLOADS))));
    }

    @Test
    void rejectsProtectedFolderForOrganizer() {
        assertThrows(ProtectedFolderException.class,
                () -> rules.assertCanSave(organizer("A", "C:\\Windows\\System32"), List.of()));
    }

    @Test
    void rejectsProtectedFolderForCleaner() {
        assertThrows(ProtectedFolderException.class, () -> rules.assertCanSave(
                cleaner("A", folderTarget("C:\\Program Files\\App"), true), List.of()));
    }

    @Test
    void acceptsRecycleBinCleanerWhenThereAreNoOthers() {
        assertDoesNotThrow(() -> rules.assertCanSave(cleaner("A", new RecycleBinTarget(), true), List.of()));
    }

    @Test
    void folderAlreadyInUseExceptionIsADomainException() {
        assertInstanceOf(DomainException.class,
                FolderAlreadyInUseException.forFolder(Path.of(DOWNLOADS), new HelperName("A")));
    }

    @Test
    void rejectsNullProtectedFolders() {
        assertThrows(NullPointerException.class, () -> new HelperRules(null));
    }

    @Test
    void rejectsNameAlreadyUsedByAnotherHelper() {
        Helper existing = helper("Organizar Downloads");

        assertThrows(HelperNameAlreadyUsedException.class,
                () -> rules.assertCanSave(helper("Organizar Downloads"), List.of(existing)));
    }

    @Test
    void rejectsNameThatDiffersOnlyByCase() {
        Helper existing = helper("Organizar Downloads");

        assertThrows(HelperNameAlreadyUsedException.class,
                () -> rules.assertCanSave(helper("organizar downloads"), List.of(existing)));
    }

    @Test
    void rejectsNameThatDiffersOnlyBySurroundingSpaces() {
        Helper existing = helper("Organizar Downloads");

        assertThrows(HelperNameAlreadyUsedException.class,
                () -> rules.assertCanSave(helper("  Organizar Downloads "), List.of(existing)));
    }

    @Test
    void rejectsNameUsedByAnyOfTheOthers() {
        List<Helper> others = List.of(helper("Fotos"), helper("Limpeza"), helper("Documentos"));

        assertThrows(HelperNameAlreadyUsedException.class,
                () -> rules.assertCanSave(helper("Limpeza"), others));
    }

    @Test
    void acceptsNameNotUsedByTheOthers() {
        List<Helper> others = List.of(helper("Fotos"), helper("Limpeza"));

        assertDoesNotThrow(() -> rules.assertCanSave(helper("Documentos"), others));
    }

    @Test
    void acceptsNameWhenThereAreNoOthers() {
        assertDoesNotThrow(() -> rules.assertCanSave(helper("Fotos"), List.of()));
    }

    @Test
    void acceptsOwnNameOnEditBecauseOthersExcludeTheHelperItself() {
        Helper edited = helper("Fotos");
        List<Helper> others = List.of(helper("Limpeza"));

        assertDoesNotThrow(() -> rules.assertCanSave(edited, others));
    }

    @Test
    void exceptionMessageMentionsTheName() {
        HelperNameAlreadyUsedException exception = assertThrows(HelperNameAlreadyUsedException.class,
                () -> rules.assertCanSave(helper("Fotos"), List.of(helper("fotos"))));

        assertTrue(exception.getMessage().contains("Fotos"));
    }

    @Test
    void nameAlreadyUsedExceptionIsADomainException() {
        assertInstanceOf(DomainException.class, new HelperNameAlreadyUsedException(new HelperName("Fotos")));
    }
}
