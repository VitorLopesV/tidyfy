package com.tidyfy.presentation.component;

import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Interruptor ligado/desligado: um ToggleButton com trilho e botão deslizante.
 */
public final class ToggleSwitch extends ToggleButton {

    /** Botão que desliza sobre o trilho. */
    private final StackPane knob = new StackPane();

    /** Animação que leva o botão de um lado ao outro. */
    private final TranslateTransition slide = new TranslateTransition(Duration.millis(320), knob);

    // Constantes

    /** Distância, em pixels, que o botão percorre ao ligar. */
    private static final double TRAVEL = 18;

    /** Classe CSS do interruptor. */
    private static final String STYLE_CLASS = "toggle-switch";

    /** Classe CSS do botão deslizante. */
    private static final String KNOB_STYLE_CLASS = "knob";

    /** Classe CSS do trilho. */
    private static final String TRACK_STYLE_CLASS = "track";

    /**
     * Cria um interruptor desligado.
     */
    public ToggleSwitch() {
        getStyleClass().setAll(STYLE_CLASS);
        knob.getStyleClass().add(KNOB_STYLE_CLASS);
        StackPane track = new StackPane(knob);
        track.getStyleClass().add(TRACK_STYLE_CLASS);
        StackPane.setAlignment(knob, Pos.CENTER_LEFT);
        setGraphic(track);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        slide.setInterpolator(Animations.BACK_OUT);
        selectedProperty().addListener((obs, was, on) -> moveKnob(on));
    }

    /**
     * Leva o botão para o lado correspondente ao estado. Sem cena, a troca é imediata.
     *
     * @param on {@code true} para o lado ligado
     */
    private void moveKnob(boolean on) {
        double target = on ? TRAVEL : 0;
        slide.stop();
        if (getScene() == null) {
            knob.setTranslateX(target);
            return;
        }
        slide.setToX(target);
        slide.playFromStart();
    }
}
