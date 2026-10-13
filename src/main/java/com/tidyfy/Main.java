package com.tidyfy;

import com.tidyfy.presentation.TidyfyApplication;
import javafx.application.Application;

/**
 * Ponto de entrada do Tidyfy: abre a interface gráfica.
 */
public class Main {

    /**
     * Inicia a aplicação JavaFX.
     *
     * @param args argumentos de linha de comando, repassados à aplicação
     */
    public static void main(String[] args) {
        Application.launch(TidyfyApplication.class, args);
    }
}
