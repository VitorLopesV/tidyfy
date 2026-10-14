package com.tidyfy.presentation;

import com.tidyfy.presentation.component.Animations;
import com.tidyfy.presentation.component.Fxml;
import com.tidyfy.presentation.component.Nodes;
import com.tidyfy.presentation.model.ErrorMessage;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Diálogo que explica um erro: título, mensagem e, recolhido até o usuário pedir, o detalhe técnico.
 */
public class ErrorDialogController {

    /** Raiz do diálogo. */
    @FXML private VBox root;

    /** Selo de alerta, que balança ao abrir. */
    @FXML private StackPane dangerBadge;

    /** Título do erro. */
    @FXML private Label titleLabel;

    /** Mensagem do erro. */
    @FXML private Label messageLabel;

    /** Botão que mostra ou oculta o detalhe técnico. */
    @FXML private Button detailsButton;

    /** Caixa com o detalhe técnico. */
    @FXML private VBox detailsBox;

    /** Texto do detalhe técnico. */
    @FXML private Label detailsLabel;

    /** Erro exibido. */
    private final ErrorMessage error;

    // Constantes

    /** Arquivo FXML do diálogo, no pacote desta classe. */
    private static final String FXML_FILE = "error-dialog.fxml";

    /** Texto do botão quando o detalhe está recolhido. */
    private static final String SHOW_DETAILS = "Mostrar detalhes";

    /** Texto do botão quando o detalhe está à mostra. */
    private static final String HIDE_DETAILS = "Ocultar";

    /** Nome do parâmetro do erro, usado na mensagem de valor nulo. */
    private static final String ERROR_NAME = "error";

    /**
     * Cria o diálogo.
     *
     * @param error erro a exibir
     * @throws NullPointerException se {@code error} for nulo
     */
    public ErrorDialogController(ErrorMessage error) {
        this.error = Objects.requireNonNull(error, ERROR_NAME);
    }

    /**
     * Carrega o diálogo.
     *
     * @return a raiz do diálogo
     */
    public Parent createView() {
        return Fxml.load(ErrorDialogController.class.getResource(FXML_FILE), this);
    }

    /**
     * Preenche os textos depois que o FXML é carregado; o detalhe começa recolhido.
     */
    @FXML
    private void initialize() {
        titleLabel.setText(error.title());
        messageLabel.setText(error.message());
        detailsLabel.setText(error.details());
        detailsButton.setText(SHOW_DETAILS);
        Nodes.setShown(detailsBox, false);
        Animations.shake(dangerBadge);
    }

    /**
     * Mostra ou oculta o detalhe técnico.
     */
    @FXML
    private void onToggleDetails() {
        boolean show = !detailsBox.isVisible();
        Nodes.setShown(detailsBox, show);
        detailsButton.setText(show ? HIDE_DETAILS : SHOW_DETAILS);
        if (root.getScene() != null && root.getScene().getWindow() != null) {
            root.getScene().getWindow().sizeToScene();
        }
    }

    /**
     * Fecha o diálogo.
     */
    @FXML
    private void onClose() {
        root.getScene().getWindow().hide();
    }
}
