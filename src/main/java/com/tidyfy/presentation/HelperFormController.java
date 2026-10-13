package com.tidyfy.presentation;

import com.tidyfy.presentation.component.Animations;
import com.tidyfy.presentation.component.Fxml;
import com.tidyfy.presentation.component.ScheduleEditor;
import com.tidyfy.presentation.component.ToggleSwitch;
import com.tidyfy.presentation.component.UpcomingRunsCalendar;
import com.tidyfy.presentation.model.HelperItem;
import java.time.LocalTime;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

/**
 * Partes comuns aos formulários de ajudante: nome, frequência, ligado/desligado e rodapé.
 */
public abstract class HelperFormController {

    /** Raiz do formulário. */
    @FXML protected BorderPane root;

    /** Título do formulário. */
    @FXML private Label titleLabel;

    /** Rótulo do campo de nome. */
    @FXML private Label nameTitle;

    /** Campo do nome do ajudante. */
    @FXML private TextField nameField;

    /** Ajuda ou erro do campo de nome. */
    @FXML private Label nameHelp;

    /** Editor da frequência. */
    @FXML private ScheduleEditor scheduleEditor;

    /** Calendário dos próximos dias de execução. */
    @FXML private UpcomingRunsCalendar calendar;

    /** Interruptor de ligado/desligado. */
    @FXML private ToggleSwitch enabledSwitch;

    /** Título do estado ligado/desligado. */
    @FXML private Label enabledTitle;

    /** Explicação do estado ligado/desligado. */
    @FXML private Label enabledHelp;

    /** Botão de salvar. */
    @FXML private Button saveButton;

    /** Ajudante em edição; {@code null} ao criar um novo. */
    private final HelperItem editing;

    /** Indica se o usuário já mexeu no nome; o erro só aparece depois disso. */
    private final BooleanProperty nameTouched = new SimpleBooleanProperty();

    // Constantes

    /** Ajuda do campo de nome. */
    private static final String NAME_HELP = "Obrigatório. Espaços no começo e no fim são ignorados.";

    /** Erro do campo de nome. */
    private static final String NAME_ERROR = "O nome é obrigatório.";

    /** Título do formulário ao criar. */
    private static final String CREATE_TITLE = "Novo ajudante";

    /** Título do formulário ao editar. */
    private static final String EDIT_TITLE = "Editar ajudante";

    /** Texto do botão de salvar ao criar. */
    private static final String CREATE_BUTTON = "Criar ajudante";

    /** Texto do botão de salvar ao editar. */
    private static final String EDIT_BUTTON = "Salvar alterações";

    /** Título do estado ligado. */
    private static final String ENABLED_TITLE = "Ajudante ligado";

    /** Título do estado desligado. */
    private static final String DISABLED_TITLE = "Ajudante desligado";

    /** Explicação do estado ligado. */
    private static final String ENABLED_HELP = "Fica registrado no Agendador do Windows.";

    /** Explicação do estado desligado. */
    private static final String DISABLED_HELP = "Não fica no Agendador e não executa.";

    /** Classe CSS do campo de nome com erro. */
    private static final String INVALID_STYLE_CLASS = "invalid";

    /** Classe CSS do texto de erro. */
    private static final String ERROR_STYLE_CLASS = "error";

    /**
     * Cria o formulário.
     *
     * @param editing ajudante sendo editado, ou {@code null} para criar um novo
     */
    protected HelperFormController(HelperItem editing) {
        this.editing = editing;
    }

    /**
     * Carrega o formulário.
     *
     * @return a raiz do formulário
     */
    public Parent createView() {
        return Fxml.load(HelperFormController.class.getResource(viewName()), this);
    }

    /**
     * Informa o arquivo FXML do formulário.
     *
     * @return nome do arquivo, no pacote desta classe
     */
    protected abstract String viewName();

    /**
     * Informa se ainda falta escolher onde o ajudante vai atuar.
     *
     * @return binding verdadeiro enquanto faltar o alvo
     */
    protected abstract BooleanBinding targetMissing();

    /**
     * Mostra o alvo do ajudante que está sendo editado.
     *
     * @param item ajudante em edição
     */
    protected abstract void showTarget(HelperItem item);

    /**
     * Preenche o formulário e liga as regras de validação depois que o FXML é carregado.
     */
    @FXML
    protected void initialize() {
        boolean creating = editing == null;
        titleLabel.setText(creating ? CREATE_TITLE : EDIT_TITLE);
        saveButton.setText(creating ? CREATE_BUTTON : EDIT_BUTTON);
        nameTitle.setLabelFor(nameField);

        if (creating) {
            scheduleEditor.setTimes(List.of(LocalTime.of(9, 0)));
            enabledSwitch.setSelected(true);
        } else {
            nameField.setText(editing.name());
            scheduleEditor.setIntervalDays(editing.intervalDays());
            scheduleEditor.setTimes(editing.times());
            enabledSwitch.setSelected(editing.enabled());
            showTarget(editing);
        }

        nameField.textProperty().addListener((obs, old, now) -> nameTouched.set(true));
        BooleanBinding nameMissing = Bindings.createBooleanBinding(
                () -> nameField.getText() == null || nameField.getText().isBlank(), nameField.textProperty());
        BooleanBinding nameError = nameTouched.and(nameMissing);
        showNameError(false);
        nameError.addListener((obs, was, error) -> showNameError(error));
        saveButton.disableProperty().bind(nameMissing.or(targetMissing()));

        calendar.bind(scheduleEditor.intervalDaysProperty(), scheduleEditor.getTimes());
        showEnabled(enabledSwitch.isSelected());
        enabledSwitch.selectedProperty().addListener((obs, was, on) -> showEnabled(on));
        enabledSwitch.accessibleTextProperty().bind(enabledTitle.textProperty());
    }

    /**
     * Mostra a ajuda ou o erro do campo de nome.
     *
     * @param error {@code true} para mostrar o erro
     */
    private void showNameError(boolean error) {
        nameHelp.setText(error ? NAME_ERROR : NAME_HELP);
        if (error) {
            nameField.getStyleClass().add(INVALID_STYLE_CLASS);
            nameHelp.getStyleClass().add(ERROR_STYLE_CLASS);
            Animations.fadeUpIn(nameHelp);
        } else {
            nameField.getStyleClass().remove(INVALID_STYLE_CLASS);
            nameHelp.getStyleClass().remove(ERROR_STYLE_CLASS);
        }
    }

    /**
     * Mostra o título e a explicação do estado ligado ou desligado.
     *
     * @param on {@code true} quando o ajudante está ligado
     */
    private void showEnabled(boolean on) {
        enabledTitle.setText(on ? ENABLED_TITLE : DISABLED_TITLE);
        enabledHelp.setText(on ? ENABLED_HELP : DISABLED_HELP);
    }

    /**
     * Fecha o formulário.
     */
    @FXML
    protected void onClose() {
        root.getScene().getWindow().hide();
    }

    /**
     * Salva o ajudante. Por enquanto só fecha o formulário: salvar de verdade chega com o SaveHelperUseCase.
     */
    @FXML
    protected void onSave() {
        onClose();
    }
}
