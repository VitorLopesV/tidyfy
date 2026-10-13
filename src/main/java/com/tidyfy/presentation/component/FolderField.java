package com.tidyfy.presentation.component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

/**
 * Mostra a pasta escolhida (ou que nenhuma foi escolhida) e abre o seletor de pastas do Windows.
 */
public final class FolderField extends HBox {

    /** Pasta escolhida; {@code null} enquanto nenhuma foi escolhida. */
    private final ObjectProperty<Path> folder = new SimpleObjectProperty<>(this, PROPERTY_FOLDER);

    /** Ícone da pasta. */
    private final Icon icon = new Icon(Glyph.FOLDER, 22);

    /** Nome da pasta escolhida. */
    private final Label name = new Label();

    /** Caminho completo da pasta escolhida. */
    private final Label path = new Label();

    /** Nome e caminho, exibidos quando há pasta escolhida. */
    private final VBox chosenTexts = new VBox(2, name, path);

    /** Aviso exibido enquanto nenhuma pasta foi escolhida. */
    private final Label empty = new Label(EMPTY_TEXT);

    /** Botão que abre o seletor de pastas. */
    private final Button choose = new Button();

    /** Título da janela do seletor de pastas. */
    private String chooserTitle = DEFAULT_CHOOSER_TITLE;

    // Constantes

    /** Nome do estado visual de "pasta escolhida". */
    private static final String CHOSEN_STATE = "chosen";

    /** Estado visual de "pasta escolhida". */
    private static final PseudoClass CHOSEN = PseudoClass.getPseudoClass(CHOSEN_STATE);

    /** Nome da propriedade da pasta. */
    private static final String PROPERTY_FOLDER = "folder";

    /** Aviso de que nenhuma pasta foi escolhida. */
    private static final String EMPTY_TEXT = "Nenhuma pasta escolhida";

    /** Título padrão do seletor de pastas. */
    private static final String DEFAULT_CHOOSER_TITLE = "Escolher pasta";

    /** Classe CSS do componente. */
    private static final String STYLE_CLASS = "folder-field";

    /** Classe CSS do ícone. */
    private static final String ICON_STYLE_CLASS = "folder-icon";

    /** Classe CSS do nome da pasta. */
    private static final String NAME_STYLE_CLASS = "folder-name";

    /** Classe CSS do caminho da pasta. */
    private static final String PATH_STYLE_CLASS = "folder-path";

    /** Classe CSS do aviso de pasta não escolhida. */
    private static final String EMPTY_STYLE_CLASS = "folder-empty";

    /** Classes CSS do botão destacado, usado enquanto não há pasta. */
    private static final String[] PRIMARY_BUTTON_CLASSES = {"button", "btn", "btn-primary", "btn-compact"};

    /** Classes CSS do botão discreto, usado quando já há pasta. */
    private static final String[] GHOST_BUTTON_CLASSES = {"button", "btn", "btn-ghost", "btn-compact"};

    /** Texto do botão enquanto não há pasta. */
    private static final String CHOOSE_TEXT = "Escolher pasta…";

    /** Texto do botão quando já há pasta. */
    private static final String CHANGE_TEXT = "Alterar…";

    /**
     * Cria o campo sem pasta escolhida.
     */
    public FolderField() {
        getStyleClass().add(STYLE_CLASS);
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        icon.getStyleClass().add(ICON_STYLE_CLASS);
        name.getStyleClass().add(NAME_STYLE_CLASS);
        path.getStyleClass().add(PATH_STYLE_CLASS);
        empty.getStyleClass().add(EMPTY_STYLE_CLASS);
        chosenTexts.setMinWidth(0);
        empty.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(chosenTexts, Priority.ALWAYS);
        HBox.setHgrow(empty, Priority.ALWAYS);
        choose.setOnAction(event -> chooseFolder());
        folder.addListener((obs, old, now) -> refresh(true));
        refresh(false);
    }

    /**
     * Devolve o nome para exibir de uma pasta: o último trecho do caminho, ou o caminho inteiro para raízes
     * como {@code C:\}.
     *
     * @param folder pasta a nomear
     * @return nome da pasta
     */
    public static String displayName(Path folder) {
        Path fileName = folder.getFileName();
        return fileName == null ? folder.toString() : fileName.toString();
    }

    /**
     * Atualiza o que o campo mostra conforme haja ou não pasta escolhida.
     *
     * @param animate {@code true} para animar a entrada do texto da pasta
     */
    private void refresh(boolean animate) {
        Path current = folder.get();
        pseudoClassStateChanged(CHOSEN, current != null);
        if (current == null) {
            choose.getStyleClass().setAll(PRIMARY_BUTTON_CLASSES);
            choose.setText(CHOOSE_TEXT);
            getChildren().setAll(icon, empty, choose);
            return;
        }
        name.setText(displayName(current));
        path.setText(current.toString());
        choose.getStyleClass().setAll(GHOST_BUTTON_CLASSES);
        choose.setText(CHANGE_TEXT);
        getChildren().setAll(icon, chosenTexts, choose);
        if (animate) {
            Animations.fadeUpIn(chosenTexts);
        }
    }

    /**
     * Abre o seletor de pastas, começando na pasta atual, e guarda a escolhida.
     */
    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(chooserTitle);
        Path current = folder.get();
        if (current != null && Files.isDirectory(current)) {
            chooser.setInitialDirectory(current.toFile());
        }
        File chosen = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (chosen != null) {
            folder.set(chosen.toPath());
        }
    }

    /**
     * Devolve a pasta escolhida.
     *
     * @return a pasta, ou {@code null} se nenhuma foi escolhida
     */
    public Path getFolder() {
        return folder.get();
    }

    /**
     * Define a pasta escolhida.
     *
     * @param value a pasta, ou {@code null} para limpar
     */
    public void setFolder(Path value) {
        folder.set(value);
    }

    /**
     * Devolve a propriedade da pasta escolhida.
     *
     * @return propriedade da pasta
     */
    public ObjectProperty<Path> folderProperty() {
        return folder;
    }

    /**
     * Devolve o título da janela do seletor de pastas.
     *
     * @return título do seletor
     */
    public String getChooserTitle() {
        return chooserTitle;
    }

    /**
     * Define o título da janela do seletor de pastas.
     *
     * @param value título do seletor
     */
    public void setChooserTitle(String value) {
        chooserTitle = value;
    }
}
