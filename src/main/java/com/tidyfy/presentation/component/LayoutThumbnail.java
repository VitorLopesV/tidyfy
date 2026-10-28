package com.tidyfy.presentation.component;

import com.tidyfy.domain.DesktopGrid;
import com.tidyfy.domain.DesktopIcon;
import com.tidyfy.domain.DesktopLayout;
import com.tidyfy.domain.DesktopLayoutPlanner;
import com.tidyfy.domain.IconPosition;
import java.util.List;
import java.util.Objects;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Miniatura de uma tela mostrando onde os ícones ficam em um layout. As posições vêm do próprio
 * {@link DesktopLayoutPlanner}, aplicado a uma tela de exemplo, então a miniatura sempre mostra o que o
 * ajudante de fato faz.
 */
public final class LayoutThumbnail extends StackPane {

    /** Layout mostrado. */
    private final DesktopLayout layout;

    /** Área onde os ícones são desenhados. */
    private final Pane screen = new Pane();

    // Constantes

    /** Classe CSS do componente. */
    private static final String STYLE_CLASS = "layout-thumb";

    /** Classe CSS da tela. */
    private static final String SCREEN_STYLE_CLASS = "layout-thumb-screen";

    /** Classe CSS de cada ícone. */
    private static final String ICON_STYLE_CLASS = "layout-thumb-icon";

    /** Largura da tela de exemplo, na unidade da grade. */
    private static final int SAMPLE_WIDTH = 1600;

    /** Altura da tela de exemplo, na unidade da grade. */
    private static final int SAMPLE_HEIGHT = 1000;

    /** Lado da célula da tela de exemplo. */
    private static final int SAMPLE_CELL = 200;

    /** Parte da célula ocupada pelo ícone desenhado. */
    private static final double ICON_FILL = 0.55;

    /** Nome do parâmetro do layout, usado na mensagem de valor nulo. */
    private static final String LAYOUT_NAME = "layout";

    /** Mensagem quando a largura não é positiva. */
    private static final String INVALID_WIDTH_MESSAGE = "width must be positive";

    /** Ícones de exemplo: nomes que cobrem atalhos, documentos, imagens, executável, compactado e outros. */
    private static final List<String> SAMPLE_NAMES = List.of("Navegador", "Editor", "Jogos", "Contrato.pdf",
            "Notas.txt", "Foto 1.png", "Foto 2.jpg", "Foto 3.png", "Instalador.exe", "Backup.zip", "Dados.xyz",
            "Video.mp4");

    /**
     * Cria a miniatura.
     *
     * @param layout o layout a mostrar
     * @param width  largura da miniatura, em pixels; a altura segue a proporção da tela de exemplo
     * @throws NullPointerException     se {@code layout} for nulo
     * @throws IllegalArgumentException se {@code width} não for positiva
     */
    public LayoutThumbnail(DesktopLayout layout, double width) {
        this.layout = Objects.requireNonNull(layout, LAYOUT_NAME);
        if (width <= 0) {
            throw new IllegalArgumentException(INVALID_WIDTH_MESSAGE);
        }
        double scale = width / SAMPLE_WIDTH;
        double height = SAMPLE_HEIGHT * scale;
        getStyleClass().add(STYLE_CLASS);
        screen.getStyleClass().add(SCREEN_STYLE_CLASS);
        screen.setMinSize(width, height);
        screen.setPrefSize(width, height);
        screen.setMaxSize(width, height);
        screen.getChildren().addAll(icons(scale));
        getChildren().add(screen);
    }

    /**
     * Devolve o layout mostrado.
     *
     * @return o layout
     */
    public DesktopLayout desktopLayout() {
        return layout;
    }

    /**
     * Devolve a quantidade de ícones desenhados.
     *
     * @return quantidade de ícones na miniatura
     */
    public int iconCount() {
        return screen.getChildren().size();
    }

    /**
     * Desenha os ícones de exemplo nas posições calculadas pelo planejador.
     *
     * @param scale escala da tela de exemplo para pixels
     * @return um quadrado por ícone posicionado
     */
    private List<Region> icons(double scale) {
        DesktopGrid grid = new DesktopGrid(0, 0, SAMPLE_WIDTH, SAMPLE_HEIGHT, SAMPLE_CELL, SAMPLE_CELL);
        List<DesktopIcon> sample = SAMPLE_NAMES.stream()
                .map(name -> new DesktopIcon(name, new IconPosition(0, 0))).toList();
        double cell = SAMPLE_CELL * scale;
        double side = cell * ICON_FILL;
        double margin = (cell - side) / 2;
        return new DesktopLayoutPlanner().plan(sample, grid, layout).placements().stream().map(placed -> {
            Region icon = new Region();
            icon.getStyleClass().add(ICON_STYLE_CLASS);
            icon.setMinSize(side, side);
            icon.setPrefSize(side, side);
            icon.setMaxSize(side, side);
            icon.setLayoutX(placed.position().x() * scale + margin);
            icon.setLayoutY(placed.position().y() * scale + margin);
            return icon;
        }).toList();
    }
}
