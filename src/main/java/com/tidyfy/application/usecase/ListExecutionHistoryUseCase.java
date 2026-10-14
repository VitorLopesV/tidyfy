package com.tidyfy.application.usecase;

import com.tidyfy.application.port.ExecutionHistoryRepository;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Lista as execuções de um ajudante, da mais recente para a mais antiga.
 */
public final class ListExecutionHistoryUseCase {

    /** Repositório de onde o histórico é lido. */
    private final ExecutionHistoryRepository history;

    // Constantes

    /** Nome do parâmetro do histórico, usado na mensagem de valor nulo. */
    private static final String HISTORY_NAME = "history";

    /** Nome do parâmetro do identificador, usado na mensagem de valor nulo. */
    private static final String ID_NAME = "id";

    /**
     * Cria o caso de uso.
     *
     * @param history repositório do histórico de execuções
     * @throws NullPointerException se o repositório for nulo
     */
    public ListExecutionHistoryUseCase(ExecutionHistoryRepository history) {
        this.history = Objects.requireNonNull(history, HISTORY_NAME);
    }

    /**
     * Devolve os registros do ajudante, do mais recente para o mais antigo. A ordem é garantida aqui
     * (ordenação estável por instante de execução decrescente), sem depender do repositório.
     * Registros vencidos (mais de 30 dias) que ainda estejam no repositório não são filtrados:
     * quem os apaga é a execução do ajudante.
     *
     * @param id identificador do ajudante
     * @return lista imutável de registros; vazia se o ajudante não tiver nenhum
     * @throws NullPointerException se o id for nulo
     * @throws java.io.UncheckedIOException se a leitura do histórico falhar
     */
    public List<ExecutionRecord> execute(HelperId id) {
        Objects.requireNonNull(id, ID_NAME);
        return history.findByHelper(id).stream()
                .sorted(Comparator.comparing(ExecutionRecord::executedAt).reversed())
                .toList();
    }
}
