package com.tidyfy.presentation;

import com.tidyfy.application.usecase.SaveHelperUseCase;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import com.tidyfy.presentation.component.Animations;
import com.tidyfy.presentation.component.Fxml;
import com.tidyfy.presentation.component.ScheduleEditor;
import com.tidyfy.presentation.component.Nodes;
import com.tidyfy.presentation.component.ToggleSwitch;
import com.tidyfy.presentation.component.UpcomingRunsCalendar;
import com.tidyfy.presentation.model.HelperItem;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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

    /** Erro do salvamento, exibido no rodapé; escondido enquanto não há erro. */
    @FXML private Label saveError;

    /** Botão de salvar. */
    @FXML private Button saveButton;

    /** Ajudante em edição; {@code null} ao criar um novo. */
    private final HelperItem editing;

    /** Caso de uso que grava o ajudante; {@code null} quando o formulário foi criado sem serviços. */
    private final SaveHelperUseCase saveHelper;

    /** Relógio que define o dia de hoje, usado como data de início da frequência. */
    private final Clock clock;

    /** Indica se o ajudante já foi salvo com sucesso por este formulário. */
    private boolean saved;

    /** Indica se o usuário já mexeu no nome; o erro só aparece depois disso. */
    private final BooleanProperty nameTouched = new SimpleBooleanProperty();

    /**
     * Indica se o erro do nome deve aparecer. Fica em atributo porque as dependências de um binding só o
     * seguram por referência fraca: como variável local, ele podia ser coletado e o erro nunca aparecia.
     */
    private BooleanBinding nameError;

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

    /** Mensagem quando o formulário foi criado sem o caso de uso de salvar. */
    private static final String SAVE_UNAVAILABLE = "Salvar não está disponível neste formulário.";

    /** Mensagem quando a falha do salvamento não traz texto. */
    private static final String SAVE_FAILED = "Não foi possível salvar o ajudante.";

    /** Nome do parâmetro do relógio, usado na mensagem de valor nulo. */
    private static final String CLOCK_NAME = "clock";

    /** Classe CSS do texto de erro. */
    private static final String ERROR_STYLE_CLASS = "error";

    /**
     * Cria o formulário.
     *
     * @param editing ajudante sendo editado, ou {@code null} para criar um novo
     */
    protected HelperFormController(HelperItem editing) {
        this(editing, null, Clock.systemDefaultZone());
    }

    /**
     * Cria o formulário ligado ao caso de uso de salvar.
     *
     * @param editing    ajudante sendo editado, ou {@code null} para criar um novo
     * @param saveHelper caso de uso que grava o ajudante; {@code null} deixa o formulário sem poder salvar
     *                   (Salvar mostra um aviso e não fecha), o que basta para abri-lo em testes de tela
     * @param clock      relógio que define a data de início da frequência
     * @throws NullPointerException se {@code clock} for nulo
     */
    protected HelperFormController(HelperItem editing, SaveHelperUseCase saveHelper, Clock clock) {
        this.editing = editing;
        this.saveHelper = saveHelper;
        this.clock = Objects.requireNonNull(clock, CLOCK_NAME);
    }

    /**
     * Informa se o ajudante foi salvo com sucesso por este formulário.
     *
     * @return {@code true} depois de um Salvar bem-sucedido
     */
    public boolean saved() {
        return saved;
    }

    /**
     * Monta o ajudante do domínio com os dados da tela.
     *
     * @param id       identificador (novo ao criar; o do item ao editar)
     * @param name     nome do ajudante
     * @param schedule frequência
     * @param enabled  se o ajudante está ligado
     * @param editing  ajudante em edição, ou {@code null} ao criar
     * @return o ajudante do domínio
     */
    protected abstract Helper buildHelper(HelperId id, HelperName name, Schedule schedule, boolean enabled,
                                          HelperItem editing);

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
        Nodes.setShown(saveError, false);

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
        nameError = nameTouched.and(nameMissing);
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
     * Monta o ajudante e o grava pelo caso de uso. Se der certo, marca o formulário como salvo e o fecha;
     * se falhar, mantém o formulário aberto e preenchido e mostra a mensagem do erro no rodapé.
     */
    @FXML
    protected void onSave() {
        Nodes.setShown(saveError, false);
        if (saveHelper == null) {
            showSaveError(SAVE_UNAVAILABLE);
            return;
        }
        try {
            saveHelper.execute(buildHelper(editingId(), new HelperName(nameField.getText()), schedule(),
                    enabledSwitch.isSelected(), editing));
        } catch (RuntimeException error) {
            showSaveError(error.getMessage() == null || error.getMessage().isBlank()
                    ? SAVE_FAILED : error.getMessage());
            return;
        }
        saved = true;
        onClose();
    }

    /**
     * Identificador do ajudante: o do item em edição, ou um novo ao criar.
     */
    private HelperId editingId() {
        return editing == null ? HelperId.newId() : new HelperId(UUID.fromString(editing.id()));
    }

    /**
     * Frequência da tela. A data de início é hoje ao criar ou quando a frequência (intervalo ou horários)
     * mudou; do contrário mantém a do ajudante em edição.
     */
    private Schedule schedule() {
        int interval = scheduleEditor.getIntervalDays();
        List<LocalTime> times = scheduleEditor.getTimes().stream().sorted().toList();
        LocalDate start = LocalDate.now(clock);
        if (editing != null && editing.startDate() != null
                && editing.intervalDays() == interval && editing.times().equals(times)) {
            start = editing.startDate();
        }
        return new Schedule(interval, times, start);
    }

    /**
     * Mostra o erro do salvamento no rodapé.
     *
     * @param message texto do erro
     */
    private void showSaveError(String message) {
        saveError.setText(message);
        Nodes.setShown(saveError, true);
    }
}
