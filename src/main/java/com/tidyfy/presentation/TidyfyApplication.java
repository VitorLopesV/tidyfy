package com.tidyfy.presentation;

import com.tidyfy.presentation.component.Logo;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Aplicação JavaFX do Tidyfy: monta a janela principal com a lista de ajudantes.
 */
public class TidyfyApplication extends Application {

    // Constantes

    /** Nome do arquivo de estilos, no mesmo pacote desta classe. */
    static final String STYLESHEET = "tidyfy.css";

    /** Título da janela. */
    private static final String TITLE = "Tidyfy";

    /**
     * Abre a janela principal.
     *
     * @param stage janela fornecida pelo JavaFX
     */
    @Override
    public void start(Stage stage) {
        HelperListController helperList = new HelperListController();
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(helperList.createView(),
                Math.min(1280, screen.getWidth() - 80),
                Math.min(900, screen.getHeight() - 40));
        scene.getStylesheets().add(stylesheet());

        stage.setTitle(TITLE);
        stage.getIcons().addAll(Logo.image(16), Logo.image(32), Logo.image(64));
        stage.setMinWidth(960);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Devolve o endereço do arquivo de estilos.
     *
     * @return endereço do {@value #STYLESHEET}, pronto para uso em {@code Scene.getStylesheets()}
     */
    static String stylesheet() {
        return TidyfyApplication.class.getResource(STYLESHEET).toExternalForm();
    }
}
