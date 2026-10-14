package com.tidyfy.presentation;

import com.tidyfy.application.usecase.ListExecutionHistoryUseCase;
import com.tidyfy.application.usecase.ListHelpersUseCase;
import com.tidyfy.application.usecase.RemoveHelperUseCase;
import com.tidyfy.application.usecase.SaveHelperUseCase;
import java.time.Clock;
import java.util.Objects;

/**
 * Tudo de que a interface precisa para falar com a aplicação: os casos de uso e o relógio. É montado na
 * composição ({@code InterfaceLauncher}) e entregue à {@link TidyfyApplication}, que o JavaFX instancia
 * sem argumentos. Assim a camada de apresentação não conhece o {@code AppContainer} nem a infraestrutura.
 *
 * @param listHelpers          caso de uso de listar os ajudantes
 * @param saveHelper           caso de uso de salvar (criar ou editar) um ajudante
 * @param removeHelper         caso de uso de remover ajudantes
 * @param listExecutionHistory caso de uso de consultar o histórico de execuções
 * @param clock                relógio que define "agora" para os textos da tela
 */
public record UiServices(ListHelpersUseCase listHelpers, SaveHelperUseCase saveHelper,
                         RemoveHelperUseCase removeHelper, ListExecutionHistoryUseCase listExecutionHistory,
                         Clock clock) {

    // Constantes

    /** Nome do componente de listar ajudantes, usado na mensagem de valor nulo. */
    private static final String LIST_HELPERS_NAME = "listHelpers";

    /** Nome do componente de salvar, usado na mensagem de valor nulo. */
    private static final String SAVE_HELPER_NAME = "saveHelper";

    /** Nome do componente de remover, usado na mensagem de valor nulo. */
    private static final String REMOVE_HELPER_NAME = "removeHelper";

    /** Nome do componente de histórico, usado na mensagem de valor nulo. */
    private static final String LIST_EXECUTION_HISTORY_NAME = "listExecutionHistory";

    /** Nome do componente do relógio, usado na mensagem de valor nulo. */
    private static final String CLOCK_NAME = "clock";

    /**
     * Valida que nenhum componente é nulo.
     *
     * @throws NullPointerException se algum componente for nulo
     */
    public UiServices {
        Objects.requireNonNull(listHelpers, LIST_HELPERS_NAME);
        Objects.requireNonNull(saveHelper, SAVE_HELPER_NAME);
        Objects.requireNonNull(removeHelper, REMOVE_HELPER_NAME);
        Objects.requireNonNull(listExecutionHistory, LIST_EXECUTION_HISTORY_NAME);
        Objects.requireNonNull(clock, CLOCK_NAME);
    }
}
