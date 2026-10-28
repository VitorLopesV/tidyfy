package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link DesktopLayoutPlanner} e {@link DesktopLayoutPlan}.
 */
class DesktopLayoutPlannerTest {

    /** Área de 3 colunas por 2 linhas, células de 100 x 100, a partir da origem. */
    private static final DesktopGrid SMALL = new DesktopGrid(0, 0, 300, 200, 100, 100);

    private final DesktopLayoutPlanner planner = new DesktopLayoutPlanner();

    private static DesktopIcon icon(String name) {
        return new DesktopIcon(name, new IconPosition(999, 999));
    }

    private static DesktopIcon at(String name, int x, int y) {
        return new DesktopIcon(name, new IconPosition(x, y));
    }

    private static List<DesktopIcon> icons(String... names) {
        List<DesktopIcon> icons = new ArrayList<>();
        for (String name : names) {
            icons.add(icon(name));
        }
        return icons;
    }

    @Test
    void leftFillsColumnsFromTheLeftEdge() {
        DesktopLayoutPlan plan = planner.plan(icons("a", "b", "c", "d", "e"), SMALL, DesktopLayout.LEFT);

        assertEquals(List.of(at("a", 0, 0), at("b", 0, 100), at("c", 100, 0), at("d", 100, 100), at("e", 200, 0)),
                plan.placements());
        assertTrue(plan.leftOut().isEmpty());
    }

    @Test
    void rightFillsColumnsFromTheRightEdge() {
        DesktopLayoutPlan plan = planner.plan(icons("a", "b", "c", "d", "e"), SMALL, DesktopLayout.RIGHT);

        assertEquals(List.of(at("a", 200, 0), at("b", 200, 100), at("c", 100, 0), at("d", 100, 100), at("e", 0, 0)),
                plan.placements());
    }

    @Test
    void topFillsRowsFromTheTopEdge() {
        DesktopLayoutPlan plan = planner.plan(icons("a", "b", "c", "d", "e"), SMALL, DesktopLayout.TOP);

        assertEquals(List.of(at("a", 0, 0), at("b", 100, 0), at("c", 200, 0), at("d", 0, 100), at("e", 100, 100)),
                plan.placements());
    }

    @Test
    void bottomFillsRowsFromTheBottomEdge() {
        DesktopLayoutPlan plan = planner.plan(icons("a", "b", "c", "d", "e"), SMALL, DesktopLayout.BOTTOM);

        assertEquals(List.of(at("a", 0, 100), at("b", 100, 100), at("c", 200, 100), at("d", 0, 0), at("e", 100, 0)),
                plan.placements());
    }

    @Test
    void bordersGoClockwiseAndKeepTheCenterFree() {
        DesktopGrid grid = new DesktopGrid(0, 0, 400, 300, 100, 100);

        DesktopLayoutPlan plan = planner.plan(
                icons("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"), grid, DesktopLayout.BORDERS);

        assertEquals(List.of(at("a", 0, 0), at("b", 100, 0), at("c", 200, 0), at("d", 300, 0),
                at("e", 300, 100), at("f", 300, 200),
                at("g", 200, 200), at("h", 100, 200), at("i", 0, 200),
                at("j", 0, 100)), plan.placements());
        assertEquals(icons("k", "l"), plan.leftOut());
        for (DesktopIcon placed : plan.placements()) {
            assertTrue(placed.position().x() == 0 || placed.position().x() == 300
                    || placed.position().y() == 0 || placed.position().y() == 200);
        }
    }

    @Test
    void bordersUseEveryCellWhenThereIsNoCenter() {
        DesktopLayoutPlan plan = planner.plan(icons("a", "b", "c", "d", "e", "f"), SMALL, DesktopLayout.BORDERS);

        assertEquals(6, plan.placements().size());
        assertEquals(6, positionsOf(plan).size());
        assertTrue(plan.leftOut().isEmpty());
    }

    @Test
    void bordersOnASingleRowFollowTheTopOrder() {
        DesktopGrid grid = new DesktopGrid(0, 0, 300, 100, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("a", "b", "c", "d"), grid, DesktopLayout.BORDERS);

        assertEquals(List.of(at("a", 0, 0), at("b", 100, 0), at("c", 200, 0)), plan.placements());
        assertEquals(icons("d"), plan.leftOut());
    }

    @Test
    void anchorsTheGridToTheEdgeOfTheLayoutWhenTheAreaIsNotAMultipleOfTheCell() {
        DesktopGrid grid = new DesktopGrid(1000, 10, 350, 250, 100, 100);

        assertEquals(at("a", 1250, 10), planner.plan(icons("a"), grid, DesktopLayout.RIGHT).placements().get(0));
        assertEquals(at("a", 1000, 160), planner.plan(icons("a"), grid, DesktopLayout.BOTTOM).placements().get(0));
        assertEquals(at("a", 1000, 10), planner.plan(icons("a"), grid, DesktopLayout.LEFT).placements().get(0));
        assertEquals(at("a", 1000, 10), planner.plan(icons("a"), grid, DesktopLayout.TOP).placements().get(0));
    }

    @Test
    void bordersTouchTheRightAndBottomEdgesWhenTheAreaIsNotAMultipleOfTheCell() {
        DesktopGrid grid = new DesktopGrid(1000, 10, 350, 250, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("a", "b", "c", "d", "e", "f"), grid, DesktopLayout.BORDERS);

        assertEquals(List.of(at("a", 1000, 10), at("b", 1100, 10), at("c", 1250, 10),
                at("d", 1250, 160), at("e", 1100, 160), at("f", 1000, 160)), plan.placements());
    }

    @Test
    void honorsAnOffsetThatIsNegative() {
        DesktopGrid grid = new DesktopGrid(-1920, 0, 300, 200, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("a", "b"), grid, DesktopLayout.TOP);

        assertEquals(List.of(at("a", -1920, 0), at("b", -1820, 0)), plan.placements());
    }

    @Test
    void byTypePutsEachGroupInItsOwnColumnFromTheLeft() {
        DesktopGrid grid = new DesktopGrid(0, 0, 500, 200, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("Steam", "App.lnk", "foto.PNG", "doc.pdf", "x.exe", "a.xyz",
                "b.jpg"), grid, DesktopLayout.BY_TYPE_LEFT);

        assertEquals(List.of(at("App.lnk", 0, 0), at("Steam", 0, 100),
                at("doc.pdf", 100, 0),
                at("x.exe", 200, 0),
                at("b.jpg", 300, 0), at("foto.PNG", 300, 100),
                at("a.xyz", 400, 0)), plan.placements());
        assertTrue(plan.leftOut().isEmpty());
    }

    @Test
    void byTypeSkipsEmptyGroupsAndTreatsUrlAsShortcut() {
        DesktopGrid grid = new DesktopGrid(0, 0, 500, 200, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("video.mp4", "site.url"), grid, DesktopLayout.BY_TYPE_LEFT);

        assertEquals(List.of(at("site.url", 0, 0), at("video.mp4", 100, 0)), plan.placements());
    }

    @Test
    void byTypeContinuesInTheNextColumnWhenAGroupHasMoreIconsThanRows() {
        DesktopGrid grid = new DesktopGrid(0, 0, 400, 200, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("A", "B", "C", "foto.jpg"), grid, DesktopLayout.BY_TYPE_LEFT);

        assertEquals(List.of(at("A", 0, 0), at("B", 0, 100), at("C", 100, 0), at("foto.jpg", 200, 0)),
                plan.placements());
    }

    @Test
    void byTypeReportsWhatDoesNotFit() {
        DesktopGrid grid = new DesktopGrid(0, 0, 100, 200, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("A", "B", "C", "doc.pdf"), grid, DesktopLayout.BY_TYPE_LEFT);

        assertEquals(List.of(at("A", 0, 0), at("B", 0, 100)), plan.placements());
        assertEquals(icons("C", "doc.pdf"), plan.leftOut());
    }

    @Test
    void byTypeUsesTheCategoriesGiven() {
        FileCategory music = new FileCategory("Música", "Música", Set.of(new FileExtension("mp3")));
        DesktopLayoutPlanner custom = new DesktopLayoutPlanner(List.of(music));
        DesktopGrid grid = new DesktopGrid(0, 0, 300, 100, 100, 100);

        DesktopLayoutPlan plan = custom.plan(icons("a.txt", "b.mp3"), grid, DesktopLayout.BY_TYPE_LEFT);

        assertEquals(List.of(at("b.mp3", 0, 0), at("a.txt", 100, 0)), plan.placements());
    }

    @Test
    void ordersByNameIgnoringCaseAndKeepsTheOriginalOrderOfEqualNames() {
        DesktopLayoutPlan plan = planner.plan(icons("b", "A", "c", "B"), SMALL, DesktopLayout.TOP);

        assertEquals(List.of("A", "B", "b", "c"),
                plan.placements().stream().map(DesktopIcon::name).toList());
    }

    @Test
    void duplicatedNamesAreBothPlaced() {
        DesktopLayoutPlan plan = planner.plan(icons("Chrome", "Chrome"), SMALL, DesktopLayout.TOP);

        assertEquals(List.of(at("Chrome", 0, 0), at("Chrome", 100, 0)), plan.placements());
    }

    @Test
    void sameInputInAnyOrderGivesTheSamePlan() {
        List<DesktopIcon> original = icons("Steam", "a.pdf", "Foto.jpg", "x.exe", "Zeta", "b.zip", "c.mp4");
        for (DesktopLayout layout : DesktopLayout.values()) {
            DesktopLayoutPlan expected = planner.plan(original, SMALL, layout);
            List<DesktopIcon> shuffled = new ArrayList<>(original);
            Collections.reverse(shuffled);

            assertEquals(expected, planner.plan(shuffled, SMALL, layout), layout.name());
            assertEquals(expected, planner.plan(original, SMALL, layout), layout.name());
        }
    }

    @Test
    void everyLayoutKeepsIconsInsideTheAreaWithoutOverlapping() {
        DesktopGrid grid = new DesktopGrid(50, 20, 640, 430, 112, 123);
        List<DesktopIcon> many = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            many.add(icon((i % 2 == 0 ? "Arquivo " : "Foto ") + i + (i % 3 == 0 ? ".png" : "")));
        }
        for (DesktopLayout layout : DesktopLayout.values()) {
            DesktopLayoutPlan plan = planner.plan(many, grid, layout);

            assertEquals(many.size(), plan.placements().size() + plan.leftOut().size(), layout.name());
            assertTrue(plan.leftOut().size() > 0, layout.name());
            List<IconPosition> positions = positionsOf(plan);
            assertEquals(positions.size(), new HashSet<>(positions).size(), layout.name());
            for (IconPosition position : positions) {
                assertTrue(position.x() >= grid.left() && position.x() + grid.cellWidth() <= grid.left() + grid.width(),
                        layout.name());
                assertTrue(position.y() >= grid.top() && position.y() + grid.cellHeight() <= grid.top() + grid.height(),
                        layout.name());
            }
            for (int i = 0; i < positions.size(); i++) {
                for (int j = i + 1; j < positions.size(); j++) {
                    boolean apartX = Math.abs(positions.get(i).x() - positions.get(j).x()) >= grid.cellWidth();
                    boolean apartY = Math.abs(positions.get(i).y() - positions.get(j).y()) >= grid.cellHeight();
                    assertTrue(apartX || apartY, layout.name());
                }
            }
        }
    }

    @Test
    void placedIconsKeepTheirNamesAndTheOnesLeftOutKeepTheirPosition() {
        DesktopGrid grid = new DesktopGrid(0, 0, 100, 100, 100, 100);
        DesktopIcon first = at("a", 5, 6);
        DesktopIcon second = at("b", 7, 8);

        DesktopLayoutPlan plan = planner.plan(List.of(second, first), grid, DesktopLayout.LEFT);

        assertEquals(List.of(at("a", 0, 0)), plan.placements());
        assertEquals(List.of(second), plan.leftOut());
    }

    @Test
    void leavesEverythingOutWhenTheAreaHoldsNoCell() {
        DesktopGrid grid = new DesktopGrid(0, 0, 50, 50, 100, 100);
        List<DesktopIcon> input = icons("a", "b.pdf");
        for (DesktopLayout layout : DesktopLayout.values()) {
            DesktopLayoutPlan plan = planner.plan(input, grid, layout);

            assertTrue(plan.placements().isEmpty(), layout.name());
            assertEquals(2, plan.leftOut().size(), layout.name());
        }
    }

    @Test
    void emptyDesktopGivesAnEmptyPlan() {
        for (DesktopLayout layout : DesktopLayout.values()) {
            DesktopLayoutPlan plan = planner.plan(List.of(), SMALL, layout);

            assertTrue(plan.placements().isEmpty());
            assertTrue(plan.leftOut().isEmpty());
        }
    }

    @Test
    void doesNotChangeTheListReceived() {
        List<DesktopIcon> input = icons("b", "a");
        List<DesktopIcon> copy = new ArrayList<>(input);

        planner.plan(input, SMALL, DesktopLayout.LEFT);

        assertEquals(copy, input);
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> planner.plan(null, SMALL, DesktopLayout.LEFT));
        assertThrows(NullPointerException.class, () -> planner.plan(List.of(), null, DesktopLayout.LEFT));
        assertThrows(NullPointerException.class, () -> planner.plan(List.of(), SMALL, null));
        assertThrows(NullPointerException.class, () -> new DesktopLayoutPlanner(null));
    }

    @Test
    void planCopiesItsListsAndRejectsNull() {
        List<DesktopIcon> placements = new ArrayList<>(List.of(at("a", 0, 0)));
        DesktopLayoutPlan plan = new DesktopLayoutPlan(placements, List.of());

        placements.clear();

        assertEquals(1, plan.placements().size());
        assertThrows(UnsupportedOperationException.class, () -> plan.placements().add(at("b", 1, 1)));
        assertThrows(NullPointerException.class, () -> new DesktopLayoutPlan(null, List.of()));
        assertThrows(NullPointerException.class, () -> new DesktopLayoutPlan(List.of(), null));
    }

    private static List<IconPosition> positionsOf(DesktopLayoutPlan plan) {
        return plan.placements().stream().map(DesktopIcon::position).toList();
    }
}
