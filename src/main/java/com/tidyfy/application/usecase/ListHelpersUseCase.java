package com.tidyfy.application.usecase;

import com.tidyfy.application.port.HelperRepository;
import com.tidyfy.domain.Helper;
import java.util.List;
import java.util.Objects;

/**
 * Lista todos os ajudantes salvos.
 */
public final class ListHelpersUseCase {

    /** Repositório de onde os ajudantes são lidos. */
    private final HelperRepository repository;

    // Constantes

    /** Nome do parâmetro do repositório, usado na mensagem de valor nulo. */
    private static final String REPOSITORY_NAME = "repository";

    /**
     * Cria o caso de uso.
     *
     * @param repository repositório dos ajudantes
     * @throws NullPointerException se o repositório for nulo
     */
    public ListHelpersUseCase(HelperRepository repository) {
        this.repository = Objects.requireNonNull(repository, REPOSITORY_NAME);
    }

    /**
     * Devolve todos os ajudantes salvos, na ordem do repositório (a de criação).
     *
     * @return lista imutável de ajudantes; vazia se não houver nenhum
     * @throws java.io.UncheckedIOException se a leitura do repositório falhar
     */
    public List<Helper> execute() {
        return List.copyOf(repository.findAll());
    }
}
