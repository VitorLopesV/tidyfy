package com.tidyfy.presentation;

import com.tidyfy.application.usecase.SaveHelperUseCase;
import com.tidyfy.domain.AgeLimit;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.CleanupTarget;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.RecycleBinTarget;
import com.tidyfy.domain.Schedule;
import com.tidyfy.presentation.component.Animations;
import com.tidyfy.presentation.component.FolderField;
import com.tidyfy.presentation.component.Nodes;
import com.tidyfy.presentation.model.HelperAgeReference;
import com.tidyfy.presentation.model.HelperItem;
import com.tidyfy.presentation.model.SchedulePreview;
import java.time.Clock;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Formulário do ajudante de Limpeza: nome, alvo (lixeira ou uma pasta), frequência e ligado/desligado.
 */
public class CleanerFormController extends HelperFormController {

    /** Opção de limpar a lixeira. */
    @FXML private ToggleButton binChoice;

    /** Opção de limpar uma pasta. */
    @FXML private ToggleButton folderChoice;

    /** Seção do campo de pasta, exibida quando o alvo é uma pasta. */
    @FXML private VBox folderSection;

    /** Campo da pasta a limpar. */
    @FXML private FolderField folderField;

    /** Prévia da limpeza de uma pasta. */
    @FXML private VBox folderPreview;

    /** Prévia da limpeza da lixeira. */
    @FXML private Node binPreview;

    /** Nome da pasta na prévia. */
    @FXML private Label previewFolderName;

    /** Selo animado da prévia da pasta. */
    @FXML private StackPane folderPreviewBadge;

    /** Selo animado da prévia da lixeira. */
    @FXML private StackPane binPreviewBadge;

    /** Opção de limpar tudo. */
    @FXML private ToggleButton allChoice;

    /** Opção de limpar só o que passou de N dias. */
    @FXML private ToggleButton olderChoice;

    /** Linha do contador de dias, ativa só em «mais de N dias». */
    @FXML private HBox ageStepperRow;

    /** Valor do contador de dias. */
    @FXML private Label ageDaysLabel;

    /** Unidade ao lado do contador ("dia" ou "dias"). */
    @FXML private Label ageUnitLabel;

    /** Botão de diminuir os dias. */
    @FXML private Button decreaseAge;

    /** Dica sobre a idade na lixeira. */
    @FXML private Label ageHelp;

    /** Seção da data de referência, exibida só para pasta com limite ligado. */
    @FXML private VBox referenceSection;

    /** Opção de contar a idade da criação. */
    @FXML private ToggleButton createdChoice;

    /** Opção de contar a idade da última modificação. */
    @FXML private ToggleButton modifiedChoice;

    /** Resumo do limite na prévia da pasta. */
    @FXML private Label folderAgeSummary;

    /** Resumo do limite na prévia da lixeira. */
    @FXML private Label binAgeSummary;

    /** Grupo das opções de alvo: sempre há uma opção escolhida. */
    private final ToggleGroup target = new ToggleGroup();

    /** Grupo «tudo» ou «mais de N dias»: sempre há uma opção escolhida. */
    private final ToggleGroup amount = new ToggleGroup();

    /** Grupo da data de referência: sempre há uma opção escolhida. */
    private final ToggleGroup reference = new ToggleGroup();

    /** Número de dias do limite de idade (mínimo 1). */
    private final IntegerProperty ageDays = new SimpleIntegerProperty(this, PROPERTY_AGE_DAYS, DEFAULT_AGE_DAYS);

    // Constantes

    /** Nome da propriedade dos dias do limite. */
    private static final String PROPERTY_AGE_DAYS = "ageDays";

    /** Dias do limite quando o usuário não escolheu outro. */
    private static final int DEFAULT_AGE_DAYS = 30;

    /** Menor número de dias do limite. */
    private static final int MIN_AGE_DAYS = 1;

    /** Resumo na prévia quando o ajudante limpa tudo. */
    private static final String SUMMARY_ALL = "Remove tudo, sem olhar a idade.";

    /** Modelo do resumo na prévia com limite; recebe os dias e a unidade. */
    private static final String SUMMARY_OLDER = "Remove só o que tem mais de %d %s.";

    /** Dica da idade na lixeira. */
    private static final String BIN_AGE_HELP = "Na lixeira, a idade conta do dia em que o arquivo foi para lá.";

    /** Arquivo FXML do formulário, no pacote desta classe. */
    private static final String FXML_FILE = "cleaner-form.fxml";

    /** Nome da pasta na prévia enquanto nenhuma foi escolhida. */
    private static final String FOLDER_PLACEHOLDER = "Pasta ainda não escolhida";

    /**
     * Cria o formulário.
     *
     * @param editing ajudante sendo editado, ou {@code null} para criar um novo
     */
    public CleanerFormController(HelperItem editing) {
        super(editing);
    }

    /**
     * Cria o formulário ligado ao caso de uso de salvar.
     *
     * @param editing    ajudante sendo editado, ou {@code null} para criar um novo
     * @param saveHelper caso de uso que grava o ajudante
     * @param clock      relógio que define a data de início da frequência
     */
    public CleanerFormController(HelperItem editing, SaveHelperUseCase saveHelper, Clock clock) {
        super(editing, saveHelper, clock);
    }

    /**
     * Monta o ajudante de limpeza com o limite de idade e a data de referência escolhidos no formulário.
     *
     * @return o ajudante do domínio
     */
    @Override
    protected Helper buildHelper(HelperId id, HelperName name, Schedule schedule, boolean enabled,
                                 HelperItem editing) {
        AgeLimit ageLimit = olderChoice.isSelected() ? new AgeLimit(ageDays.get()) : null;
        AgeReference ageReference = createdChoice.isSelected() ? AgeReference.CREATED : AgeReference.MODIFIED;
        CleanupTarget cleanupTarget = folderChoice.isSelected()
                ? new FolderTarget(folderField.getFolder(), ageReference)
                : new RecycleBinTarget();
        return new CleanerHelper(id, name, schedule, enabled, cleanupTarget, ageLimit);
    }

    /**
     * Diminui os dias do limite, sem passar do mínimo.
     */
    @FXML
    private void onDecreaseAge() {
        ageDays.set(Math.max(MIN_AGE_DAYS, ageDays.get() - 1));
    }

    /**
     * Aumenta os dias do limite.
     */
    @FXML
    private void onIncreaseAge() {
        ageDays.set(ageDays.get() + 1);
    }

    /**
     * Informa o arquivo FXML do formulário.
     *
     * @return nome do arquivo
     */
    @Override
    protected String viewName() {
        return FXML_FILE;
    }

    /**
     * Devolve o campo da pasta a limpar.
     *
     * @return o campo de pasta
     */
    @Override
    protected FolderField folderField() {
        return folderField;
    }

    /**
     * Informa se falta escolher a pasta: só importa quando o alvo é uma pasta.
     *
     * @return binding verdadeiro quando o alvo é uma pasta e nenhuma foi escolhida
     */
    @Override
    protected BooleanBinding targetMissing() {
        return folderChoice.selectedProperty().and(folderField.folderProperty().isNull());
    }

    /**
     * Mostra o alvo do ajudante em edição: a lixeira ou a pasta.
     *
     * @param item ajudante em edição
     */
    @Override
    protected void showTarget(HelperItem item) {
        if (item.targetsRecycleBin()) {
            target.selectToggle(binChoice);
        } else {
            target.selectToggle(folderChoice);
            folderField.setFolder(item.folder());
        }
        showAgeOf(item);
    }

    /**
     * Mostra o limite de idade e a data de referência do ajudante em edição.
     *
     * @param item ajudante em edição
     */
    private void showAgeOf(HelperItem item) {
        if (item.ageLimitDays() == null) {
            amount.selectToggle(allChoice);
        } else {
            ageDays.set(Math.max(MIN_AGE_DAYS, item.ageLimitDays()));
            amount.selectToggle(olderChoice);
        }
        reference.selectToggle(item.ageReference() == HelperAgeReference.CREATED ? createdChoice : modifiedChoice);
    }

    /**
     * Prepara as opções de alvo e as prévias. A pasta é o alvo inicial.
     */
    @Override
    @FXML
    protected void initialize() {
        binChoice.setToggleGroup(target);
        folderChoice.setToggleGroup(target);
        Nodes.stretchGraphic(binChoice, false);
        Nodes.stretchGraphic(folderChoice, false);
        target.selectToggle(folderChoice);
        target.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                old.setSelected(true);
                return;
            }
            clearFolderError();
            showTargetChoice(true);
        });

        allChoice.setToggleGroup(amount);
        olderChoice.setToggleGroup(amount);
        createdChoice.setToggleGroup(reference);
        modifiedChoice.setToggleGroup(reference);
        amount.selectToggle(allChoice);
        reference.selectToggle(modifiedChoice);
        keepOneSelected(amount);
        keepOneSelected(reference);
        ageDaysLabel.textProperty().bind(ageDays.asString());
        ageUnitLabel.textProperty().bind(Bindings.createStringBinding(
                () -> SchedulePreview.unit(ageDays.get()), ageDays));
        decreaseAge.disableProperty().bind(ageDays.lessThanOrEqualTo(MIN_AGE_DAYS));
        ageStepperRow.disableProperty().bind(olderChoice.selectedProperty().not());
        ageHelp.setText(BIN_AGE_HELP);

        super.initialize();

        amount.selectedToggleProperty().addListener((obs, old, now) -> showAge());
        ageDays.addListener((obs, old, now) -> showAge());
        showTargetChoice(false);
        previewFolderName.textProperty().bind(Bindings.createStringBinding(
                () -> folderField.getFolder() == null
                        ? FOLDER_PLACEHOLDER
                        : FolderField.displayName(folderField.getFolder()),
                folderField.folderProperty()));
        Animations.floating(folderPreviewBadge);
        Animations.floating(binPreviewBadge);
    }

    /**
     * Atualiza a visibilidade da referência, a dica da lixeira e os resumos das prévias.
     */
    private void showAge() {
        boolean folder = folderChoice.isSelected();
        boolean limited = olderChoice.isSelected();
        Nodes.setShown(referenceSection, folder && limited);
        Nodes.setShown(ageHelp, !folder);
        String summary = limited
                ? SUMMARY_OLDER.formatted(ageDays.get(), SchedulePreview.unit(ageDays.get()))
                : SUMMARY_ALL;
        folderAgeSummary.setText(summary);
        binAgeSummary.setText(summary);
    }

    /**
     * Impede que um grupo fique sem opção: clicar na opção escolhida a mantém.
     *
     * @param group grupo de opções
     */
    private static void keepOneSelected(ToggleGroup group) {
        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                old.setSelected(true);
            }
        });
    }

    /**
     * Mostra a seção e a prévia do alvo escolhido e esconde as do outro.
     *
     * @param animate {@code true} para animar a entrada
     */
    private void showTargetChoice(boolean animate) {
        boolean folder = folderChoice.isSelected();
        Nodes.setShown(folderSection, folder);
        Nodes.setShown(folderPreview, folder);
        Nodes.setShown(binPreview, !folder);
        showAge();
        if (animate) {
            Animations.fadeUpIn(folder ? folderPreview : binPreview);
            if (folder) {
                Animations.fadeUpIn(folderSection);
            }
        }
    }
}
