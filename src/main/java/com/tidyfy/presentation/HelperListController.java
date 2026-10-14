package com.tidyfy.presentation;

import com.tidyfy.presentation.component.Animations;
import com.tidyfy.presentation.component.CardGrid;
import com.tidyfy.presentation.component.Fxml;
import com.tidyfy.presentation.component.Glyph;
import com.tidyfy.presentation.component.HelperCard;
import com.tidyfy.presentation.component.Icon;
import com.tidyfy.presentation.component.Nodes;
import com.tidyfy.presentation.component.UsageMeter;
import com.tidyfy.presentation.model.ExecutionItem;
import com.tidyfy.presentation.model.HelperItem;
import com.tidyfy.presentation.model.HelperKind;
import com.tidyfy.presentation.model.HelperSelection;
import com.tidyfy.presentation.model.TypeFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Lista de ajudantes: filtro por tipo, seleção e ações. Edição só com um ajudante selecionado;
 * remoção com um ou vários. A página não rola; só a área dos cartões, quando eles não cabem.
 * Ainda sem casos de uso: a lista fica vazia até alguém chamar {@link #showHelpers(List)}.
 */
public class HelperListController {

    /** Raiz da tela. */
    @FXML private StackPane root;

    /** Botão de criar ajudante do cabeçalho. */
    @FXML private Button newHelperButton;

    /** Ícone do botão de criar ajudante. */
    @FXML private Icon newHelperIcon;

    /** Quantidade de ajudantes de organização. */
    @FXML private Label organizerCount;

    /** Medidor de uso do limite de ajudantes de organização. */
    @FXML private UsageMeter organizerUsage;

    /** Quantidade de ajudantes de limpeza. */
    @FXML private Label cleanerCount;

    /** Medidor de uso do limite de ajudantes de limpeza. */
    @FXML private UsageMeter cleanerUsage;

    /** Quantidade de ajudantes ligados. */
    @FXML private Label enabledCount;

    /** Seção com filtro e cartões, exibida quando há ajudantes. */
    @FXML private VBox listSection;

    /** Barra de filtros por tipo. */
    @FXML private HBox filterBar;

    /** Filtro que mostra todos os tipos. */
    @FXML private ToggleButton filterAll;

    /** Filtro que mostra só a organização. */
    @FXML private ToggleButton filterOrganizer;

    /** Filtro que mostra só a limpeza. */
    @FXML private ToggleButton filterCleaner;

    /** Quantidade exibida no filtro de todos. */
    @FXML private Label filterAllCount;

    /** Quantidade exibida no filtro de organização. */
    @FXML private Label filterOrganizerCount;

    /** Quantidade exibida no filtro de limpeza. */
    @FXML private Label filterCleanerCount;

    /** Caixa que seleciona todos os cartões visíveis. */
    @FXML private CheckBox selectAll;

    /** Grade com os cartões. */
    @FXML private CardGrid cardGrid;

    /** Aviso de que o filtro não encontrou ajudantes. */
    @FXML private Label noResults;

    /** Tela exibida quando não há nenhum ajudante. */
    @FXML private VBox emptyState;

    /** Ilustração da tela vazia. */
    @FXML private Group emptyIllustration;

    /** Botão de criar o primeiro ajudante. */
    @FXML private Button firstHelperButton;

    /** Ícone do botão de criar o primeiro ajudante. */
    @FXML private Icon firstHelperIcon;

    /** Película que escurece a lista enquanto um diálogo está aberto. */
    @FXML private Region scrim;

    /** Ações exibidas quando há ajudantes selecionados. */
    @FXML private HBox selectionActions;

    /** Aviso de que só dá para editar um ajudante por vez. */
    @FXML private Label editHint;

    /** Quantidade de ajudantes selecionados. */
    @FXML private Label selectionCount;

    /** Texto que acompanha a quantidade de selecionados. */
    @FXML private Label selectionText;

    /** Botão de editar. */
    @FXML private Button editButton;

    /** Ícone do botão de editar. */
    @FXML private Icon editIcon;

    /** Botão de remover. */
    @FXML private Button removeButton;

    /** Ajudantes exibidos pela tela. */
    private final List<HelperItem> helpers = new ArrayList<>();

    /** Ajudantes marcados. */
    private final HelperSelection selection = new HelperSelection();

    /** Grupo dos botões de filtro: sempre há um filtro ativo. */
    private final ToggleGroup filters = new ToggleGroup();

    /** Filtro ativo. */
    private TypeFilter filter = TypeFilter.ALL;

    /** Busca o histórico de um ajudante pelo id; por padrão devolve sempre uma lista vazia. */
    private Function<String, List<ExecutionItem>> historyLoader = id -> List.of();

    // Constantes

    /** Título do diálogo de histórico. */
    private static final String HISTORY_TITLE = "Histórico";

    /** Nome do parâmetro do carregador de histórico, usado na mensagem de valor nulo. */
    private static final String HISTORY_LOADER_NAME = "historyLoader";

    /** Arquivo FXML da tela, no pacote desta classe. */
    private static final String FXML_FILE = "helper-list.fxml";

    /** Título do diálogo de novo ajudante. */
    private static final String NEW_HELPER_TITLE = "Novo ajudante";

    /** Título do diálogo de edição. */
    private static final String EDIT_HELPER_TITLE = "Editar ajudante";

    /** Título do diálogo de remoção. */
    private static final String REMOVE_HELPERS_TITLE = "Remover ajudantes";

    /** Texto do contador de ajudantes ligados quando não há ajudantes. */
    private static final String NO_ENABLED = "0";

    /** Modelo do contador de ajudantes ligados; recebe os ligados e o total. */
    private static final String ENABLED_COUNT = "%d de %d";

    /**
     * Carrega a tela.
     *
     * @return a raiz da tela
     */
    public Parent createView() {
        return Fxml.load(HelperListController.class.getResource(FXML_FILE), this);
    }

    /**
     * Prepara os filtros e as animações depois que o FXML é carregado.
     */
    @FXML
    private void initialize() {
        filterAll.setUserData(TypeFilter.ALL);
        filterOrganizer.setUserData(TypeFilter.ORGANIZER);
        filterCleaner.setUserData(TypeFilter.CLEANER);
        for (ToggleButton button : List.of(filterAll, filterOrganizer, filterCleaner)) {
            button.setToggleGroup(filters);
        }
        filters.selectToggle(filterAll);
        filters.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                old.setSelected(true);
                return;
            }
            filter = (TypeFilter) now.getUserData();
            selection.clear();
            showCards();
        });

        Animations.rotateOnHover(newHelperIcon, newHelperButton, 90);
        Animations.rotateOnHover(firstHelperIcon, firstHelperButton, 90);
        Animations.floating(emptyIllustration);
        render();
    }

    /**
     * Ponto de entrada dos dados: será alimentado pelo ListHelpersUseCase.
     *
     * @param items ajudantes a exibir
     */
    public void showHelpers(List<HelperItem> items) {
        helpers.clear();
        helpers.addAll(items);
        selection.clear();
        render();
    }

    /**
     * Atualiza a tela inteira: contadores, lista ou tela vazia, e cartões.
     */
    private void render() {
        showCounts();
        boolean any = !helpers.isEmpty();
        Nodes.setShown(listSection, any);
        Nodes.setShown(emptyState, !any);
        showCards();
    }

    /**
     * Atualiza os contadores e os medidores de uso.
     */
    private void showCounts() {
        int organizers = countOf(HelperKind.ORGANIZER);
        int cleaners = countOf(HelperKind.CLEANER);
        long enabled = helpers.stream().filter(HelperItem::enabled).count();
        organizerCount.setText(String.valueOf(organizers));
        organizerUsage.setUsed(organizers);
        cleanerCount.setText(String.valueOf(cleaners));
        cleanerUsage.setUsed(cleaners);
        enabledCount.setText(helpers.isEmpty() ? NO_ENABLED : String.format(ENABLED_COUNT, enabled, helpers.size()));
        filterAllCount.setText(String.valueOf(helpers.size()));
        filterOrganizerCount.setText(String.valueOf(organizers));
        filterCleanerCount.setText(String.valueOf(cleaners));
    }

    /**
     * Recria os cartões dos ajudantes que passam pelo filtro.
     */
    private void showCards() {
        List<HelperItem> visible = visibleHelpers();
        List<Node> cards = new ArrayList<>();
        for (int i = 0; i < visible.size(); i++) {
            HelperItem item = visible.get(i);
            HelperCard card = new HelperCard(item);
            card.selectedProperty().set(selection.contains(item.id()));
            card.selectedProperty().addListener((obs, was, selected) -> {
                selection.set(item.id(), selected);
                showSelection();
            });
            card.enabledProperty().addListener((obs, was, on) -> {
                replace(item.withEnabled(on));
                showCounts();
            });
            card.setOnHistory(this::onHistory);
            Animations.riseIn(card, Duration.millis(i * 50L));
            cards.add(card);
        }
        cardGrid.getChildren().setAll(cards);
        Nodes.setShown(noResults, visible.isEmpty() && !helpers.isEmpty());
        showSelection();
    }

    /**
     * Mostra as ações da seleção. Com algo selecionado, elas tomam o lugar do filtro na mesma linha, sem
     * empurrar os cartões.
     */
    private void showSelection() {
        int count = selection.count();
        boolean active = count > 0;
        boolean wasActive = selectionActions.isVisible();
        selectionActions.setVisible(active);
        filterBar.setVisible(!active);
        if (active != wasActive) {
            Animations.fadeUpIn(active ? selectionActions : filterBar);
        }
        selectionCount.setText(String.valueOf(count));
        selectionText.setText(selection.summary());
        editButton.setDisable(!selection.canEdit());
        editIcon.setGlyph(selection.canEdit() ? Glyph.PENCIL : Glyph.LOCK);
        removeButton.setText(selection.removeLabel());
        boolean several = count > 1;
        if (several && !editHint.isVisible()) {
            Animations.fadeUpIn(editHint);
        }
        Nodes.setShown(editHint, several);

        List<HelperItem> visible = visibleHelpers();
        long selectedVisible = visible.stream().filter(item -> selection.contains(item.id())).count();
        selectAll.setSelected(!visible.isEmpty() && selectedVisible == visible.size());
        selectAll.setIndeterminate(selectedVisible > 0 && selectedVisible < visible.size());
    }

    /**
     * Marca ou desmarca todos os cartões visíveis, conforme a caixa de seleção geral.
     */
    @FXML
    private void onSelectAll() {
        boolean select = selectAll.isSelected();
        cards().forEach(card -> card.selectedProperty().set(select));
    }

    /**
     * Desmarca todos os cartões.
     */
    @FXML
    private void onClearSelection() {
        cards().forEach(card -> card.selectedProperty().set(false));
    }

    /**
     * Abre o diálogo de escolha do tipo e, depois, o formulário do tipo escolhido.
     */
    @FXML
    private void onNewHelper() {
        HelperTypeChooserController chooser =
                new HelperTypeChooserController(countOf(HelperKind.ORGANIZER), countOf(HelperKind.CLEANER));
        showDialog(chooser.createView(), NEW_HELPER_TITLE);
        chooser.chosenKind().ifPresent(kind -> openForm(kind, null));
    }

    /**
     * Abre o formulário do ajudante selecionado.
     */
    @FXML
    private void onEdit() {
        selection.editableId().flatMap(this::find).ifPresent(item -> openForm(item.kind(), item));
    }

    /**
     * Abre a confirmação de remoção dos ajudantes selecionados.
     */
    @FXML
    private void onRemove() {
        List<HelperItem> chosen = helpers.stream().filter(item -> selection.contains(item.id())).toList();
        if (chosen.isEmpty()) {
            return;
        }
        RemoveConfirmationController confirmation = new RemoveConfirmationController(chosen);
        showDialog(confirmation.createView(), REMOVE_HELPERS_TITLE);
        // A remoção de verdade chega com o RemoveHelperUseCase.
    }

    /**
     * Define como o histórico de um ajudante é buscado. Os casos de uso ainda não chegam a esta tela:
     * quem montar a tela com eles liga aqui um {@link HistoryLoader}. Sem isso, o histórico aparece vazio.
     *
     * @param historyLoader função do identificador do ajudante para os itens do histórico
     */
    public void setHistoryLoader(Function<String, List<ExecutionItem>> historyLoader) {
        this.historyLoader = Objects.requireNonNull(historyLoader, HISTORY_LOADER_NAME);
    }

    /**
     * Abre o histórico do ajudante do cartão cuja ação foi acionada.
     *
     * @param item ajudante escolhido
     */
    private void onHistory(HelperItem item) {
        presentHistory(historyOf(item));
    }

    /**
     * Monta o diálogo de histórico de um ajudante com os itens devolvidos pelo carregador.
     *
     * @param item ajudante cujo histórico será exibido
     * @return o controller do diálogo, com o histórico do ajudante
     */
    ExecutionHistoryController historyOf(HelperItem item) {
        return new ExecutionHistoryController(item.name(), historyLoader.apply(item.id()));
    }

    /**
     * Mostra o diálogo de histórico. Separado para que os testes não abram uma janela de verdade.
     *
     * @param history controller do diálogo de histórico
     */
    void presentHistory(ExecutionHistoryController history) {
        showDialog(history.createView(), HISTORY_TITLE);
    }

    /**
     * Abre o formulário do tipo informado.
     *
     * @param kind    tipo do ajudante
     * @param editing ajudante a editar, ou {@code null} para criar um novo
     */
    private void openForm(HelperKind kind, HelperItem editing) {
        HelperFormController form = kind == HelperKind.ORGANIZER
                ? new FolderOrganizerFormController(editing)
                : new CleanerFormController(editing);
        showDialog(form.createView(), editing == null ? NEW_HELPER_TITLE : EDIT_HELPER_TITLE);
    }

    /**
     * Mostra um diálogo modal, escurecendo a lista enquanto ele estiver aberto.
     *
     * @param view  conteúdo do diálogo
     * @param title título do diálogo
     */
    private void showDialog(Parent view, String title) {
        scrim.setOpacity(0);
        scrim.setVisible(true);
        Animations.fadeTo(scrim, 1, null);
        Dialogs.showModal(root.getScene().getWindow(), view, title);
        Animations.fadeTo(scrim, 0, () -> scrim.setVisible(false));
    }

    /**
     * Lista os ajudantes que passam pelo filtro ativo.
     *
     * @return ajudantes visíveis
     */
    private List<HelperItem> visibleHelpers() {
        return helpers.stream().filter(filter::matches).toList();
    }

    /**
     * Lista os cartões exibidos.
     *
     * @return cartões da grade
     */
    private List<HelperCard> cards() {
        return cardGrid.getChildren().stream().map(HelperCard.class::cast).toList();
    }

    /**
     * Conta os ajudantes de um tipo.
     *
     * @param kind tipo a contar
     * @return quantidade de ajudantes do tipo
     */
    private int countOf(HelperKind kind) {
        return (int) helpers.stream().filter(item -> item.kind() == kind).count();
    }

    /**
     * Procura um ajudante pelo identificador.
     *
     * @param id identificador do ajudante
     * @return o ajudante, ou vazio se não existir
     */
    private Optional<HelperItem> find(String id) {
        return helpers.stream().filter(item -> item.id().equals(id)).findFirst();
    }

    /**
     * Troca um ajudante pela versão atualizada, identificada pelo mesmo id.
     *
     * @param updated ajudante atualizado
     */
    private void replace(HelperItem updated) {
        helpers.replaceAll(item -> item.id().equals(updated.id()) ? updated : item);
    }
}
