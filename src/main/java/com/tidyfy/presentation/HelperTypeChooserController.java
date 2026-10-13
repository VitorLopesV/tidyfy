package com.tidyfy.presentation;

import com.tidyfy.presentation.component.Fxml;
import com.tidyfy.presentation.component.Icon;
import com.tidyfy.presentation.component.Nodes;
import com.tidyfy.presentation.component.UsageMeter;
import com.tidyfy.presentation.model.HelperKind;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * "Novo ajudante": pede o tipo antes de abrir o formulário. O tipo que já está no limite fica bloqueado.
 */
public class HelperTypeChooserController {

    /** Raiz do diálogo. */
    @FXML private VBox root;

    /** Cartão que escolhe o tipo organização. */
    @FXML private Button organizerCard;

    /** Descrição do tipo organização. */
    @FXML private Label organizerDescription;

    /** Aviso de limite atingido do tipo organização. */
    @FXML private Label organizerLimitBadge;

    /** Seta do cartão de organização. */
    @FXML private Icon organizerArrow;

    /** Quantidade de ajudantes de organização. */
    @FXML private Label organizerCount;

    /** Medidor de uso do limite de organização. */
    @FXML private UsageMeter organizerUsage;

    /** Cartão que escolhe o tipo limpeza. */
    @FXML private Button cleanerCard;

    /** Descrição do tipo limpeza. */
    @FXML private Label cleanerDescription;

    /** Aviso de limite atingido do tipo limpeza. */
    @FXML private Label cleanerLimitBadge;

    /** Seta do cartão de limpeza. */
    @FXML private Icon cleanerArrow;

    /** Quantidade de ajudantes de limpeza. */
    @FXML private Label cleanerCount;

    /** Medidor de uso do limite de limpeza. */
    @FXML private UsageMeter cleanerUsage;

    /** Quantidade de ajudantes de organização já criados. */
    private final int organizers;

    /** Quantidade de ajudantes de limpeza já criados. */
    private final int cleaners;

    /** Tipo escolhido; {@code null} enquanto nenhum foi escolhido. */
    private HelperKind chosen;

    // Constantes

    /** Arquivo FXML do diálogo, no pacote desta classe. */
    private static final String FXML_FILE = "helper-type-chooser.fxml";

    /** Nome do tipo organização, usado na mensagem de limite. */
    private static final String ORGANIZER_NAME = "organização";

    /** Nome do tipo limpeza, usado na mensagem de limite. */
    private static final String CLEANER_NAME = "limpeza";

    /** Modelo da mensagem de limite atingido; recebe o limite e o nome do tipo. */
    private static final String LIMIT_REACHED = "Você já tem %d ajudantes de %s. Remova um deles para criar outro.";

    /**
     * Cria o diálogo.
     *
     * @param organizers quantidade de ajudantes de organização já criados
     * @param cleaners   quantidade de ajudantes de limpeza já criados
     */
    public HelperTypeChooserController(int organizers, int cleaners) {
        this.organizers = organizers;
        this.cleaners = cleaners;
    }

    /**
     * Carrega o diálogo.
     *
     * @return a raiz do diálogo
     */
    public Parent createView() {
        return Fxml.load(HelperTypeChooserController.class.getResource(FXML_FILE), this);
    }

    /**
     * Mostra o uso de cada tipo depois que o FXML é carregado.
     */
    @FXML
    private void initialize() {
        Nodes.stretchGraphic(organizerCard, true);
        Nodes.stretchGraphic(cleanerCard, true);
        show(organizerCard, organizerDescription, organizerLimitBadge, organizerArrow, organizerCount, organizerUsage,
                organizers, ORGANIZER_NAME);
        show(cleanerCard, cleanerDescription, cleanerLimitBadge, cleanerArrow, cleanerCount, cleanerUsage,
                cleaners, CLEANER_NAME);
    }

    /**
     * Mostra o uso de um tipo e bloqueia o cartão quando o limite foi atingido.
     *
     * @param card       cartão do tipo
     * @param description descrição do tipo
     * @param limitBadge aviso de limite atingido
     * @param arrow      seta do cartão
     * @param count      contador de ajudantes
     * @param usage      medidor de uso
     * @param created    quantidade de ajudantes já criados do tipo
     * @param kindName   nome do tipo, usado na mensagem de limite
     */
    private static void show(Button card, Label description, Label limitBadge, Icon arrow, Label count,
                             UsageMeter usage, int created, String kindName) {
        count.setText(String.valueOf(created));
        usage.setUsed(created);
        boolean full = created >= HelperKind.LIMIT_PER_TYPE;
        card.setDisable(full);
        Nodes.setShown(limitBadge, full);
        Nodes.setShown(arrow, !full);
        if (full) {
            description.setText(String.format(LIMIT_REACHED, HelperKind.LIMIT_PER_TYPE, kindName));
        }
    }

    /**
     * Escolhe o tipo organização e fecha o diálogo.
     */
    @FXML
    private void onOrganizer() {
        choose(HelperKind.ORGANIZER);
    }

    /**
     * Escolhe o tipo limpeza e fecha o diálogo.
     */
    @FXML
    private void onCleaner() {
        choose(HelperKind.CLEANER);
    }

    /**
     * Fecha o diálogo.
     */
    @FXML
    private void onClose() {
        root.getScene().getWindow().hide();
    }

    /**
     * Guarda o tipo escolhido e fecha o diálogo.
     *
     * @param kind tipo escolhido
     */
    private void choose(HelperKind kind) {
        chosen = kind;
        onClose();
    }

    /**
     * Devolve o tipo escolhido.
     *
     * @return o tipo, ou vazio se o diálogo foi fechado sem escolha
     */
    public Optional<HelperKind> chosenKind() {
        return Optional.ofNullable(chosen);
    }
}
