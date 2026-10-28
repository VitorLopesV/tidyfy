package com.tidyfy.presentation.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.DesktopLayout;
import com.tidyfy.presentation.JavaFx;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

/**
 * Testa a miniatura de layout da área de trabalho.
 */
class LayoutThumbnailTest {

    @Test
    void drawsOneSquarePerSampleIconForEveryLayout() {
        JavaFx.run(() -> {
            for (DesktopLayout layout : DesktopLayout.values()) {
                LayoutThumbnail thumbnail = new LayoutThumbnail(layout, 150);

                assertEquals(layout, thumbnail.desktopLayout());
                assertEquals(12, thumbnail.iconCount());
            }
        });
    }

    @Test
    void differentLayoutsPlaceTheIconsDifferently() {
        JavaFx.run(() -> {
            LayoutThumbnail left = new LayoutThumbnail(DesktopLayout.LEFT, 160);
            LayoutThumbnail right = new LayoutThumbnail(DesktopLayout.RIGHT, 160);

            assertTrue(minX(left) < minX(right));
        });
    }

    @Test
    void rejectsNonPositiveWidthAndNullLayout() {
        assertThrows(IllegalArgumentException.class, () -> new LayoutThumbnail(DesktopLayout.LEFT, 0));
        assertThrows(NullPointerException.class, () -> new LayoutThumbnail(null, 100));
    }

    private static double minX(LayoutThumbnail thumbnail) {
        Pane screen = (Pane) thumbnail.getChildren().get(0);
        return screen.getChildren().stream().mapToDouble(node -> node.getLayoutX()).min().orElseThrow();
    }
}
