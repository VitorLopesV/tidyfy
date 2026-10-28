package com.tidyfy.domain;

/**
 * Ajudante: uma tarefa de limpeza ou organização que roda periodicamente.
 * Cada ajudante atua em um único alvo e só existem os tipos listados em {@link HelperType}.
 */
public sealed interface Helper permits FolderOrganizerHelper, CleanerHelper, DesktopArrangerHelper {

    /**
     * Identificador único do ajudante.
     *
     * @return o identificador
     */
    HelperId id();

    /**
     * Nome do ajudante, definido pelo usuário.
     *
     * @return o nome
     */
    HelperName name();

    /**
     * Tipo do ajudante.
     *
     * @return o tipo
     */
    HelperType type();

    /**
     * Frequência com que o ajudante roda.
     *
     * @return a frequência
     */
    Schedule schedule();

    /**
     * Diz se o ajudante está ligado. Ajudante desligado não fica registrado no Agendador nem executa.
     *
     * @return {@code true} se estiver ligado
     */
    boolean enabled();

    /**
     * Devolve uma cópia do ajudante com o estado ligado/desligado trocado; todo o resto é preservado.
     *
     * @param enabled {@code true} para ligar, {@code false} para desligar
     * @return a cópia, do mesmo tipo deste ajudante
     */
    Helper withEnabled(boolean enabled);
}
