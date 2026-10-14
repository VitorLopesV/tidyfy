package com.tidyfy.presentation;

import com.tidyfy.application.usecase.ListExecutionHistoryUseCase;
import com.tidyfy.domain.HelperId;
import com.tidyfy.presentation.model.ExecutionItem;
import com.tidyfy.presentation.model.ExecutionItemMapper;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Busca o histórico de um ajudante pelo identificador que a tela conhece (texto) e o entrega já convertido
 * nos itens exibidos, do mais recente para o mais antigo.
 */
public final class HistoryLoader implements Function<String, List<ExecutionItem>> {

    /** Caso de uso que lista as execuções do ajudante. */
    private final ListExecutionHistoryUseCase useCase;

    /** Conversor dos registros em itens de tela. */
    private final ExecutionItemMapper mapper;

    // Constantes

    /** Nome do parâmetro do caso de uso, usado na mensagem de valor nulo. */
    private static final String USE_CASE_NAME = "useCase";

    /** Nome do parâmetro do conversor, usado na mensagem de valor nulo. */
    private static final String MAPPER_NAME = "mapper";

    /** Nome do parâmetro do identificador, usado na mensagem de valor nulo. */
    private static final String HELPER_ID_NAME = "helperId";

    /**
     * Cria o carregador.
     *
     * @param useCase caso de uso que lista o histórico
     * @param mapper  conversor dos registros em itens de tela
     * @throws NullPointerException se algum parâmetro for nulo
     */
    public HistoryLoader(ListExecutionHistoryUseCase useCase, ExecutionItemMapper mapper) {
        this.useCase = Objects.requireNonNull(useCase, USE_CASE_NAME);
        this.mapper = Objects.requireNonNull(mapper, MAPPER_NAME);
    }

    /**
     * Carrega o histórico de um ajudante.
     *
     * @param helperId identificador do ajudante, em texto (um UUID)
     * @return os itens da tela, do mais recente para o mais antigo; vazia se não houver execuções
     * @throws NullPointerException     se o identificador for nulo
     * @throws IllegalArgumentException se o identificador não for um UUID
     */
    @Override
    public List<ExecutionItem> apply(String helperId) {
        Objects.requireNonNull(helperId, HELPER_ID_NAME);
        return mapper.mapAll(useCase.execute(new HelperId(UUID.fromString(helperId))));
    }
}
