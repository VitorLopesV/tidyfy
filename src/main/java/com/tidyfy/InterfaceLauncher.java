package com.tidyfy;

import com.tidyfy.presentation.TidyfyApplication;
import com.tidyfy.presentation.UiServices;
import java.time.Clock;
import javafx.application.Application;

/**
 * Abre a interface JavaFX. Fica separada do {@link Main} para que o JavaFX só seja carregado quando
 * esta classe é usada, isto é, nunca no modo {@code --run}.
 */
final class InterfaceLauncher {

    /**
     * Construtor privado: classe utilitária.
     */
    private InterfaceLauncher() {
    }

    /**
     * Entrega à interface os casos de uso do container, inicia a aplicação JavaFX e só retorna quando a
     * interface é fechada.
     *
     * @param container container com os casos de uso
     * @param clock     relógio da interface
     * @param args      argumentos de linha de comando, repassados à aplicação
     */
    static void launch(AppContainer container, Clock clock, String[] args) {
        TidyfyApplication.configure(new UiServices(container.listHelpers(), container.saveHelper(),
                container.removeHelper(), container.listExecutionHistory(), clock));
        Application.launch(TidyfyApplication.class, args);
    }
}
