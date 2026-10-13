package com.tidyfy.presentation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Testa a seleção de ajudantes da lista.
 */
class HelperSelectionTest {

    private final HelperSelection selection = new HelperSelection();

    @Test
    void startsEmptyWithNothingToEditOrRemove() {
        assertTrue(selection.isEmpty());
        assertFalse(selection.canEdit());
        assertFalse(selection.canRemove());
    }

    @Test
    void oneSelectedHelperCanBeEditedAndRemoved() {
        selection.set("a", true);

        assertTrue(selection.canEdit());
        assertTrue(selection.canRemove());
        assertEquals(Optional.of("a"), selection.editableId());
    }

    @Test
    void severalSelectedHelpersCanBeRemovedButNotEdited() {
        selection.set("a", true);
        selection.set("b", true);

        assertFalse(selection.canEdit());
        assertTrue(selection.canRemove());
        assertEquals(Optional.empty(), selection.editableId());
    }

    @Test
    void editingIsAllowedAgainWhenOnlyOneRemainsSelected() {
        selection.set("a", true);
        selection.set("b", true);
        selection.set("b", false);

        assertTrue(selection.canEdit());
        assertEquals(Optional.of("a"), selection.editableId());
    }

    @Test
    void selectingTheSameHelperTwiceCountsOnce() {
        selection.set("a", true);
        selection.set("a", true);

        assertEquals(1, selection.count());
    }

    @Test
    void replaceWithKeepsOnlyTheGivenHelpers() {
        selection.set("a", true);
        selection.replaceWith(List.of("b", "c"));

        assertFalse(selection.contains("a"));
        assertEquals(2, selection.count());
    }

    @Test
    void clearUnselectsEverything() {
        selection.set("a", true);
        selection.clear();

        assertTrue(selection.isEmpty());
    }

    @Test
    void labelsUseSingularForOneHelper() {
        selection.set("a", true);

        assertEquals("ajudante selecionado", selection.summary());
        assertEquals("Remover", selection.removeLabel());
    }

    @Test
    void labelsUsePluralAndCountForSeveralHelpers() {
        selection.replaceWith(List.of("a", "b", "c"));

        assertEquals("ajudantes selecionados", selection.summary());
        assertEquals("Remover 3", selection.removeLabel());
    }
}
