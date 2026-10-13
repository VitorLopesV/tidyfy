package com.tidyfy.presentation.component;

import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Region;

/**
 * Operações utilitárias sobre nós do JavaFX.
 */
public final class Nodes {

    /**
     * Impede a criação de instâncias: a classe só tem métodos estáticos.
     */
    private Nodes() {
    }

    /**
     * Mostra ou esconde o nó, liberando o espaço dele no layout quando escondido.
     *
     * @param node  nó a mostrar ou esconder
     * @param shown {@code true} para mostrar
     */
    public static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    /**
     * Informa se o alvo é o contêiner ou um descendente dele.
     *
     * @param target    alvo a testar (ex.: o alvo de um evento); só nós podem estar dentro do contêiner
     * @param container nó que pode conter o alvo
     * @return {@code true} quando o alvo está dentro do contêiner, ou é ele mesmo
     */
    public static boolean isInside(Object target, Node container) {
        for (Node node = target instanceof Node start ? start : null; node != null; node = node.getParent()) {
            if (node == container) {
                return true;
            }
        }
        return false;
    }

    /**
     * Faz o conteúdo (graphic) de um botão ocupar a largura, e opcionalmente a altura, do botão.
     *
     * @param labeled botão cujo conteúdo será esticado
     * @param height  {@code true} para esticar também a altura
     */
    public static void stretchGraphic(Labeled labeled, boolean height) {
        if (!(labeled.getGraphic() instanceof Region graphic)) {
            return;
        }
        graphic.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> labeled.getWidth() - labeled.getInsets().getLeft() - labeled.getInsets().getRight(),
                labeled.widthProperty(), labeled.insetsProperty()));
        if (height) {
            graphic.prefHeightProperty().bind(Bindings.createDoubleBinding(
                    () -> labeled.getHeight() - labeled.getInsets().getTop() - labeled.getInsets().getBottom(),
                    labeled.heightProperty(), labeled.insetsProperty()));
        }
    }
}
