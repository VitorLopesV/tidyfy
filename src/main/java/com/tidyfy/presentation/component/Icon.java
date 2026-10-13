package com.tidyfy.presentation.component;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

/**
 * Ícone de traço; a cor vem do CSS ({@code .icon-shape}).
 */
public final class Icon extends StackPane {

    /** Traço do ícone. */
    private final SVGPath shape = new SVGPath();

    /** Grupo que recebe a escala da grade para o tamanho real. */
    private final Group scaled;

    /** Desenho exibido. */
    private final ObjectProperty<Glyph> glyph = new SimpleObjectProperty<>(this, PROPERTY_GLYPH, Glyph.FOLDER);

    /** Lado do ícone, em pixels. */
    private final DoubleProperty size = new SimpleDoubleProperty(this, PROPERTY_SIZE, 18);

    // Constantes

    /** Tamanho da grade em que os desenhos foram feitos. */
    private static final double GRID = 24;

    /** Nome da propriedade do desenho. */
    private static final String PROPERTY_GLYPH = "glyph";

    /** Nome da propriedade do tamanho. */
    private static final String PROPERTY_SIZE = "size";

    /** Classe CSS do componente. */
    private static final String STYLE_CLASS = "icon";

    /** Classe CSS do traço. */
    private static final String SHAPE_STYLE_CLASS = "icon-shape";

    /**
     * Cria o ícone padrão: uma pasta de 18 px.
     */
    public Icon() {
        this(Glyph.FOLDER, 18);
    }

    /**
     * Cria um ícone.
     *
     * @param glyph desenho a exibir
     * @param size  lado do ícone, em pixels
     */
    public Icon(Glyph glyph, double size) {
        getStyleClass().add(STYLE_CLASS);
        shape.getStyleClass().add(SHAPE_STYLE_CLASS);
        Rectangle box = new Rectangle(GRID, GRID, Color.TRANSPARENT);
        scaled = new Group(box, shape);
        getChildren().add(new Group(scaled));
        setMouseTransparent(true);
        this.glyph.addListener((obs, old, now) -> update());
        this.size.addListener((obs, old, now) -> update());
        this.glyph.set(glyph);
        this.size.set(size);
        update();
    }

    /**
     * Aplica o desenho e o tamanho atuais.
     */
    private void update() {
        shape.setContent(glyph.get().path());
        double factor = size.get() / GRID;
        scaled.getTransforms().setAll(new Scale(factor, factor, 0, 0));
        setMinSize(size.get(), size.get());
        setPrefSize(size.get(), size.get());
        setMaxSize(size.get(), size.get());
    }

    /**
     * Devolve o desenho exibido.
     *
     * @return desenho atual
     */
    public Glyph getGlyph() {
        return glyph.get();
    }

    /**
     * Define o desenho exibido.
     *
     * @param value novo desenho
     */
    public void setGlyph(Glyph value) {
        glyph.set(value);
    }

    /**
     * Devolve a propriedade do desenho.
     *
     * @return propriedade do desenho
     */
    public ObjectProperty<Glyph> glyphProperty() {
        return glyph;
    }

    /**
     * Devolve o lado do ícone.
     *
     * @return lado, em pixels
     */
    public double getSize() {
        return size.get();
    }

    /**
     * Define o lado do ícone.
     *
     * @param value lado, em pixels
     */
    public void setSize(double value) {
        size.set(value);
    }

    /**
     * Devolve a propriedade do tamanho.
     *
     * @return propriedade do tamanho
     */
    public DoubleProperty sizeProperty() {
        return size;
    }
}
