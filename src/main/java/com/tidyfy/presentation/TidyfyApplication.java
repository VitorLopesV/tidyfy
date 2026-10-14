package com.tidyfy.presentation;

import com.tidyfy.presentation.component.Logo;
import com.tidyfy.presentation.model.ExecutionItemMapper;
import com.tidyfy.presentation.model.HelperItemMapper;
import java.time.ZoneId;
import java.util.Objects;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Aplicação JavaFX do Tidyfy: monta a janela principal com a lista de ajudantes.
 */
public class TidyfyApplication extends Application {

    /** Serviços entregues antes de {@link Application#launch}; o JavaFX instancia a aplicação sem argumentos. */
    private static UiServices services;

    // Constantes

    /** Nome do arquivo de estilos, no mesmo pacote desta classe. */
    static final String STYLESHEET = "tidyfy.css";

    /** Título da janela. */
    private static final String TITLE = "Tidyfy";

    /** Mensagem de quando a aplicação abre sem os serviços configurados. */
    private static final String NOT_CONFIGURED =
            "TidyfyApplication.configure(UiServices) deve ser chamado antes de abrir a interface";

    /** Nome do parâmetro dos serviços, usado na mensagem de valor nulo. */
    private static final String SERVICES_NAME = "services";

    /**
     * Define os serviços que a interface usa. Deve ser chamado antes de {@link Application#launch}.
     *
     * @param uiServices casos de uso e relógio da interface
     * @throws NullPointerException se {@code uiServices} for nulo
     */
    public static void configure(UiServices uiServices) {
        services = Objects.requireNonNull(uiServices, SERVICES_NAME);
    }

    /**
     * Abre a janela principal.
     *
     * @param stage janela fornecida pelo JavaFX
     */
    @Override
    public void start(Stage stage) {
        UiServices ui = services;
        if (ui == null) {
            throw new IllegalStateException(NOT_CONFIGURED);
        }
        HelperListController helperList =
                new HelperListController(ui.listHelpers(), new HelperItemMapper(ui.clock()));
        helperList.setHistoryLoader(new HistoryLoader(ui.listExecutionHistory(),
                new ExecutionItemMapper(ZoneId.systemDefault())));
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
