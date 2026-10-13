package com.tidyfy.presentation.component;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Textura de pontos alinhados que some aos poucos para baixo.
 */
public final class DotGrid extends Region {

    /** Tela onde os pontos são desenhados. */
    private final Canvas canvas = new Canvas();

    // Constantes

    /** Distância, em pixels, entre os centros dos pontos. */
    private static final double STEP = 22;

    /** Raio dos pontos, em pixels. */
    private static final double RADIUS = 1.1;

    /** Classe CSS do componente. */
    private static final String STYLE_CLASS = "dot-grid";

    /**
     * Cria a textura. Ela não captura o mouse.
     */
    public DotGrid() {
        getStyleClass().add(STYLE_CLASS);
        getChildren().add(canvas);
        setMouseTransparent(true);
    }

    /**
     * Redimensiona a tela e redesenha os pontos quando o tamanho muda.
     */
    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        if (canvas.getWidth() != width || canvas.getHeight() != height) {
            canvas.setWidth(width);
            canvas.setHeight(height);
            draw(width, height);
        }
    }

    /**
     * Desenha os pontos, cada linha mais transparente que a anterior.
     *
     * @param width  largura da área, em pixels
     * @param height altura da área, em pixels
     */
    private void draw(double width, double height) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, width, height);
        for (double y = STEP / 2; y < height; y += STEP) {
            g.setFill(Color.rgb(42, 53, 49, Math.max(0, 1 - y / height)));
            for (double x = STEP / 2; x < width; x += STEP) {
                g.fillOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);
            }
        }
    }
}
