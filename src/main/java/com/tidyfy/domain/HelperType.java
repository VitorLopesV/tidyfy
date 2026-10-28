package com.tidyfy.domain;

/**
 * Tipo de um ajudante. Cada tipo pode ter no máximo {@link #MAX_PER_TYPE} ajudantes.
 */
public enum HelperType {

    /** Organiza os arquivos de uma pasta em subpastas por tipo. */
    FOLDER_ORGANIZER,

    /** Esvazia a lixeira ou remove os arquivos de uma pasta. */
    CLEANER,

    /** Reposiciona os ícones da área de trabalho em um layout; só pode existir um. */
    DESKTOP_ARRANGER;

    // Constantes

    /** Quantidade máxima de ajudantes de cada tipo (exceto o Organizador da área de trabalho). */
    public static final int MAX_PER_TYPE = 20;

    /** Quantidade máxima de Organizadores da área de trabalho: a área de trabalho é única. */
    public static final int MAX_DESKTOP_ARRANGERS = 1;

    /**
     * Garante que ainda é possível criar um ajudante deste tipo.
     * A checagem vale só para a criação; editar um ajudante existente não passa por ela.
     *
     * @param existingCount quantos ajudantes deste tipo já existem
     * @throws HelperLimitReachedException           se a quantidade já atingiu {@link #MAX_PER_TYPE}
     * @throws DesktopArrangerAlreadyExistsException se já existe um {@link #DESKTOP_ARRANGER}
     */
    public void assertCanCreate(int existingCount) {
        if (this == DESKTOP_ARRANGER) {
            if (existingCount >= MAX_DESKTOP_ARRANGERS) {
                throw new DesktopArrangerAlreadyExistsException();
            }
        } else if (existingCount >= MAX_PER_TYPE) {
            throw new HelperLimitReachedException(this);
        }
    }
}
