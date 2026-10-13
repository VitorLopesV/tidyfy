package com.tidyfy.presentation.component;

import com.tidyfy.presentation.model.SchedulePreview;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Edita a frequência: a cada quantos dias e em quais horários (pelo menos um, sem repetir).
 */
public final class ScheduleEditor extends VBox {

    /** Intervalo, em dias, entre as execuções. */
    private final IntegerProperty intervalDays = new SimpleIntegerProperty(this, PROPERTY_INTERVAL_DAYS, 1);

    /** Horários do dia, em ordem crescente e sem repetição. */
    private final ObservableList<LocalTime> times = FXCollections.observableArrayList();

    /** Área com um rótulo por horário e o botão de adicionar. */
    private final FlowPane chips = new FlowPane(8, 8);

    /** Botão que abre o seletor de novo horário. */
    private final Button addButton = new Button(ADD_TIME_TEXT, new Icon(Glyph.PLUS, 16));

    /** Seletor da hora do novo horário. */
    private final Spinner<Integer> hour = timeSpinner(23, 1, 12);

    /** Seletor dos minutos do novo horário. */
    private final Spinner<Integer> minute = timeSpinner(59, 5, 0);

    /** Linha com os seletores de hora e minuto, exibida ao adicionar um horário. */
    private final HBox adder;

    /** Texto de ajuda; mostra o erro quando o horário já existe. */
    private final Label help = new Label(HELP);

    // Constantes

    /** Ajuda padrão sobre os horários. */
    private static final String HELP = "Pelo menos um horário, sem repetir.";

    /** Erro exibido ao tentar repetir um horário. */
    private static final String DUPLICATED = "Esse horário já está na lista.";

    /** Nome da propriedade do intervalo. */
    private static final String PROPERTY_INTERVAL_DAYS = "intervalDays";

    /** Texto do botão que abre o seletor de horário. */
    private static final String ADD_TIME_TEXT = "Adicionar horário";

    /** Texto do botão que confirma o novo horário. */
    private static final String CONFIRM_TEXT = "Adicionar";

    /** Texto antes do contador de dias. */
    private static final String REPEAT_TEXT = "Repetir a cada";

    /** Título da lista de horários. */
    private static final String TIMES_TITLE = "Horários do dia";

    /** Separador visual entre hora e minuto. */
    private static final String COLON = ":";

    /** Texto de acessibilidade do botão de diminuir o intervalo. */
    private static final String DECREASE_ACCESSIBLE_TEXT = "Diminuir intervalo";

    /** Texto de acessibilidade do botão de aumentar o intervalo. */
    private static final String INCREASE_ACCESSIBLE_TEXT = "Aumentar intervalo";

    /** Texto de acessibilidade do botão que cancela o novo horário. */
    private static final String CANCEL_ACCESSIBLE_TEXT = "Cancelar novo horário";

    /** Modelo do texto de acessibilidade do botão de remover horário; recebe o horário. */
    private static final String REMOVE_ACCESSIBLE_TEXT = "Remover horário %s";

    /** Identificador do botão de diminuir o intervalo. */
    private static final String DECREASE_ID = "decreaseInterval";

    /** Identificador do botão de aumentar o intervalo. */
    private static final String INCREASE_ID = "increaseInterval";

    /** Identificador do botão de adicionar horário. */
    private static final String ADD_TIME_ID = "addTime";

    /** Identificador do texto de ajuda. */
    private static final String HELP_ID = "timesHelp";

    /** Classe CSS do componente. */
    private static final String STYLE_CLASS = "schedule-editor";

    /** Classe CSS do valor do contador. */
    private static final String STEPPER_VALUE_STYLE_CLASS = "stepper-value";

    /** Classe CSS do contador. */
    private static final String STEPPER_STYLE_CLASS = "stepper";

    /** Classe CSS do rótulo de campo. */
    private static final String FIELD_LABEL_STYLE_CLASS = "field-label";

    /** Classe CSS do texto esmaecido. */
    private static final String MUTED_STYLE_CLASS = "muted";

    /** Classe CSS do título da lista de horários. */
    private static final String SUBFIELD_LABEL_STYLE_CLASS = "subfield-label";

    /** Classe CSS dos botões. */
    private static final String BUTTON_STYLE_CLASS = "btn";

    /** Classe CSS do botão tracejado. */
    private static final String DASHED_BUTTON_STYLE_CLASS = "btn-dashed";

    /** Classe CSS do botão de destaque. */
    private static final String PRIMARY_BUTTON_STYLE_CLASS = "btn-primary";

    /** Classe CSS do botão compacto. */
    private static final String COMPACT_BUTTON_STYLE_CLASS = "btn-compact";

    /** Classe CSS dos dois pontos entre hora e minuto. */
    private static final String TIME_COLON_STYLE_CLASS = "time-colon";

    /** Classe CSS da linha de adicionar horário. */
    private static final String ADDER_STYLE_CLASS = "time-adder";

    /** Classe CSS do texto de ajuda. */
    private static final String HELP_STYLE_CLASS = "help";

    /** Classe CSS do texto de ajuda quando mostra um erro. */
    private static final String ERROR_STYLE_CLASS = "error";

    /** Classe CSS do ícone do relógio no rótulo de horário. */
    private static final String CHIP_CLOCK_STYLE_CLASS = "chip-clock";

    /** Classe CSS do texto do horário. */
    private static final String CHIP_TEXT_STYLE_CLASS = "chip-text";

    /** Classe CSS do botão de remover horário. */
    private static final String CHIP_REMOVE_STYLE_CLASS = "chip-remove";

    /** Classe CSS do rótulo de horário editável. */
    private static final String CHIP_STYLE_CLASS = "time-chip-editable";

    /** Classe CSS dos seletores de hora e minuto. */
    private static final String SPINNER_STYLE_CLASS = "time-spinner";

    /** Classe CSS dos botões que só têm ícone. */
    private static final String ICON_BUTTON_STYLE_CLASS = "icon-btn";

    /** Formato dos números do seletor: dois dígitos. */
    private static final String TWO_DIGITS = "%02d";

    /** Texto do seletor quando não há valor. */
    private static final String EMPTY = "";

    /**
     * Cria o editor com intervalo de 1 dia e sem horários.
     */
    public ScheduleEditor() {
        getStyleClass().add(STYLE_CLASS);
        setSpacing(14);

        Button decrease = iconButton(Glyph.MINUS, DECREASE_ACCESSIBLE_TEXT);
        Button increase = iconButton(Glyph.PLUS, INCREASE_ACCESSIBLE_TEXT);
        decrease.setId(DECREASE_ID);
        increase.setId(INCREASE_ID);
        addButton.setId(ADD_TIME_ID);
        help.setId(HELP_ID);
        decrease.disableProperty().bind(intervalDays.lessThanOrEqualTo(1));
        decrease.setOnAction(event -> intervalDays.set(intervalDays.get() - 1));
        increase.setOnAction(event -> intervalDays.set(intervalDays.get() + 1));
        Label value = new Label();
        value.getStyleClass().add(STEPPER_VALUE_STYLE_CLASS);
        value.textProperty().bind(intervalDays.asString());
        HBox stepper = new HBox(2, decrease, value, increase);
        stepper.getStyleClass().add(STEPPER_STYLE_CLASS);
        stepper.setAlignment(Pos.CENTER);
        Label unit = new Label();
        unit.getStyleClass().add(FIELD_LABEL_STYLE_CLASS);
        unit.textProperty().bind(Bindings.createStringBinding(() -> SchedulePreview.unit(intervalDays.get()), intervalDays));
        Label repeat = new Label(REPEAT_TEXT);
        repeat.getStyleClass().add(MUTED_STYLE_CLASS);
        HBox intervalRow = new HBox(12, repeat, stepper, unit);
        intervalRow.setAlignment(Pos.CENTER_LEFT);

        Label timesTitle = new Label(TIMES_TITLE);
        timesTitle.getStyleClass().add(SUBFIELD_LABEL_STYLE_CLASS);
        addButton.getStyleClass().addAll(BUTTON_STYLE_CLASS, DASHED_BUTTON_STYLE_CLASS);
        addButton.setOnAction(event -> openAdder());
        Button confirm = new Button(CONFIRM_TEXT);
        confirm.getStyleClass().addAll(BUTTON_STYLE_CLASS, PRIMARY_BUTTON_STYLE_CLASS, COMPACT_BUTTON_STYLE_CLASS);
        confirm.setOnAction(event -> confirmAdder());
        Button cancel = iconButton(Glyph.CLOSE, CANCEL_ACCESSIBLE_TEXT);
        cancel.setOnAction(event -> closeAdder());
        Label colon = new Label(COLON);
        colon.getStyleClass().add(TIME_COLON_STYLE_CLASS);
        adder = new HBox(6, hour, colon, minute, confirm, cancel);
        adder.getStyleClass().add(ADDER_STYLE_CLASS);
        adder.setAlignment(Pos.CENTER_LEFT);
        Nodes.setShown(adder, false);
        help.getStyleClass().add(HELP_STYLE_CLASS);
        help.setWrapText(true);

        getChildren().addAll(intervalRow, new VBox(8, timesTitle, chips, adder, help));
        times.addListener((ListChangeListener<LocalTime>) change -> rebuildChips(null));
        rebuildChips(null);
    }

    /**
     * Adiciona o horário se ele ainda não estiver na lista, mantendo a ordem crescente.
     *
     * @param time horário a adicionar
     * @return {@code true} se o horário foi adicionado; {@code false} se já estava na lista
     */
    public boolean addTime(LocalTime time) {
        if (times.contains(time)) {
            return false;
        }
        List<LocalTime> sorted = new ArrayList<>(times);
        sorted.add(time);
        sorted.sort(null);
        times.setAll(sorted);
        return true;
    }

    /**
     * Recria os rótulos dos horários, seguidos do botão de adicionar.
     *
     * @param highlight horário que aparece com animação; pode ser {@code null}
     */
    private void rebuildChips(LocalTime highlight) {
        List<Node> nodes = new ArrayList<>();
        for (LocalTime time : times) {
            Node chip = chip(time);
            if (time.equals(highlight)) {
                Animations.pop(chip);
            }
            nodes.add(chip);
        }
        nodes.add(addButton);
        chips.getChildren().setAll(nodes);
    }

    /**
     * Cria o rótulo de um horário, com o botão de remover (desativado quando é o único horário).
     *
     * @param time horário do rótulo
     * @return o rótulo pronto
     */
    private Node chip(LocalTime time) {
        Icon clock = new Icon(Glyph.CLOCK, 15);
        clock.getStyleClass().add(CHIP_CLOCK_STYLE_CLASS);
        Label text = new Label(SchedulePreview.time(time));
        text.getStyleClass().add(CHIP_TEXT_STYLE_CLASS);
        Button remove = iconButton(Glyph.CLOSE, String.format(REMOVE_ACCESSIBLE_TEXT, text.getText()));
        remove.getStyleClass().add(CHIP_REMOVE_STYLE_CLASS);
        ((Icon) remove.getGraphic()).setSize(14);
        remove.setDisable(times.size() == 1);
        remove.setOnAction(event -> times.remove(time));
        HBox chip = new HBox(6, clock, text, remove);
        chip.getStyleClass().add(CHIP_STYLE_CLASS);
        chip.setAlignment(Pos.CENTER_LEFT);
        return chip;
    }

    /**
     * Troca o botão de adicionar pelos seletores de hora e minuto.
     */
    private void openAdder() {
        Nodes.setShown(addButton, false);
        Nodes.setShown(adder, true);
        Animations.fadeUpIn(adder);
        hour.requestFocus();
    }

    /**
     * Adiciona o horário escolhido; se ele já existir, mostra o erro e mantém os seletores abertos.
     */
    private void confirmAdder() {
        commit(hour);
        commit(minute);
        LocalTime time = LocalTime.of(hour.getValue(), minute.getValue());
        if (addTime(time)) {
            closeAdder();
            rebuildChips(time);
        } else {
            help.setText(DUPLICATED);
            help.getStyleClass().add(ERROR_STYLE_CLASS);
        }
    }

    /**
     * Esconde os seletores, devolve o botão de adicionar e limpa o erro.
     */
    private void closeAdder() {
        Nodes.setShown(adder, false);
        Nodes.setShown(addButton, true);
        help.setText(HELP);
        help.getStyleClass().remove(ERROR_STYLE_CLASS);
    }

    /**
     * Confirma o que foi digitado no seletor; se o texto não for um número, volta ao valor anterior.
     *
     * @param spinner seletor a confirmar
     */
    private static void commit(Spinner<Integer> spinner) {
        try {
            spinner.getValueFactory().setValue(spinner.getValueFactory().getConverter().fromString(spinner.getEditor().getText()));
        } catch (NumberFormatException ignored) {
            spinner.getEditor().setText(spinner.getValueFactory().getConverter().toString(spinner.getValue()));
        }
    }

    /**
     * Cria um seletor numérico de dois dígitos, que volta ao início depois do máximo.
     *
     * @param max     maior valor
     * @param step    quanto cada clique soma ou subtrai
     * @param initial valor inicial
     * @return o seletor pronto
     */
    private static Spinner<Integer> timeSpinner(int max, int step, int initial) {
        IntegerSpinnerValueFactory factory = new IntegerSpinnerValueFactory(0, max, initial, step);
        factory.setWrapAround(true);
        factory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? EMPTY : TWO_DIGITS.formatted(value);
            }

            @Override
            public Integer fromString(String text) {
                return Integer.parseInt(text.trim());
            }
        });
        Spinner<Integer> spinner = new Spinner<>(factory);
        spinner.setEditable(true);
        spinner.getStyleClass().add(SPINNER_STYLE_CLASS);
        spinner.setPrefWidth(86);
        return spinner;
    }

    /**
     * Cria um botão que só mostra um ícone.
     *
     * @param glyph          desenho do ícone
     * @param accessibleText texto lido por leitores de tela
     * @return o botão pronto
     */
    private static Button iconButton(Glyph glyph, String accessibleText) {
        Button button = new Button(null, new Icon(glyph, 18));
        button.getStyleClass().add(ICON_BUTTON_STYLE_CLASS);
        button.setAccessibleText(accessibleText);
        return button;
    }

    /**
     * Devolve o intervalo entre as execuções.
     *
     * @return intervalo, em dias
     */
    public int getIntervalDays() {
        return intervalDays.get();
    }

    /**
     * Define o intervalo entre as execuções.
     *
     * @param value intervalo, em dias
     */
    public void setIntervalDays(int value) {
        intervalDays.set(value);
    }

    /**
     * Devolve a propriedade do intervalo.
     *
     * @return propriedade do intervalo, em dias
     */
    public IntegerProperty intervalDaysProperty() {
        return intervalDays;
    }

    /**
     * Devolve a lista de horários, que pode ser observada.
     *
     * @return horários do dia, em ordem crescente
     */
    public ObservableList<LocalTime> getTimes() {
        return times;
    }

    /**
     * Substitui os horários, sem repetições e em ordem crescente.
     *
     * @param values novos horários
     */
    public void setTimes(Collection<LocalTime> values) {
        times.setAll(values.stream().distinct().sorted().toList());
    }
}
