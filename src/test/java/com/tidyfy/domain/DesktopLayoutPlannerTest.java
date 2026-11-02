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

    /** Área de 3 colunas por 2 linhas, células de 100 x 100 e margem de 20 nas bordas, a partir da origem. */
    private static final DesktopGrid SMALL = new DesktopGrid(0, 0, 340, 240, 100, 100);

    /** Margem das bordas para células de 100 x 100. */
    private static final int MARGIN = 20;

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

        assertEquals(List.of(at("a", 20, 20), at("b", 20, 120), at("c", 120, 20), at("d", 120, 120), at("e", 220, 20)),
                plan.placements());
        assertTrue(plan.leftOut().isEmpty());
    }

    @Test
    void rightFillsColumnsFromTheRightEdge() {
        DesktopLayoutPlan plan = planner.plan(icons("a", "b", "c", "d", "e"), SMALL, DesktopLayout.RIGHT);

        assertEquals(List.of(at("a", 220, 20), at("b", 220, 120), at("c", 120, 20), at("d", 120, 120), at("e", 20, 20)),
                plan.placements());
    }

    @Test
    void topFillsRowsFromTheTopEdge() {
        DesktopLayoutPlan plan = planner.plan(icons("a", "b", "c", "d", "e"), SMALL, DesktopLayout.TOP);

        assertEquals(List.of(at("a", 20, 20), at("b", 120, 20), at("c", 220, 20), at("d", 20, 120), at("e", 120, 120)),
                plan.placements());
    }

    @Test
    void bottomFillsRowsFromTheBottomEdge() {
        DesktopLayoutPlan plan = planner.plan(icons("a", "b", "c", "d", "e"), SMALL, DesktopLayout.BOTTOM);

        assertEquals(List.of(at("a", 20, 120), at("b", 120, 120), at("c", 220, 120), at("d", 20, 20), at("e", 120, 20)),
                plan.placements());
    }

    @Test
    void bordersGoClockwiseAndKeepTheCenterFree() {
        DesktopGrid grid = new DesktopGrid(0, 0, 440, 340, 100, 100);

        DesktopLayoutPlan plan = planner.plan(
                icons("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"), grid, DesktopLayout.BORDERS);

        assertEquals(List.of(at("a", 20, 20), at("b", 120, 20), at("c", 220, 20), at("d", 320, 20),
                at("e", 320, 120), at("f", 320, 220),
                at("g", 220, 220), at("h", 120, 220), at("i", 20, 220),
                at("j", 20, 120)), plan.placements());
        assertEquals(icons("k", "l"), plan.leftOut());
        for (DesktopIcon placed : plan.placements()) {
            assertTrue(placed.position().x() == 20 || placed.position().x() == 320
                    || placed.position().y() == 20 || placed.position().y() == 220);
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
        DesktopGrid grid = new DesktopGrid(0, 0, 340, 140, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("a", "b", "c", "d"), grid, DesktopLayout.BORDERS);

        assertEquals(List.of(at("a", 20, 20), at("b", 120, 20), at("c", 220, 20)), plan.placements());
        assertEquals(icons("d"), plan.leftOut());
    }

    @Test
    void anchorsTheGridToTheEdgeOfTheLayoutWhenTheAreaIsNotAMultipleOfTheCell() {
        DesktopGrid grid = new DesktopGrid(1000, 10, 390, 290, 100, 100);

        assertEquals(at("a", 1270, 30), planner.plan(icons("a"), grid, DesktopLayout.RIGHT).placements().get(0));
        assertEquals(at("a", 1020, 180), planner.plan(icons("a"), grid, DesktopLayout.BOTTOM).placements().get(0));
        assertEquals(at("a", 1020, 30), planner.plan(icons("a"), grid, DesktopLayout.LEFT).placements().get(0));
        assertEquals(at("a", 1020, 30), planner.plan(icons("a"), grid, DesktopLayout.TOP).placements().get(0));
    }

    @Test
    void bordersTouchTheRightAndBottomEdgesWhenTheAreaIsNotAMultipleOfTheCell() {
        DesktopGrid grid = new DesktopGrid(1000, 10, 390, 290, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("a", "b", "c", "d", "e", "f"), grid, DesktopLayout.BORDERS);

        assertEquals(List.of(at("a", 1020, 30), at("b", 1120, 30), at("c", 1270, 30),
                at("d", 1270, 180), at("e", 1120, 180), at("f", 1020, 180)), plan.placements());
    }

    @Test
    void honorsAnOffsetThatIsNegative() {
        DesktopGrid grid = new DesktopGrid(-1920, 0, 340, 240, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("a", "b"), grid, DesktopLayout.TOP);

        assertEquals(List.of(at("a", -1900, 20), at("b", -1800, 20)), plan.placements());
    }

    @Test
    void byTypePutsEachGroupInItsOwnColumnFromTheLeft() {
        DesktopGrid grid = new DesktopGrid(0, 0, 540, 240, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("Steam", "App.lnk", "foto.PNG", "doc.pdf", "x.exe", "a.xyz",
                "b.jpg"), grid, DesktopLayout.BY_TYPE_LEFT);

        assertEquals(List.of(at("App.lnk", 20, 20), at("Steam", 20, 120),
                at("doc.pdf", 120, 20),
                at("x.exe", 220, 20),
                at("b.jpg", 320, 20), at("foto.PNG", 320, 120),
                at("a.xyz", 420, 20)), plan.placements());
        assertTrue(plan.leftOut().isEmpty());
    }

    @Test
    void byTypeSkipsEmptyGroupsAndTreatsUrlAsShortcut() {
        DesktopGrid grid = new DesktopGrid(0, 0, 540, 240, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("video.mp4", "site.url"), grid, DesktopLayout.BY_TYPE_LEFT);

        assertEquals(List.of(at("site.url", 20, 20), at("video.mp4", 120, 20)), plan.placements());
    }

    @Test
    void byTypeContinuesInTheNextColumnWhenAGroupHasMoreIconsThanRows() {
        DesktopGrid grid = new DesktopGrid(0, 0, 440, 240, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("A", "B", "C", "foto.jpg"), grid, DesktopLayout.BY_TYPE_LEFT);

        assertEquals(List.of(at("A", 20, 20), at("B", 20, 120), at("C", 120, 20), at("foto.jpg", 220, 20)),
                plan.placements());
    }

    @Test
    void byTypeReportsWhatDoesNotFit() {
        DesktopGrid grid = new DesktopGrid(0, 0, 140, 240, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("A", "B", "C", "doc.pdf"), grid, DesktopLayout.BY_TYPE_LEFT);

        assertEquals(List.of(at("A", 20, 20), at("B", 20, 120)), plan.placements());
        assertEquals(icons("C", "doc.pdf"), plan.leftOut());
    }

    @Test
    void byTypeUsesTheCategoriesGiven() {
        FileCategory music = new FileCategory("Música", "Música", Set.of(new FileExtension("mp3")));
        DesktopLayoutPlanner custom = new DesktopLayoutPlanner(List.of(music));
        DesktopGrid grid = new DesktopGrid(0, 0, 340, 140, 100, 100);

        DesktopLayoutPlan plan = custom.plan(icons("a.txt", "b.mp3"), grid, DesktopLayout.BY_TYPE_LEFT);

        assertEquals(List.of(at("b.mp3", 20, 20), at("a.txt", 120, 20)), plan.placements());
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

        assertEquals(List.of(at("Chrome", 20, 20), at("Chrome", 120, 20)), plan.placements());
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
                int margin = Math.min(grid.cellWidth(), grid.cellHeight()) / 5;
                assertTrue(position.x() >= grid.left() + margin
                        && position.x() + grid.cellWidth() <= grid.left() + grid.width() - margin, layout.name());
                assertTrue(position.y() >= grid.top() + margin
                        && position.y() + grid.cellHeight() <= grid.top() + grid.height() - margin, layout.name());
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

    /** Reproduz a issue 84: nenhum layout pode encostar os ícones nas bordas da área visível. */
    @Test
    void everyLayoutKeepsAMarginFromTheEdgesOfTheVisibleArea() {
        DesktopGrid grid = new DesktopGrid(1941, 0, 2560, 1392, 75, 87);
        List<DesktopIcon> many = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            many.add(icon("Item " + i + (i % 2 == 0 ? ".pdf" : ".png")));
        }
        int margin = 75 / 5;
        for (DesktopLayout layout : DesktopLayout.values()) {
            for (DesktopIcon placed : planner.plan(many, grid, layout).placements()) {
                IconPosition p = placed.position();
                assertTrue(p.x() >= grid.left() + margin, layout.name());
                assertTrue(p.y() >= grid.top() + margin, layout.name());
                assertTrue(p.x() + grid.cellWidth() <= grid.left() + grid.width() - margin, layout.name());
                assertTrue(p.y() + grid.cellHeight() <= grid.top() + grid.height() - margin, layout.name());
            }
        }
    }

    @Test
    void firstIconOfEachLayoutIsAwayFromItsCornerByTheMargin() {
        List<DesktopIcon> one = icons("a");

        assertEquals(at("a", MARGIN, MARGIN), planner.plan(one, SMALL, DesktopLayout.LEFT).placements().get(0));
        assertEquals(at("a", 220, MARGIN), planner.plan(one, SMALL, DesktopLayout.RIGHT).placements().get(0));
        assertEquals(at("a", MARGIN, MARGIN), planner.plan(one, SMALL, DesktopLayout.TOP).placements().get(0));
        assertEquals(at("a", MARGIN, 120), planner.plan(one, SMALL, DesktopLayout.BOTTOM).placements().get(0));
        assertEquals(at("a", MARGIN, MARGIN), planner.plan(one, SMALL, DesktopLayout.BORDERS).placements().get(0));
        assertEquals(at("a.pdf", MARGIN, MARGIN),
                planner.plan(icons("a.pdf"), SMALL, DesktopLayout.BY_TYPE_LEFT).placements().get(0));
    }

    @Test
    void marginTakesCellsAwayWhenTheAreaIsJustBigEnough() {
        DesktopGrid grid = new DesktopGrid(0, 0, 300, 200, 100, 100);

        DesktopLayoutPlan plan = planner.plan(icons("a", "b", "c", "d", "e", "f"), grid, DesktopLayout.TOP);

        assertEquals(List.of(at("a", 20, 20), at("b", 120, 20)), plan.placements());
        assertEquals(4, plan.leftOut().size());
    }

    @Test
    void marginScalesWithTheSmallerSideOfTheCell() {
        DesktopGrid grid = new DesktopGrid(0, 0, 1000, 1000, 150, 100);

        DesktopLayoutPlan plan = planner.plan(icons("a"), grid, DesktopLayout.LEFT);

        assertEquals(List.of(at("a", 20, 20)), plan.placements());
    }

    @Test
    void placedIconsKeepTheirNamesAndTheOnesLeftOutKeepTheirPosition() {
        DesktopGrid grid = new DesktopGrid(0, 0, 140, 140, 100, 100);
        DesktopIcon first = at("a", 5, 6);
        DesktopIcon second = at("b", 7, 8);

        DesktopLayoutPlan plan = planner.plan(List.of(second, first), grid, DesktopLayout.LEFT);

        assertEquals(List.of(at("a", 20, 20)), plan.placements());
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
