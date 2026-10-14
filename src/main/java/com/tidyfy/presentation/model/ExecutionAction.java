package com.tidyfy.presentation.model;

import java.util.Objects;

/**
 * Uma ação de uma execução, já em texto para a tela.
 *
 * @param text    texto curto exibido na linha (nomes de arquivo, não caminhos completos)
 * @param detail  caminhos completos, mostrados como dica; {@code null} quando não há caminho
 * @param failure {@code true} quando a ação é uma falha, que a tela destaca
 */
public record ExecutionAction(String text, String detail, boolean failure) {

    // Constantes

    /** Nome do parâmetro do texto, usado na mensagem de valor nulo. */
    private static final String TEXT_NAME = "text";

    /**
     * Valida os dados da ação.
     *
     * @throws NullPointerException se {@code text} for nulo
     */
    public ExecutionAction {
        Objects.requireNonNull(text, TEXT_NAME);
    }
}
