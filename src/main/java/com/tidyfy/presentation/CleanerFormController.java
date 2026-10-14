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
import java.time.Clock;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
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

    /** Grupo das opções de alvo: sempre há uma opção escolhida. */
    private final ToggleGroup target = new ToggleGroup();

    // Constantes

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
     * Monta o ajudante de limpeza. O formulário ainda não tem campos de limite de idade nem de data de
     * referência: ao criar, limpa tudo e mede pela modificação; ao editar, preserva os do ajudante original.
     *
     * @return o ajudante do domínio
     */
    @Override
    protected Helper buildHelper(HelperId id, HelperName name, Schedule schedule, boolean enabled,
                                 HelperItem editing) {
        AgeLimit ageLimit = editing == null || editing.ageLimitDays() == null
                ? null : new AgeLimit(editing.ageLimitDays());
        CleanupTarget cleanupTarget = folderChoice.isSelected()
                ? new FolderTarget(folderField.getFolder(), referenceOf(editing))
                : new RecycleBinTarget();
        return new CleanerHelper(id, name, schedule, enabled, cleanupTarget, ageLimit);
    }

    /**
     * Data de referência da idade: a do ajudante em edição, ou a modificação quando não há.
     */
    private static AgeReference referenceOf(HelperItem editing) {
        if (editing == null || editing.ageReference() == null) {
            return AgeReference.MODIFIED;
        }
        return editing.ageReference() == HelperAgeReference.CREATED ? AgeReference.CREATED : AgeReference.MODIFIED;
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
            showTargetChoice(true);
        });

        super.initialize();

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
     * Mostra a seção e a prévia do alvo escolhido e esconde as do outro.
     *
     * @param animate {@code true} para animar a entrada
     */
    private void showTargetChoice(boolean animate) {
        boolean folder = folderChoice.isSelected();
        Nodes.setShown(folderSection, folder);
        Nodes.setShown(folderPreview, folder);
        Nodes.setShown(binPreview, !folder);
        if (animate) {
            Animations.fadeUpIn(folder ? folderPreview : binPreview);
            if (folder) {
                Animations.fadeUpIn(folderSection);
            }
        }
    }
}
