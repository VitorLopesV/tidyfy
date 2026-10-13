package com.tidyfy.presentation.component;

import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;

/**
 * Marca do Tidyfy: uma lista arrumada com um ponto de "feito".
 */
public final class Logo extends StackPane {

    // Constantes

    /** Tamanho da grade em que a marca foi desenhada. */
    private static final double GRID = 34;

    /** Cor do fundo da marca (cor de identidade, fora do tema da interface). */
    private static final String TILE_COLOR = "#E9EFEC";

    /** Cor das linhas da marca. */
    private static final String LINES_COLOR = "#0D1210";

    /** Cor do ponto de "feito". */
    private static final String DOT_COLOR = "#11A37C";

    /** Fundo da marca. */
    private static final Color TILE = Color.web(TILE_COLOR);

    /** Linhas da marca. */
    private static final Color LINES = Color.web(LINES_COLOR);

    /** Ponto de "feito". */
    private static final Color DOT = Color.web(DOT_COLOR);

    /**
     * Cria a marca no tamanho padrão.
     */
    public Logo() {
        this(GRID);
    }

    /**
     * Cria a marca quadrada com o tamanho informado.
     *
     * @param size lado da marca, em pixels
     */
    public Logo(double size) {
        double k = size / GRID;
        Rectangle tile = new Rectangle(size, size, TILE);
        tile.setArcWidth(20 * k);
        tile.setArcHeight(20 * k);
        Group mark = new Group(tile,
                line(10, 12, 24, k),
                line(10, 17, 20, k),
                line(10, 22, 16, k),
                new Circle(23.5 * k, 22 * k, 2.4 * k, DOT));
        getChildren().add(mark);
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
    }

    /**
     * Cria uma linha horizontal da marca.
     *
     * @param startX coordenada x inicial, na grade
     * @param y      coordenada y, na grade
     * @param endX   coordenada x final, na grade
     * @param k      fator de escala da grade para o tamanho real
     * @return a linha pronta
     */
    private static Line line(double startX, double y, double endX, double k) {
        Line line = new Line(startX * k, y * k, endX * k, y * k);
        line.setStroke(LINES);
        line.setStrokeWidth(2.4 * k);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        return line;
    }

    /**
     * Desenha a marca numa imagem com fundo transparente, para usar como ícone de janela.
     *
     * @param size lado da imagem, em pixels
     * @return a imagem da marca
     */
    public static Image image(double size) {
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return new Logo(size).snapshot(parameters, null);
    }
}
