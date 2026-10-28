package com.tidyfy.presentation.model;

/**
 * Filtro da lista de ajudantes por tipo.
 */
public enum TypeFilter {

    /** Mostra todos os tipos. */
    ALL(null),

    /** Mostra só os ajudantes de organização. */
    ORGANIZER(HelperKind.ORGANIZER),

    /** Mostra só os ajudantes de limpeza. */
    CLEANER(HelperKind.CLEANER),

    /** Mostra só o ajudante da área de trabalho. */
    DESKTOP(HelperKind.DESKTOP);

    /** Tipo aceito pelo filtro; {@code null} aceita todos. */
    private final HelperKind kind;

    /**
     * Cria um filtro.
     *
     * @param kind tipo aceito, ou {@code null} para aceitar todos
     */
    TypeFilter(HelperKind kind) {
        this.kind = kind;
    }

    /**
     * Informa se o ajudante passa pelo filtro.
     *
     * @param item ajudante a testar
     * @return {@code true} quando o ajudante é do tipo do filtro, ou o filtro aceita todos
     */
    public boolean matches(HelperItem item) {
        return kind == null || item.kind() == kind;
    }
}
