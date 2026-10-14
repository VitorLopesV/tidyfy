package com.tidyfy;

import com.tidyfy.presentation.TidyfyApplication;
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
     * Inicia a aplicação JavaFX e só retorna quando a interface é fechada.
     *
     * @param args argumentos de linha de comando, repassados à aplicação
     */
    static void launch(String[] args) {
        Application.launch(TidyfyApplication.class, args);
    }
}
