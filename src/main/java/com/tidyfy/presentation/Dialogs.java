package com.tidyfy.presentation;

import com.tidyfy.presentation.component.Animations;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Abre diálogos modais sem a moldura do Windows, com sombra e arrastáveis pelo cabeçalho.
 */
final class Dialogs {

    // Constantes

    /** Espaço, em pixels, em volta do diálogo para a sombra aparecer. */
    private static final double SHADOW_SPACE = 24;

    /** Classe CSS da camada que envolve o diálogo. */
    private static final String SHELL_STYLE_CLASS = "dialog-shell";

    /** Seletor do nó pelo qual o diálogo é arrastado. */
    private static final String DRAG_HANDLE_SELECTOR = ".drag-handle";

    /**
     * Impede a criação de instâncias: a classe só tem métodos estáticos.
     */
    private Dialogs() {
    }

    /**
     * Mostra o diálogo e espera ele ser fechado. Esc fecha o diálogo; o nó com a classe {@code drag-handle}
     * o arrasta.
     *
     * @param owner janela dona do diálogo
     * @param view  conteúdo do diálogo
     * @param title título da janela
     */
    static void showModal(Window owner, Parent view, String title) {
        Rectangle2D screen = screenOf(owner);
        if (view instanceof Region region && region.getPrefHeight() > screen.getHeight() - 2 * SHADOW_SPACE) {
            region.setPrefHeight(screen.getHeight() - 2 * SHADOW_SPACE);
        }
        StackPane shell = new StackPane(view);
        shell.getStyleClass().add(SHELL_STYLE_CLASS);
        shell.setPadding(new Insets(SHADOW_SPACE));

        Scene scene = new Scene(shell, Color.TRANSPARENT);
        scene.getStylesheets().setAll(owner.getScene().getStylesheets());

        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(title);
        stage.setScene(scene);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                stage.hide();
            }
        });
        Node handle = view.lookup(DRAG_HANDLE_SELECTOR);
        if (handle != null) {
            makeDraggable(handle, stage);
        }
        stage.setOnShown(event -> {
            centerOn(owner, stage, screen);
            Animations.dialogIn(view);
        });
        stage.showAndWait();
    }

    /**
     * Encontra a área útil da tela onde a janela dona está.
     *
     * @param owner janela dona do diálogo
     * @return área visível da tela, sem a barra de tarefas
     */
    private static Rectangle2D screenOf(Window owner) {
        return Screen.getScreensForRectangle(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight())
                .stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
    }

    /**
     * Centraliza o diálogo sobre a janela dona, sem sair da tela.
     *
     * @param owner  janela dona do diálogo
     * @param stage  janela do diálogo
     * @param screen área útil da tela
     */
    private static void centerOn(Window owner, Stage stage, Rectangle2D screen) {
        double x = owner.getX() + (owner.getWidth() - stage.getWidth()) / 2;
        double y = owner.getY() + (owner.getHeight() - stage.getHeight()) / 2;
        stage.setX(Math.max(screen.getMinX(), Math.min(x, screen.getMaxX() - stage.getWidth())));
        stage.setY(Math.max(screen.getMinY(), Math.min(y, screen.getMaxY() - stage.getHeight())));
    }

    /**
     * Faz a janela acompanhar o mouse quando o nó é arrastado.
     *
     * @param handle nó que serve de alça
     * @param stage  janela a mover
     */
    private static void makeDraggable(Node handle, Stage stage) {
        double[] grab = new double[2];
        handle.setOnMousePressed(event -> {
            grab[0] = event.getScreenX() - stage.getX();
            grab[1] = event.getScreenY() - stage.getY();
        });
        handle.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - grab[0]);
            stage.setY(event.getScreenY() - grab[1]);
        });
    }
}
