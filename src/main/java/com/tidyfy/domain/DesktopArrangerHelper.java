package com.tidyfy.domain;

import java.util.Objects;

/**
 * Ajudante que reposiciona os ícones da área de trabalho do usuário em um layout escolhido.
 * Não move nem apaga arquivos, só muda a posição dos ícones. A área de trabalho é única e fixa,
 * por isso o ajudante não tem pasta.
 *
 * @param id       identificador do ajudante
 * @param name     nome definido pelo usuário
 * @param schedule frequência de execução
 * @param enabled  se o ajudante está ligado
 * @param layout   layout dos ícones
 */
public record DesktopArrangerHelper(HelperId id, HelperName name, Schedule schedule, boolean enabled,
                                    DesktopLayout layout) implements Helper {

    // Constantes

    /** Nome do parâmetro do identificador, usado na mensagem de valor nulo. */
    private static final String ID_NAME = "id";

    /** Nome do parâmetro do nome, usado na mensagem de valor nulo. */
    private static final String NAME_NAME = "name";

    /** Nome do parâmetro da frequência, usado na mensagem de valor nulo. */
    private static final String SCHEDULE_NAME = "schedule";

    /** Nome do parâmetro do layout, usado na mensagem de valor nulo. */
    private static final String LAYOUT_NAME = "layout";

    /**
     * Valida os dados do ajudante.
     *
     * @throws NullPointerException se {@code id}, {@code name}, {@code schedule} ou {@code layout} for nulo
     */
    public DesktopArrangerHelper {
        Objects.requireNonNull(id, ID_NAME);
        Objects.requireNonNull(name, NAME_NAME);
        Objects.requireNonNull(schedule, SCHEDULE_NAME);
        Objects.requireNonNull(layout, LAYOUT_NAME);
    }

    /**
     * Tipo deste ajudante.
     *
     * @return {@link HelperType#DESKTOP_ARRANGER}
     */
    @Override
    public HelperType type() {
        return HelperType.DESKTOP_ARRANGER;
    }

    /**
     * Cópia deste ajudante com o estado ligado/desligado trocado.
     *
     * @param enabled {@code true} para ligar, {@code false} para desligar
     * @return a cópia, com os demais campos iguais
     */
    @Override
    public DesktopArrangerHelper withEnabled(boolean enabled) {
        return new DesktopArrangerHelper(id, name, schedule, enabled, layout);
    }
}
