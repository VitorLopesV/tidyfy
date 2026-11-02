package com.tidyfy.presentation;

import com.tidyfy.presentation.component.Animations;
import com.tidyfy.presentation.component.Fxml;
import com.tidyfy.presentation.model.RunResult;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Diálogo que avisa que uma execução manual terminou com sucesso. As falhas usam o
 * {@link ErrorDialogController}.
 */
public class RunResultDialogController {

    /** Raiz do diálogo. */
    @FXML private VBox root;

    /** Selo de sucesso, que salta ao abrir. */
    @FXML private StackPane successBadge;

    /** Título do resultado. */
    @FXML private Label titleLabel;

    /** Mensagem do resultado. */
    @FXML private Label messageLabel;

    /** Resultado exibido. */
    private final RunResult result;

    // Constantes

    /** Arquivo FXML do diálogo, no pacote desta classe. */
    private static final String FXML_FILE = "run-result-dialog.fxml";

    /** Nome do parâmetro do resultado, usado na mensagem de valor nulo. */
    private static final String RESULT_NAME = "result";

    /**
     * Cria o diálogo.
     *
     * @param result resultado a exibir
     * @throws NullPointerException se {@code result} for nulo
     */
    public RunResultDialogController(RunResult result) {
        this.result = Objects.requireNonNull(result, RESULT_NAME);
    }

    /**
     * Carrega o diálogo.
     *
     * @return a raiz do diálogo
     */
    public Parent createView() {
        return Fxml.load(RunResultDialogController.class.getResource(FXML_FILE), this);
    }

    /**
     * Preenche os textos depois que o FXML é carregado.
     */
    @FXML
    private void initialize() {
        titleLabel.setText(result.title());
        messageLabel.setText(result.message());
        Animations.pop(successBadge);
    }

    /**
     * Fecha o diálogo.
     */
    @FXML
    private void onClose() {
        root.getScene().getWindow().hide();
    }
}
