package com.tidyfy.presentation.component;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Ponto que pulsa para indicar que algo está ativo. A cor vem do CSS.
 */
public final class Pulse extends StackPane {

    // Constantes

    /** Classe CSS do componente. */
    private static final String STYLE_CLASS = "pulse";

    /** Classe CSS do anel que se expande. */
    private static final String RING_STYLE_CLASS = "pulse-ring";

    /** Classe CSS do ponto central. */
    private static final String CORE_STYLE_CLASS = "pulse-core";

    /**
     * Cria o ponto e inicia a animação enquanto ele estiver numa cena.
     */
    public Pulse() {
        getStyleClass().add(STYLE_CLASS);
        Circle ring = new Circle(4);
        ring.getStyleClass().add(RING_STYLE_CLASS);
        Circle core = new Circle(4);
        core.getStyleClass().add(CORE_STYLE_CLASS);
        getChildren().addAll(ring, core);
        setMinSize(8, 8);
        setPrefSize(8, 8);
        setMaxSize(8, 8);
        setMouseTransparent(true);

        Duration cycle = Duration.millis(2200);
        ScaleTransition grow = new ScaleTransition(cycle, ring);
        grow.setFromX(1);
        grow.setFromY(1);
        grow.setToX(2.8);
        grow.setToY(2.8);
        grow.setInterpolator(Animations.EASE_OUT);
        FadeTransition fade = new FadeTransition(cycle, ring);
        fade.setFromValue(0.55);
        fade.setToValue(0);
        fade.setInterpolator(Animations.EASE_OUT);
        ParallelTransition pulse = new ParallelTransition(grow, fade);
        pulse.setCycleCount(Animation.INDEFINITE);
        Animations.playWhileInScene(this, pulse);
    }
}
