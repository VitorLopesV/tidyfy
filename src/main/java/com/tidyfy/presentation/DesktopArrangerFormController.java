package com.tidyfy.presentation;

import com.tidyfy.application.usecase.SaveHelperUseCase;
import com.tidyfy.domain.DesktopArrangerHelper;
import com.tidyfy.domain.DesktopLayout;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import com.tidyfy.presentation.component.LayoutThumbnail;
import com.tidyfy.presentation.model.DesktopLayoutText;
import com.tidyfy.presentation.model.HelperItem;
import java.time.Clock;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Formulário do Organizador da área de trabalho: nome, layout dos ícones (cada opção com uma miniatura da
 * tela), frequência e ligado/desligado. Não tem campo de pasta: o alvo é sempre a área de trabalho.
 */
public class DesktopArrangerFormController extends HelperFormController {

    /** Opções de layout, uma por {@link DesktopLayout}. */
    @FXML private FlowPane layoutOptions;

    /** Local da prévia grande do layout escolhido. */
    @FXML private StackPane previewHolder;

    /** Nome do layout escolhido, na prévia. */
    @FXML private Label previewName;

    /** Descrição do layout escolhido, na prévia. */
    @FXML private Label previewDescription;

    /** Grupo das opções de layout: sempre há uma escolhida. */
    private final ToggleGroup layouts = new ToggleGroup();

    // Constantes

    /** Arquivo FXML do formulário, no pacote desta classe. */
    private static final String FXML_FILE = "desktop-arranger-form.fxml";

    /** Layout escolhido ao criar um novo ajudante. */
    private static final DesktopLayout DEFAULT_LAYOUT = DesktopLayout.LEFT;

    /** Classe CSS de cada opção de layout. */
    private static final String OPTION_STYLE_CLASS = "layout-option";

    /** Classe CSS do nome do layout na opção. */
    private static final String OPTION_LABEL_STYLE_CLASS = "layout-option-label";

    /** Classe CSS da descrição do layout na opção. */
    private static final String OPTION_DESCRIPTION_STYLE_CLASS = "layout-option-desc";

    /** Largura da miniatura de cada opção, em pixels. */
    private static final double OPTION_THUMBNAIL_WIDTH = 150;

    /** Largura da miniatura da prévia, em pixels. */
    private static final double PREVIEW_THUMBNAIL_WIDTH = 330;

    /** Espaço entre a miniatura e os textos da opção. */
    private static final double OPTION_SPACING = 6;

    /** Modelo do texto acessível de cada opção; recebe o nome e a descrição do layout. */
    private static final String OPTION_ACCESSIBLE_TEXT = "%s. %s";

    /**
     * Cria o formulário.
     *
     * @param editing ajudante sendo editado, ou {@code null} para criar um novo
     */
    public DesktopArrangerFormController(HelperItem editing) {
        super(editing);
    }

    /**
     * Cria o formulário ligado ao caso de uso de salvar.
     *
     * @param editing    ajudante sendo editado, ou {@code null} para criar um novo
     * @param saveHelper caso de uso que grava o ajudante
     * @param clock      relógio que define a data de início da frequência
     */
    public DesktopArrangerFormController(HelperItem editing, SaveHelperUseCase saveHelper, Clock clock) {
        super(editing, saveHelper, clock);
    }

    /**
     * Devolve o layout escolhido na tela.
     *
     * @return o layout selecionado
     */
    public DesktopLayout selectedLayout() {
        return (DesktopLayout) layouts.getSelectedToggle().getUserData();
    }

    /**
     * Monta o ajudante da área de trabalho com o layout escolhido.
     *
     * @return o ajudante do domínio
     */
    @Override
    protected Helper buildHelper(HelperId id, HelperName name, Schedule schedule, boolean enabled,
                                 HelperItem editing) {
        return new DesktopArrangerHelper(id, name, schedule, enabled, selectedLayout());
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
     * Informa se falta escolher o layout.
     *
     * @return binding verdadeiro enquanto nenhum layout estiver escolhido
     */
    @Override
    protected BooleanBinding targetMissing() {
        return layouts.selectedToggleProperty().isNull();
    }

    /**
     * Seleciona o layout do ajudante em edição.
     *
     * @param item ajudante em edição
     */
    @Override
    protected void showTarget(HelperItem item) {
        if (item.desktopLayout() != null) {
            select(item.desktopLayout());
        }
    }

    /**
     * Cria as opções de layout, escolhe o padrão e liga a prévia, depois prepara o resto do formulário.
     */
    @Override
    @FXML
    protected void initialize() {
        for (DesktopLayout layout : DesktopLayout.values()) {
            layoutOptions.getChildren().add(option(layout));
        }
        select(DEFAULT_LAYOUT);
        layouts.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                old.setSelected(true);
                return;
            }
            showPreview((DesktopLayout) now.getUserData());
        });
        showPreview(DEFAULT_LAYOUT);
        super.initialize();
    }

    /**
     * Cria o botão de uma opção de layout, com a miniatura, o nome e a descrição.
     *
     * @param layout o layout da opção
     * @return o botão
     */
    private ToggleButton option(DesktopLayout layout) {
        String label = DesktopLayoutText.label(layout);
        String description = DesktopLayoutText.description(layout);
        Label name = new Label(label);
        name.getStyleClass().add(OPTION_LABEL_STYLE_CLASS);
        Label detail = new Label(description);
        detail.getStyleClass().add(OPTION_DESCRIPTION_STYLE_CLASS);
        detail.setWrapText(true);
        detail.setMaxWidth(OPTION_THUMBNAIL_WIDTH);
        VBox content = new VBox(OPTION_SPACING, new LayoutThumbnail(layout, OPTION_THUMBNAIL_WIDTH), name, detail);
        ToggleButton button = new ToggleButton();
        button.setGraphic(content);
        button.getStyleClass().add(OPTION_STYLE_CLASS);
        button.setUserData(layout);
        button.setToggleGroup(layouts);
        button.setAccessibleText(String.format(OPTION_ACCESSIBLE_TEXT, label, description));
        return button;
    }

    /**
     * Seleciona a opção do layout.
     *
     * @param layout o layout a selecionar
     */
    private void select(DesktopLayout layout) {
        for (Toggle toggle : layouts.getToggles()) {
            if (toggle.getUserData() == layout) {
                layouts.selectToggle(toggle);
                return;
            }
        }
    }

    /**
     * Mostra a miniatura grande, o nome e a descrição do layout escolhido.
     *
     * @param layout o layout escolhido
     */
    private void showPreview(DesktopLayout layout) {
        Node thumbnail = new LayoutThumbnail(layout, PREVIEW_THUMBNAIL_WIDTH);
        previewHolder.getChildren().setAll(thumbnail);
        previewName.setText(DesktopLayoutText.label(layout));
        previewDescription.setText(DesktopLayoutText.description(layout));
    }
}
