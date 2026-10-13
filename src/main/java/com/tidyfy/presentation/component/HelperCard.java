package com.tidyfy.presentation.component;

import com.tidyfy.presentation.model.HelperItem;
import com.tidyfy.presentation.model.HelperKind;
import com.tidyfy.presentation.model.SchedulePreview;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Cartão compacto de um ajudante na lista. Clicar em qualquer parte do cartão marca ou desmarca a seleção.
 */
public final class HelperCard extends VBox {

    /** Caixa de seleção do cartão. */
    @FXML private CheckBox selectBox;

    /** Fundo do ícone do tipo. */
    @FXML private StackPane kindBadge;

    /** Ícone do tipo. */
    @FXML private Icon kindIcon;

    /** Nome do ajudante. */
    @FXML private Label nameLabel;

    /** Nome curto do tipo. */
    @FXML private Label kindLabel;

    /** Interruptor que liga e desliga o ajudante. */
    @FXML private ToggleSwitch enabledSwitch;

    /** Ícone do alvo (pasta ou lixeira). */
    @FXML private Icon targetIcon;

    /** Nome do alvo. */
    @FXML private Label targetName;

    /** Detalhe do alvo: o caminho da pasta ou o que acontece com a lixeira. */
    @FXML private Label targetDetail;

    /** Frequência em dias. */
    @FXML private Label everyLabel;

    /** Horários de execução. */
    @FXML private HBox timeChips;

    /** Bloco da próxima execução. */
    @FXML private HBox nextRunBox;

    /** Texto da próxima execução. */
    @FXML private Label nextRunLabel;

    /** Bloco exibido quando o ajudante está desligado. */
    @FXML private HBox pausedBox;

    /** Trilho da barra de progresso até a próxima execução. */
    @FXML private StackPane progressTrack;

    /** Preenchimento da barra de progresso. */
    @FXML private Region progressFill;

    /** Ajudante exibido pelo cartão. */
    private final HelperItem item;

    /** Progresso até a próxima execução, de 0 a 1. */
    private final DoubleProperty progress = new SimpleDoubleProperty();

    // Constantes

    /** Horários exibidos no cartão; os demais viram "+N" (a lista completa fica na dica). */
    static final int TIMES_SHOWN = 2;

    /** Nome do estado visual de cartão selecionado. */
    private static final String SELECTED_STATE = "selected";

    /** Nome do estado visual de cartão desligado. */
    private static final String PAUSED_STATE = "paused";

    /** Estado visual de cartão selecionado. */
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass(SELECTED_STATE);

    /** Estado visual de cartão desligado. */
    private static final PseudoClass PAUSED = PseudoClass.getPseudoClass(PAUSED_STATE);

    /** Etiqueta do português do Brasil. */
    private static final String LANGUAGE_TAG = "pt-BR";

    /** Idioma usado para pôr textos em maiúsculas. */
    private static final Locale PT_BR = Locale.forLanguageTag(LANGUAGE_TAG);

    /** Arquivo FXML do cartão, no pacote do componente. */
    private static final String FXML_FILE = "helper-card.fxml";

    /** Modelo do texto de acessibilidade da caixa de seleção; recebe o nome do ajudante. */
    private static final String SELECT_ACCESSIBLE_TEXT = "Selecionar %s";

    /** Modelo do texto de acessibilidade do interruptor; recebe o nome do ajudante. */
    private static final String SWITCH_ACCESSIBLE_TEXT = "Ligar ou desligar %s";

    /** Dica do interruptor. */
    private static final String SWITCH_TOOLTIP = "Ligar ou desligar";

    /** Dica do bloco da próxima execução. */
    private static final String NEXT_RUN_TOOLTIP = "Próxima execução";

    /** Dica do bloco de ajudante desligado. */
    private static final String PAUSED_TOOLTIP = "Desligado: fora do Agendador do Windows";

    /** Nome do alvo quando é a lixeira. */
    private static final String RECYCLE_BIN_NAME = "Lixeira do Windows";

    /** Detalhe do alvo quando é a lixeira. */
    private static final String RECYCLE_BIN_DETAIL = "esvazia a lixeira";

    /** Separador dos horários na dica. */
    private static final String TIMES_SEPARATOR = ", ";

    /** Modelo do rótulo dos horários que não cabem no cartão; recebe a quantidade. */
    private static final String MORE_TIMES = "+%d";

    /** Classe CSS do rótulo "+N". */
    private static final String MORE_STYLE_CLASS = "more";

    /** Classe CSS de cada horário. */
    private static final String TIME_CHIP_STYLE_CLASS = "time-chip";

    /**
     * Cria o cartão de um ajudante.
     *
     * @param item ajudante a exibir
     */
    public HelperCard(HelperItem item) {
        this.item = item;
        Fxml.loadRoot(HelperCard.class.getResource(FXML_FILE), this);
        getStyleClass().add(item.kind().styleClass());

        kindIcon.setGlyph(item.kind() == HelperKind.ORGANIZER ? Glyph.FOLDER_SORTED : Glyph.SPARKLES);
        kindLabel.setText(item.kind().shortLabel().toUpperCase(PT_BR));
        nameLabel.setText(item.name());
        nameLabel.setTooltip(new Tooltip(item.name()));
        showTarget();
        showSchedule();

        selectBox.setAccessibleText(String.format(SELECT_ACCESSIBLE_TEXT, item.name()));
        enabledSwitch.setAccessibleText(String.format(SWITCH_ACCESSIBLE_TEXT, item.name()));
        enabledSwitch.setTooltip(new Tooltip(SWITCH_TOOLTIP));
        Tooltip.install(nextRunBox, new Tooltip(NEXT_RUN_TOOLTIP));
        Tooltip.install(pausedBox, new Tooltip(PAUSED_TOOLTIP));
        enabledSwitch.setSelected(item.enabled());
        showEnabled(item.enabled());
        enabledSwitch.selectedProperty().addListener((obs, was, on) -> showEnabled(on));
        selectBox.selectedProperty().addListener((obs, was, selected) -> {
            pseudoClassStateChanged(SELECTED, selected);
            if (selected) {
                Animations.pop(selectBox);
            }
        });
        addEventHandler(MouseEvent.MOUSE_CLICKED, this::toggleSelectionOnClick);

        Animations.hoverLift(this);
        Animations.hoverTilt(kindBadge, this);
    }

    /**
     * Mostra o alvo do ajudante: a lixeira ou a pasta.
     */
    private void showTarget() {
        if (item.targetsRecycleBin()) {
            targetIcon.setGlyph(Glyph.TRASH);
            targetName.setText(RECYCLE_BIN_NAME);
            targetDetail.setText(RECYCLE_BIN_DETAIL);
        } else {
            targetIcon.setGlyph(Glyph.FOLDER);
            targetName.setText(FolderField.displayName(item.folder()));
            targetDetail.setText(item.folder().toString());
            targetDetail.setTooltip(new Tooltip(item.folder().toString()));
        }
    }

    /**
     * Mostra a frequência, os primeiros horários, a próxima execução e a barra de progresso.
     */
    private void showSchedule() {
        everyLabel.setText(SchedulePreview.every(item.intervalDays()));
        List<LocalTime> times = item.times();
        times.stream().limit(TIMES_SHOWN).forEach(time -> timeChips.getChildren().add(chip(SchedulePreview.time(time))));
        if (times.size() > TIMES_SHOWN) {
            Label more = chip(String.format(MORE_TIMES, times.size() - TIMES_SHOWN));
            more.getStyleClass().add(MORE_STYLE_CLASS);
            more.setTooltip(new Tooltip(
                    times.stream().map(SchedulePreview::time).collect(Collectors.joining(TIMES_SEPARATOR))));
            timeChips.getChildren().add(more);
        }
        nextRunLabel.setText(item.nextRun());
        progressFill.prefWidthProperty().bind(progressTrack.widthProperty().multiply(progress));
    }

    /**
     * Cria o rótulo de um horário.
     *
     * @param text texto do rótulo
     * @return o rótulo pronto
     */
    private static Label chip(String text) {
        Label chip = new Label(text);
        chip.getStyleClass().add(TIME_CHIP_STYLE_CLASS);
        return chip;
    }

    /**
     * Ajusta o cartão ao estado ligado ou desligado e anima a barra de progresso ao ligar.
     *
     * @param on {@code true} quando o ajudante está ligado
     */
    private void showEnabled(boolean on) {
        pseudoClassStateChanged(PAUSED, !on);
        Nodes.setShown(nextRunBox, on && item.nextRun() != null);
        Nodes.setShown(pausedBox, !on);
        progress.set(0);
        if (on) {
            new Timeline(new KeyFrame(Duration.millis(1100),
                    new KeyValue(progress, item.nextRunProgress(), Animations.EASE_OUT))).play();
        }
    }

    /**
     * Marca ou desmarca o cartão num clique com o botão principal fora da caixa e do interruptor.
     *
     * @param event clique recebido pelo cartão
     */
    private void toggleSelectionOnClick(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY
                || Nodes.isInside(event.getTarget(), selectBox)
                || Nodes.isInside(event.getTarget(), enabledSwitch)) {
            return;
        }
        selectBox.setSelected(!selectBox.isSelected());
    }

    /**
     * Devolve o ajudante exibido.
     *
     * @return o ajudante do cartão
     */
    public HelperItem item() {
        return item;
    }

    /**
     * Devolve a propriedade de seleção do cartão.
     *
     * @return propriedade que indica se o cartão está selecionado
     */
    public BooleanProperty selectedProperty() {
        return selectBox.selectedProperty();
    }

    /**
     * Devolve a propriedade de ligado/desligado do cartão.
     *
     * @return propriedade que indica se o ajudante está ligado
     */
    public BooleanProperty enabledProperty() {
        return enabledSwitch.selectedProperty();
    }
}
