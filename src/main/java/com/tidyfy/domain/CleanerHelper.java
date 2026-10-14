package com.tidyfy.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * Ajudante que limpa um único alvo: a lixeira do Windows ou uma pasta.
 * O limite de idade é opcional; sem ele, limpa tudo.
 *
 * @param id       identificador do ajudante
 * @param name     nome definido pelo usuário
 * @param schedule frequência de execução
 * @param enabled  se o ajudante está ligado
 * @param target   o que será limpo
 * @param ageLimit idade máxima dos arquivos mantidos; {@code null} quando ausente (limpa tudo).
 *                 Prefira {@link #findAgeLimit()} para lidar com a ausência
 */
public record CleanerHelper(HelperId id, HelperName name, Schedule schedule, boolean enabled,
                            CleanupTarget target, AgeLimit ageLimit) implements Helper {

    // Constantes

    /** Nome do parâmetro do identificador, usado na mensagem de valor nulo. */
    private static final String ID_NAME = "id";

    /** Nome do parâmetro do nome, usado na mensagem de valor nulo. */
    private static final String NAME_NAME = "name";

    /** Nome do parâmetro da frequência, usado na mensagem de valor nulo. */
    private static final String SCHEDULE_NAME = "schedule";

    /** Nome do parâmetro do alvo, usado na mensagem de valor nulo. */
    private static final String TARGET_NAME = "target";

    /**
     * Valida os dados do ajudante. O limite de idade pode ser nulo.
     *
     * @throws NullPointerException se {@code id}, {@code name}, {@code schedule} ou {@code target} for nulo
     */
    public CleanerHelper {
        Objects.requireNonNull(id, ID_NAME);
        Objects.requireNonNull(name, NAME_NAME);
        Objects.requireNonNull(schedule, SCHEDULE_NAME);
        Objects.requireNonNull(target, TARGET_NAME);
    }

    /**
     * Tipo deste ajudante.
     *
     * @return {@link HelperType#CLEANER}
     */
    @Override
    public HelperType type() {
        return HelperType.CLEANER;
    }

    /**
     * Limite de idade como valor opcional.
     *
     * @return o limite, ou vazio quando o ajudante limpa tudo
     */
    public Optional<AgeLimit> findAgeLimit() {
        return Optional.ofNullable(ageLimit);
    }
}
