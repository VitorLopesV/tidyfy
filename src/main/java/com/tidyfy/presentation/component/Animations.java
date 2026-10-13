package com.tidyfy.presentation.component;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Movimentos curtos que dão vida às telas sem atrapalhar a leitura.
 */
public final class Animations {

    // Constantes

    /** Começa rápido e desacelera até o ponto final. */
    public static final Interpolator EASE_OUT = Interpolator.SPLINE(0.2, 0.8, 0.2, 1);

    /** Desacelera passando um pouco do ponto final e volta (efeito de "mola"). */
    public static final Interpolator BACK_OUT = new Interpolator() {
        @Override
        protected double curve(double t) {
            double overshoot = 1.70158;
            double u = t - 1;
            return 1 + (overshoot + 1) * u * u * u + overshoot * u * u;
        }
    };

    /**
     * Impede a criação de instâncias: a classe só tem métodos estáticos.
     */
    private Animations() {
    }

    /**
     * Faz o nó surgir subindo de leve, depois do atraso informado.
     *
     * @param node  nó a animar
     * @param delay tempo de espera antes de começar
     */
    public static void riseIn(Node node, Duration delay) {
        node.setOpacity(0);
        node.setTranslateY(14);
        FadeTransition fade = new FadeTransition(Duration.millis(450), node);
        fade.setToValue(1);
        TranslateTransition move = new TranslateTransition(Duration.millis(550), node);
        move.setToY(0);
        move.setInterpolator(EASE_OUT);
        ParallelTransition rise = new ParallelTransition(fade, move);
        rise.setDelay(delay);
        rise.play();
    }

    /**
     * Faz o nó surgir subindo um pouco, sem atraso.
     *
     * @param node nó a animar
     */
    public static void fadeUpIn(Node node) {
        node.setOpacity(0);
        node.setTranslateY(8);
        FadeTransition fade = new FadeTransition(Duration.millis(300), node);
        fade.setToValue(1);
        TranslateTransition move = new TranslateTransition(Duration.millis(300), node);
        move.setToY(0);
        move.setInterpolator(EASE_OUT);
        new ParallelTransition(fade, move).play();
    }

    /**
     * Faz um diálogo surgir subindo e crescendo de leve.
     *
     * @param node raiz do diálogo
     */
    public static void dialogIn(Node node) {
        node.setOpacity(0);
        node.setTranslateY(12);
        node.setScaleX(0.985);
        node.setScaleY(0.985);
        FadeTransition fade = new FadeTransition(Duration.millis(380), node);
        fade.setToValue(1);
        TranslateTransition move = new TranslateTransition(Duration.millis(450), node);
        move.setToY(0);
        move.setInterpolator(EASE_OUT);
        ScaleTransition grow = new ScaleTransition(Duration.millis(450), node);
        grow.setToX(1);
        grow.setToY(1);
        grow.setInterpolator(EASE_OUT);
        new ParallelTransition(fade, move, grow).play();
    }

    /**
     * Leva a opacidade do nó até o valor informado.
     *
     * @param node       nó a animar
     * @param opacity    opacidade final, de 0 a 1
     * @param onFinished ação executada ao terminar; pode ser {@code null}
     */
    public static void fadeTo(Node node, double opacity, Runnable onFinished) {
        FadeTransition fade = new FadeTransition(Duration.millis(220), node);
        fade.setToValue(opacity);
        if (onFinished != null) {
            fade.setOnFinished(event -> onFinished.run());
        }
        fade.play();
    }

    /**
     * Faz o nó crescer de leve até o tamanho normal, com efeito de mola.
     *
     * @param node nó a animar
     */
    public static void pop(Node node) {
        ScaleTransition pop = new ScaleTransition(Duration.millis(320), node);
        pop.setFromX(0.82);
        pop.setFromY(0.82);
        pop.setToX(1);
        pop.setToY(1);
        pop.setInterpolator(BACK_OUT);
        pop.play();
    }

    /**
     * Faz o nó crescer na vertical enquanto aparece, depois do atraso informado.
     *
     * @param node  nó a animar
     * @param delay tempo de espera antes de começar
     */
    public static void growIn(Node node, Duration delay) {
        node.setScaleY(0.15);
        node.setOpacity(0.3);
        ScaleTransition grow = new ScaleTransition(Duration.millis(500), node);
        grow.setToY(1);
        grow.setInterpolator(EASE_OUT);
        FadeTransition fade = new FadeTransition(Duration.millis(500), node);
        fade.setToValue(1);
        ParallelTransition both = new ParallelTransition(grow, fade);
        both.setDelay(delay);
        both.play();
    }

    /**
     * Levanta o nó um pouco enquanto o mouse está sobre ele.
     *
     * @param node nó a animar
     */
    public static void hoverLift(Node node) {
        TranslateTransition lift = new TranslateTransition(Duration.millis(260), node);
        lift.setInterpolator(EASE_OUT);
        node.hoverProperty().addListener((obs, was, hovering) -> {
            lift.stop();
            lift.setToY(hovering ? -3 : 0);
            lift.playFromStart();
        });
    }

    /**
     * Inclina e aumenta o alvo enquanto o mouse está sobre o gatilho.
     *
     * @param target nó que se move
     * @param trigger nó que reage ao mouse
     */
    public static void hoverTilt(Node target, Node trigger) {
        RotateTransition rotate = new RotateTransition(Duration.millis(380), target);
        rotate.setInterpolator(BACK_OUT);
        ScaleTransition scale = new ScaleTransition(Duration.millis(380), target);
        scale.setInterpolator(BACK_OUT);
        trigger.hoverProperty().addListener((obs, was, hovering) -> {
            rotate.stop();
            scale.stop();
            rotate.setToAngle(hovering ? -7 : 0);
            scale.setToX(hovering ? 1.07 : 1);
            scale.setToY(hovering ? 1.07 : 1);
            rotate.playFromStart();
            scale.playFromStart();
        });
    }

    /**
     * Gira o alvo até o ângulo informado enquanto o mouse está sobre o gatilho.
     *
     * @param target nó que gira
     * @param trigger nó que reage ao mouse
     * @param angle ângulo, em graus, com o mouse sobre o gatilho
     */
    public static void rotateOnHover(Node target, Node trigger, double angle) {
        RotateTransition rotate = new RotateTransition(Duration.millis(400), target);
        rotate.setInterpolator(BACK_OUT);
        trigger.hoverProperty().addListener((obs, was, hovering) -> {
            rotate.stop();
            rotate.setToAngle(hovering ? angle : 0);
            rotate.playFromStart();
        });
    }

    /**
     * Balança o nó de um lado para o outro, perdendo força até parar.
     *
     * @param node nó a balançar
     */
    public static void shake(Node node) {
        Timeline shake = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(node.rotateProperty(), 0)),
                new KeyFrame(Duration.millis(140), new KeyValue(node.rotateProperty(), -9)),
                new KeyFrame(Duration.millis(280), new KeyValue(node.rotateProperty(), 7)),
                new KeyFrame(Duration.millis(420), new KeyValue(node.rotateProperty(), -4)),
                new KeyFrame(Duration.millis(560), new KeyValue(node.rotateProperty(), 2)),
                new KeyFrame(Duration.millis(700), new KeyValue(node.rotateProperty(), 0)));
        shake.setDelay(Duration.millis(300));
        shake.play();
    }

    /**
     * Faz o nó flutuar para sempre enquanto ele estiver numa cena.
     *
     * @param node nó a animar
     */
    public static void floating(Node node) {
        TranslateTransition bob = new TranslateTransition(Duration.millis(2250), node);
        bob.setFromY(0);
        bob.setToY(-6);
        bob.setAutoReverse(true);
        bob.setCycleCount(Animation.INDEFINITE);
        bob.setInterpolator(Interpolator.EASE_BOTH);
        playWhileInScene(node, bob);
    }

    /**
     * Executa a animação enquanto o nó estiver numa cena e a para quando ele sair.
     *
     * @param node      nó que define quando a animação roda
     * @param animation animação a controlar
     */
    static void playWhileInScene(Node node, Animation animation) {
        if (node.getScene() != null) {
            animation.play();
        }
        node.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                animation.stop();
            } else {
                animation.play();
            }
        });
    }
}
