package com.tidyfy.domain;

/**
 * Tipo de um ajudante. Cada tipo pode ter no máximo {@link #MAX_PER_TYPE} ajudantes.
 */
public enum HelperType {

    /** Organiza os arquivos de uma pasta em subpastas por tipo. */
    FOLDER_ORGANIZER,

    /** Esvazia a lixeira ou remove os arquivos de uma pasta. */
    CLEANER;

    // Constantes

    /** Quantidade máxima de ajudantes de cada tipo. */
    public static final int MAX_PER_TYPE = 20;

    /**
     * Garante que ainda é possível criar um ajudante deste tipo.
     * A checagem vale só para a criação; editar um ajudante existente não passa por ela.
     *
     * @param existingCount quantos ajudantes deste tipo já existem
     * @throws HelperLimitReachedException se a quantidade já atingiu {@link #MAX_PER_TYPE}
     */
    public void assertCanCreate(int existingCount) {
        if (existingCount >= MAX_PER_TYPE) {
            throw new HelperLimitReachedException(this);
        }
    }
}
