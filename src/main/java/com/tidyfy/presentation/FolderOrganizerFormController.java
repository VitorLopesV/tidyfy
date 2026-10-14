package com.tidyfy.presentation;

import com.tidyfy.application.usecase.SaveHelperUseCase;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import com.tidyfy.presentation.component.FolderField;
import com.tidyfy.presentation.model.HelperItem;
import java.time.Clock;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Formulário do ajudante de Organização de pasta: nome, uma pasta, frequência e ligado/desligado.
 */
public class FolderOrganizerFormController extends HelperFormController {

    /** Campo da pasta a organizar. */
    @FXML private FolderField folderField;

    /** Nome da pasta na prévia da organização. */
    @FXML private Label rootFolderLabel;

    // Constantes

    /** Arquivo FXML do formulário, no pacote desta classe. */
    private static final String FXML_FILE = "folder-organizer-form.fxml";

    /** Nome da pasta na prévia enquanto nenhuma foi escolhida. */
    private static final String FOLDER_PLACEHOLDER = "Sua pasta";

    /**
     * Cria o formulário.
     *
     * @param editing ajudante sendo editado, ou {@code null} para criar um novo
     */
    public FolderOrganizerFormController(HelperItem editing) {
        super(editing);
    }

    /**
     * Cria o formulário ligado ao caso de uso de salvar.
     *
     * @param editing    ajudante sendo editado, ou {@code null} para criar um novo
     * @param saveHelper caso de uso que grava o ajudante
     * @param clock      relógio que define a data de início da frequência
     */
    public FolderOrganizerFormController(HelperItem editing, SaveHelperUseCase saveHelper, Clock clock) {
        super(editing, saveHelper, clock);
    }

    /**
     * Monta o ajudante de organização com a pasta escolhida.
     *
     * @return o ajudante do domínio
     */
    @Override
    protected Helper buildHelper(HelperId id, HelperName name, Schedule schedule, boolean enabled,
                                 HelperItem editing) {
        return new FolderOrganizerHelper(id, name, schedule, enabled, folderField.getFolder());
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
     * Devolve o campo da pasta a organizar.
     *
     * @return o campo de pasta
     */
    @Override
    protected FolderField folderField() {
        return folderField;
    }

    /**
     * Informa se falta escolher a pasta.
     *
     * @return binding verdadeiro enquanto nenhuma pasta foi escolhida
     */
    @Override
    protected BooleanBinding targetMissing() {
        return folderField.folderProperty().isNull();
    }

    /**
     * Mostra a pasta do ajudante em edição.
     *
     * @param item ajudante em edição
     */
    @Override
    protected void showTarget(HelperItem item) {
        folderField.setFolder(item.folder());
    }

    /**
     * Prepara o formulário e liga o nome da pasta à prévia.
     */
    @Override
    @FXML
    protected void initialize() {
        super.initialize();
        rootFolderLabel.textProperty().bind(Bindings.createStringBinding(
                () -> folderField.getFolder() == null
                        ? FOLDER_PLACEHOLDER
                        : FolderField.displayName(folderField.getFolder()),
                folderField.folderProperty()));
    }
}
