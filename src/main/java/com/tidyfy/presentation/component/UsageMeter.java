package com.tidyfy.presentation.component;

import com.tidyfy.presentation.model.HelperKind;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * Uma marca por vaga do tipo; as ocupadas acendem na cor do tipo.
 */
public final class UsageMeter extends HBox {

    /** Quantidade de vagas ocupadas. */
    private final IntegerProperty used = new SimpleIntegerProperty(this, PROPERTY_USED);

    /** Quantidade total de vagas. */
    private final IntegerProperty max = new SimpleIntegerProperty(this, PROPERTY_MAX, HelperKind.LIMIT_PER_TYPE);

    // Constantes

    /** Nome da propriedade das vagas ocupadas. */
    private static final String PROPERTY_USED = "used";

    /** Nome da propriedade do total de vagas. */
    private static final String PROPERTY_MAX = "max";

    /** Classe CSS do medidor. */
    private static final String STYLE_CLASS = "usage-meter";

    /** Classe CSS de cada marca. */
    private static final String TICK_STYLE_CLASS = "tick";

    /** Classe CSS das marcas ocupadas. */
    private static final String ON_STYLE_CLASS = "on";

    /** Modelo do texto de acessibilidade; recebe as vagas ocupadas e o total. */
    private static final String ACCESSIBLE_TEXT = "%d de %d ajudantes criados";

    /**
     * Cria o medidor vazio, com o total de vagas por tipo.
     */
    public UsageMeter() {
        getStyleClass().add(STYLE_CLASS);
        used.addListener((obs, old, now) -> rebuild(old.intValue()));
        max.addListener((obs, old, now) -> rebuild(used.get()));
        rebuild(0);
    }

    /**
     * Recria as marcas. As que acabaram de ser ocupadas aparecem com animação.
     *
     * @param previouslyUsed quantidade de vagas ocupadas antes da mudança
     */
    private void rebuild(int previouslyUsed) {
        getChildren().clear();
        for (int i = 0; i < max.get(); i++) {
            Region tick = new Region();
            tick.getStyleClass().add(TICK_STYLE_CLASS);
            tick.setMinWidth(0);
            tick.setPrefWidth(0);
            tick.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(tick, Priority.ALWAYS);
            if (i < used.get()) {
                tick.getStyleClass().add(ON_STYLE_CLASS);
                if (i >= previouslyUsed) {
                    Animations.growIn(tick, Duration.millis(150 + (i - previouslyUsed) * 35L));
                }
            }
            getChildren().add(tick);
        }
        setAccessibleText(String.format(ACCESSIBLE_TEXT, used.get(), max.get()));
    }

    /**
     * Devolve a quantidade de vagas ocupadas.
     *
     * @return vagas ocupadas
     */
    public int getUsed() {
        return used.get();
    }

    /**
     * Define a quantidade de vagas ocupadas.
     *
     * @param value vagas ocupadas
     */
    public void setUsed(int value) {
        used.set(value);
    }

    /**
     * Devolve a propriedade das vagas ocupadas.
     *
     * @return propriedade das vagas ocupadas
     */
    public IntegerProperty usedProperty() {
        return used;
    }

    /**
     * Devolve o total de vagas.
     *
     * @return total de vagas
     */
    public int getMax() {
        return max.get();
    }

    /**
     * Define o total de vagas.
     *
     * @param value total de vagas
     */
    public void setMax(int value) {
        max.set(value);
    }

    /**
     * Devolve a propriedade do total de vagas.
     *
     * @return propriedade do total de vagas
     */
    public IntegerProperty maxProperty() {
        return max;
    }
}
