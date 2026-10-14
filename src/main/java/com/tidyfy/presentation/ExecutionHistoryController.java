package com.tidyfy.presentation;

import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.presentation.component.Fxml;
import com.tidyfy.presentation.model.ExecutionAction;
import com.tidyfy.presentation.model.ExecutionItem;
import java.util.List;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Diálogo com o histórico de execuções de um ajudante, da mais recente para a mais antiga. Cada execução
 * mostra a data e a hora, um resumo e as ações realizadas; execuções com muitas ações mostram só as primeiras
 * e se expandem sob demanda. Os dados chegam prontos: quem busca e converte o histórico é quem abre o diálogo.
 */
public class ExecutionHistoryController {

    /** Raiz do diálogo. */
    @FXML private VBox root;

    /** Título do diálogo, com o nome do ajudante. */
    @FXML private Label titleLabel;

    /** Subtítulo, com a quantidade de execuções. */
    @FXML private Label subtitleLabel;

    /** Área rolável com as execuções. */
    @FXML private ScrollPane historyScroll;

    /** Lista das execuções, uma por cartão. */
    @FXML private VBox itemsBox;

    /** Estado vazio, mostrado quando não há execuções. */
    @FXML private VBox emptyBox;

    /** Mensagem do estado vazio. */
    @FXML private Label emptyLabel;

    /** Explicação do estado vazio. */
    @FXML private Label emptyHintLabel;

    /** Aviso do rodapé sobre por quanto tempo o histórico é guardado. */
    @FXML private Label retentionLabel;

    /** Nome do ajudante. */
    private final String helperName;

    /** Execuções a exibir, da mais recente para a mais antiga. */
    private final List<ExecutionItem> items;

    // Constantes

    /** Arquivo FXML do diálogo, no pacote desta classe. */
    private static final String FXML_FILE = "execution-history.fxml";

    /** Modelo do título; recebe o nome do ajudante. */
    private static final String TITLE = "Histórico de %s";

    /** Subtítulo quando não há execuções. */
    private static final String SUBTITLE_NONE = "Nenhuma execução registrada";

    /** Subtítulo quando há uma única execução. */
    private static final String SUBTITLE_ONE = "1 execução registrada";

    /** Modelo do subtítulo com várias execuções; recebe a quantidade. */
    private static final String SUBTITLE_MANY = "%d execuções registradas";

    /** Mensagem do estado vazio. */
    private static final String EMPTY_MESSAGE = "Este ajudante ainda não foi executado.";

    /** Explicação do estado vazio. */
    private static final String EMPTY_HINT = "Quando ele rodar, o que fez aparece aqui.";

    /** Modelo do aviso de retenção; recebe a quantidade de dias. */
    private static final String RETENTION_TEXT = "O histórico guarda as execuções dos últimos %d dias.";

    /** Modelo do botão que mostra todas as ações; recebe a quantidade de ações. */
    private static final String SHOW_ALL = "Ver todas as %d ações";

    /** Texto do botão que recolhe as ações. */
    private static final String SHOW_LESS = "Mostrar menos";

    /** Quantidade de ações mostradas antes de o usuário expandir a execução. */
    static final int VISIBLE_ACTIONS = 5;

    /** Classe CSS de cada cartão de execução. */
    private static final String ITEM_STYLE_CLASS = "history-item";

    /** Classe CSS do cartão de uma execução com falha. */
    private static final String ITEM_FAILED_STYLE_CLASS = "history-item-failed";

    /** Classe CSS da data e hora. */
    private static final String DATE_STYLE_CLASS = "history-date";

    /** Classe CSS do resumo. */
    private static final String SUMMARY_STYLE_CLASS = "history-summary";

    /** Classe CSS do resumo de uma execução com falha. */
    private static final String SUMMARY_FAILED_STYLE_CLASS = "history-summary-failed";

    /** Classe CSS de cada ação. */
    private static final String ACTION_STYLE_CLASS = "history-action";

    /** Classe CSS de uma ação que falhou. */
    private static final String ACTION_FAILED_STYLE_CLASS = "history-action-failed";

    /** Classe CSS do botão de expandir e recolher. */
    private static final String TOGGLE_STYLE_CLASS = "history-toggle";

    /** Espaço, em pixels, entre os elementos de um cartão. */
    private static final double CARD_SPACING = 8;

    /**
     * Cria o diálogo.
     *
     * @param helperName nome do ajudante, mostrado no título
     * @param items      execuções, da mais recente para a mais antiga; vazia quando nunca rodou
     */
    public ExecutionHistoryController(String helperName, List<ExecutionItem> items) {
        this.helperName = helperName;
        this.items = List.copyOf(items);
    }

    /**
     * Carrega o diálogo.
     *
     * @return a raiz do diálogo
     */
    public Parent createView() {
        return Fxml.load(ExecutionHistoryController.class.getResource(FXML_FILE), this);
    }

    /**
     * Preenche os textos e as execuções depois que o FXML é carregado.
     */
    @FXML
    private void initialize() {
        titleLabel.setText(String.format(TITLE, helperName));
        subtitleLabel.setText(subtitle());
        emptyLabel.setText(EMPTY_MESSAGE);
        emptyHintLabel.setText(EMPTY_HINT);
        retentionLabel.setText(String.format(RETENTION_TEXT, ExecutionRecord.RETENTION_DAYS));
        itemsBox.getChildren().setAll(items.stream().map(ExecutionHistoryController::card).toList());
        show(historyScroll, !items.isEmpty());
        show(emptyBox, items.isEmpty());
    }

    /**
     * Escolhe o subtítulo conforme a quantidade de execuções.
     *
     * @return o subtítulo
     */
    private String subtitle() {
        return switch (items.size()) {
            case 0 -> SUBTITLE_NONE;
            case 1 -> SUBTITLE_ONE;
            default -> String.format(SUBTITLE_MANY, items.size());
        };
    }

    /**
     * Mostra ou esconde um nó, sem deixar espaço quando escondido.
     *
     * @param node    nó a mostrar ou esconder
     * @param visible {@code true} para mostrar
     */
    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /**
     * Cria o cartão de uma execução.
     *
     * @param item execução do cartão
     * @return o cartão pronto
     */
    private static Node card(ExecutionItem item) {
        Label date = new Label(item.executedAt());
        date.getStyleClass().add(DATE_STYLE_CLASS);
        Label summary = new Label(item.summary());
        summary.getStyleClass().add(SUMMARY_STYLE_CLASS);
        if (item.hasFailure()) {
            summary.getStyleClass().add(SUMMARY_FAILED_STYLE_CLASS);
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, date, spacer, summary);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(CARD_SPACING, header);
        card.getStyleClass().add(ITEM_STYLE_CLASS);
        if (item.hasFailure()) {
            card.getStyleClass().add(ITEM_FAILED_STYLE_CLASS);
        }
        List<ExecutionAction> actions = item.actions();
        if (actions.isEmpty()) {
            return card;
        }
        VBox actionsBox = new VBox(4);
        card.getChildren().add(actionsBox);
        boolean collapsible = actions.size() > VISIBLE_ACTIONS;
        showActions(actionsBox, actions, !collapsible);
        if (collapsible) {
            card.getChildren().add(toggleButton(actionsBox, actions));
        }
        return card;
    }

    /**
     * Cria o botão que expande e recolhe as ações de uma execução.
     *
     * @param actionsBox caixa onde as ações aparecem
     * @param actions    todas as ações da execução
     * @return o botão
     */
    private static Button toggleButton(VBox actionsBox, List<ExecutionAction> actions) {
        Button toggle = new Button(String.format(SHOW_ALL, actions.size()));
        toggle.getStyleClass().add(TOGGLE_STYLE_CLASS);
        boolean[] expanded = {false};
        toggle.setOnAction(event -> {
            expanded[0] = !expanded[0];
            showActions(actionsBox, actions, expanded[0]);
            toggle.setText(expanded[0] ? SHOW_LESS : String.format(SHOW_ALL, actions.size()));
        });
        return toggle;
    }

    /**
     * Preenche a caixa com as ações, todas ou só as primeiras.
     *
     * @param actionsBox caixa onde as ações aparecem
     * @param actions    todas as ações da execução
     * @param all        {@code true} para mostrar todas; {@code false} para só as primeiras
     */
    private static void showActions(VBox actionsBox, List<ExecutionAction> actions, boolean all) {
        List<ExecutionAction> shown = all ? actions : actions.subList(0, VISIBLE_ACTIONS);
        actionsBox.getChildren().setAll(shown.stream().map(ExecutionHistoryController::actionRow).toList());
    }

    /**
     * Cria a linha de uma ação.
     *
     * @param action ação da linha
     * @return a linha pronta; falhas ganham a classe de destaque e os caminhos completos viram dica
     */
    private static Node actionRow(ExecutionAction action) {
        Label row = new Label(action.text());
        row.setWrapText(true);
        row.getStyleClass().add(ACTION_STYLE_CLASS);
        if (action.failure()) {
            row.getStyleClass().add(ACTION_FAILED_STYLE_CLASS);
        }
        if (action.detail() != null) {
            row.setTooltip(new Tooltip(action.detail()));
        }
        return row;
    }

    /**
     * Fecha o diálogo.
     */
    @FXML
    private void onClose() {
        root.getScene().getWindow().hide();
    }
}
