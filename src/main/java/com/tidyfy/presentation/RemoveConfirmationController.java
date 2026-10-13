package com.tidyfy.presentation;

import com.tidyfy.presentation.component.Animations;
import com.tidyfy.presentation.component.Fxml;
import com.tidyfy.presentation.component.Glyph;
import com.tidyfy.presentation.component.Icon;
import com.tidyfy.presentation.model.HelperItem;
import com.tidyfy.presentation.model.HelperKind;
import java.util.List;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Confirma a remoção de um ou vários ajudantes, listando quais serão removidos.
 */
public class RemoveConfirmationController {

    /** Raiz do diálogo. */
    @FXML private VBox root;

    /** Selo de alerta, que balança ao abrir. */
    @FXML private StackPane dangerBadge;

    /** Título do diálogo. */
    @FXML private Label titleLabel;

    /** Lista dos ajudantes que serão removidos. */
    @FXML private VBox itemsBox;

    /** Botão que confirma a remoção. */
    @FXML private Button confirmButton;

    /** Ajudantes a remover. */
    private final List<HelperItem> items;

    /** Indica se o usuário confirmou a remoção. */
    private boolean confirmed;

    // Constantes

    /** Arquivo FXML do diálogo, no pacote desta classe. */
    private static final String FXML_FILE = "remove-confirmation.fxml";

    /** Título quando há um único ajudante. */
    private static final String TITLE_ONE = "Remover este ajudante?";

    /** Modelo do título quando há vários ajudantes; recebe a quantidade. */
    private static final String TITLE_MANY = "Remover %d ajudantes?";

    /** Texto do botão quando há um único ajudante. */
    private static final String CONFIRM_ONE = "Remover ajudante";

    /** Modelo do texto do botão quando há vários ajudantes; recebe a quantidade. */
    private static final String CONFIRM_MANY = "Remover %d ajudantes";

    /** Classe CSS do selo do tipo. */
    private static final String KIND_BADGE_STYLE_CLASS = "kind-badge";

    /** Classe CSS do tamanho do selo. */
    private static final String BADGE_SIZE_STYLE_CLASS = "badge-30";

    /** Classe CSS do nome do ajudante. */
    private static final String ITEM_NAME_STYLE_CLASS = "remove-item-name";

    /** Classe CSS do tipo do ajudante. */
    private static final String ITEM_KIND_STYLE_CLASS = "remove-item-kind";

    /** Classe CSS do texto na cor do tipo. */
    private static final String KIND_TEXT_STYLE_CLASS = "kind-text";

    /** Classe CSS de cada linha da lista. */
    private static final String ITEM_STYLE_CLASS = "remove-item";

    /**
     * Cria o diálogo.
     *
     * @param items ajudantes a remover
     */
    public RemoveConfirmationController(List<HelperItem> items) {
        this.items = List.copyOf(items);
    }

    /**
     * Carrega o diálogo.
     *
     * @return a raiz do diálogo
     */
    public Parent createView() {
        return Fxml.load(RemoveConfirmationController.class.getResource(FXML_FILE), this);
    }

    /**
     * Preenche os textos e a lista depois que o FXML é carregado.
     */
    @FXML
    private void initialize() {
        boolean single = items.size() == 1;
        titleLabel.setText(single ? TITLE_ONE : String.format(TITLE_MANY, items.size()));
        confirmButton.setText(single ? CONFIRM_ONE : String.format(CONFIRM_MANY, items.size()));
        itemsBox.getChildren().setAll(items.stream().map(RemoveConfirmationController::row).toList());
        Animations.shake(dangerBadge);
    }

    /**
     * Cria a linha de um ajudante na lista.
     *
     * @param item ajudante da linha
     * @return a linha pronta
     */
    private static Node row(HelperItem item) {
        boolean organizer = item.kind() == HelperKind.ORGANIZER;
        StackPane badge = new StackPane(new Icon(organizer ? Glyph.FOLDER_SORTED : Glyph.SPARKLES, 16));
        badge.getStyleClass().addAll(KIND_BADGE_STYLE_CLASS, BADGE_SIZE_STYLE_CLASS);
        Label name = new Label(item.name());
        name.getStyleClass().add(ITEM_NAME_STYLE_CLASS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label kind = new Label(item.kind().shortLabel());
        kind.getStyleClass().addAll(ITEM_KIND_STYLE_CLASS, KIND_TEXT_STYLE_CLASS);
        HBox row = new HBox(12, badge, name, spacer, kind);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll(ITEM_STYLE_CLASS, item.kind().styleClass());
        return row;
    }

    /**
     * Fecha o diálogo sem confirmar.
     */
    @FXML
    private void onCancel() {
        root.getScene().getWindow().hide();
    }

    /**
     * Confirma a remoção e fecha o diálogo.
     */
    @FXML
    private void onConfirm() {
        confirmed = true;
        onCancel();
    }

    /**
     * Informa se o usuário confirmou a remoção.
     *
     * @return {@code true} quando a remoção foi confirmada
     */
    public boolean isConfirmed() {
        return confirmed;
    }
}
