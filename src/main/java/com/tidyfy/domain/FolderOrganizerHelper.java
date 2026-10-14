package com.tidyfy.domain;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Ajudante que organiza os arquivos de uma única pasta em subpastas por tipo.
 *
 * @param id       identificador do ajudante
 * @param name     nome definido pelo usuário
 * @param schedule frequência de execução
 * @param enabled  se o ajudante está ligado
 * @param folder   pasta a organizar
 */
public record FolderOrganizerHelper(HelperId id, HelperName name, Schedule schedule, boolean enabled, Path folder)
        implements Helper {

    // Constantes

    /** Nome do parâmetro do identificador, usado na mensagem de valor nulo. */
    private static final String ID_NAME = "id";

    /** Nome do parâmetro do nome, usado na mensagem de valor nulo. */
    private static final String NAME_NAME = "name";

    /** Nome do parâmetro da frequência, usado na mensagem de valor nulo. */
    private static final String SCHEDULE_NAME = "schedule";

    /** Nome do parâmetro da pasta, usado na mensagem de valor nulo. */
    private static final String FOLDER_NAME = "folder";

    /**
     * Valida os dados do ajudante.
     *
     * @throws NullPointerException se {@code id}, {@code name}, {@code schedule} ou {@code folder} for nulo
     */
    public FolderOrganizerHelper {
        Objects.requireNonNull(id, ID_NAME);
        Objects.requireNonNull(name, NAME_NAME);
        Objects.requireNonNull(schedule, SCHEDULE_NAME);
        Objects.requireNonNull(folder, FOLDER_NAME);
    }

    /**
     * Tipo deste ajudante.
     *
     * @return {@link HelperType#FOLDER_ORGANIZER}
     */
    @Override
    public HelperType type() {
        return HelperType.FOLDER_ORGANIZER;
    }
}
