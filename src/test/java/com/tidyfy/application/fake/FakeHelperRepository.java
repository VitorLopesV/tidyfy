package com.tidyfy.application.fake;

import com.tidyfy.application.port.HelperRepository;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositório de ajudantes em memória, para os testes dos casos de uso.
 * Mantém a ordem de criação (editar um ajudante não muda a posição dele) e permite
 * simular falha de gravação com {@link #failNextSaveWith(RuntimeException)}.
 */
public final class FakeHelperRepository implements HelperRepository {

    /** Ajudantes gravados, na ordem de criação. */
    private final Map<HelperId, Helper> helpers = new LinkedHashMap<>();

    /** Falha a lançar na próxima gravação; {@code null} quando não há. */
    private RuntimeException saveFailure;

    /** Quantidade de chamadas a {@link #save(Helper)} que terminaram com sucesso. */
    private int saveCount;

    /**
     * Prepara o estado inicial, sem contar como chamada de {@link #save(Helper)}.
     *
     * @param helper ajudante já existente
     * @return este fake, para encadear
     */
    public FakeHelperRepository with(Helper helper) {
        helpers.put(helper.id(), helper);
        return this;
    }

    /**
     * Faz a próxima gravação lançar a falha informada (uma vez só).
     *
     * @param failure falha a lançar
     */
    public void failNextSaveWith(RuntimeException failure) {
        this.saveFailure = failure;
    }

    /**
     * Informa quantas gravações foram feitas com sucesso por {@link #save(Helper)}.
     *
     * @return quantidade de gravações
     */
    public int saveCount() {
        return saveCount;
    }

    @Override
    public void save(Helper helper) {
        if (saveFailure != null) {
            RuntimeException failure = saveFailure;
            saveFailure = null;
            throw failure;
        }
        helpers.put(helper.id(), helper);
        saveCount++;
    }

    @Override
    public Optional<Helper> findById(HelperId id) {
        return Optional.ofNullable(helpers.get(id));
    }

    @Override
    public List<Helper> findAll() {
        return new ArrayList<>(helpers.values());
    }

    @Override
    public int countByType(HelperType type) {
        return (int) helpers.values().stream().filter(h -> h.type() == type).count();
    }

    @Override
    public void delete(HelperId id) {
        helpers.remove(id);
    }
}
