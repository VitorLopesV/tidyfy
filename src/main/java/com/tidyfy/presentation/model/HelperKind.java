package com.tidyfy.presentation.model;

/**
 * Tipos de ajudante e como a interface os apresenta.
 */
public enum HelperKind {

    /** Organiza os arquivos de uma pasta em subpastas por tipo. */
    ORGANIZER(HelperKind.ORGANIZER_LABEL, HelperKind.ORGANIZER_SHORT_LABEL, HelperKind.ORGANIZER_STYLE_CLASS),

    /** Esvazia a lixeira ou remove os arquivos de uma pasta. */
    CLEANER(HelperKind.CLEANER_LABEL, HelperKind.CLEANER_SHORT_LABEL, HelperKind.CLEANER_STYLE_CLASS);

    /** Nome completo do tipo. */
    private final String label;

    /** Nome curto do tipo, usado em filtros e contadores. */
    private final String shortLabel;

    /** Classe CSS que dá ao tipo a sua cor. */
    private final String styleClass;

    // Constantes

    /** Espelha HelperType.MAX_PER_TYPE: a tela só informa, quem garante o limite é o domínio. */
    public static final int LIMIT_PER_TYPE = 20;

    /** Nome completo do tipo organização. */
    private static final String ORGANIZER_LABEL = "Organização de pasta";

    /** Nome curto do tipo organização. */
    private static final String ORGANIZER_SHORT_LABEL = "Organização";

    /** Classe CSS do tipo organização. */
    private static final String ORGANIZER_STYLE_CLASS = "kind-org";

    /** Nome completo do tipo limpeza. */
    private static final String CLEANER_LABEL = "Limpeza";

    /** Nome curto do tipo limpeza. */
    private static final String CLEANER_SHORT_LABEL = "Limpeza";

    /** Classe CSS do tipo limpeza. */
    private static final String CLEANER_STYLE_CLASS = "kind-clean";

    /**
     * Cria um tipo de ajudante.
     *
     * @param label      nome completo
     * @param shortLabel nome curto
     * @param styleClass classe CSS do tipo
     */
    HelperKind(String label, String shortLabel, String styleClass) {
        this.label = label;
        this.shortLabel = shortLabel;
        this.styleClass = styleClass;
    }

    /**
     * Devolve o nome completo do tipo.
     *
     * @return nome completo
     */
    public String label() {
        return label;
    }

    /**
     * Devolve o nome curto do tipo.
     *
     * @return nome curto
     */
    public String shortLabel() {
        return shortLabel;
    }

    /**
     * Devolve a classe CSS que dá ao tipo a sua cor.
     *
     * @return classe CSS
     */
    public String styleClass() {
        return styleClass;
    }
}
