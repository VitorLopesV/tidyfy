package com.tidyfy.application.port;

import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperType;
import java.util.List;
import java.util.Optional;

/**
 * Porta de persistência da configuração dos ajudantes.
 * Falhas de leitura ou gravação são lançadas como {@link java.io.UncheckedIOException}.
 */
public interface HelperRepository {

    /**
     * Grava o ajudante. Se já existir um com o mesmo id, ele é substituído (edição);
     * senão, é incluído.
     *
     * @param helper ajudante a gravar
     */
    void save(Helper helper);

    /**
     * Busca um ajudante pelo id.
     *
     * @param id identificador do ajudante
     * @return o ajudante, ou vazio se não existir
     */
    Optional<Helper> findById(HelperId id);

    /**
     * Lista todos os ajudantes gravados.
     *
     * @return lista de ajudantes na ordem em que foram criados; vazia se não houver nenhum
     */
    List<Helper> findAll();

    /**
     * Conta os ajudantes de um tipo.
     *
     * @param type tipo a contar
     * @return quantidade de ajudantes do tipo (zero se não houver)
     */
    int countByType(HelperType type);

    /**
     * Remove o ajudante. Id inexistente não é erro: nada acontece.
     *
     * @param id identificador do ajudante
     */
    void delete(HelperId id);
}
