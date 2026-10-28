package com.tidyfy.application.port;

import com.tidyfy.domain.DesktopGrid;
import com.tidyfy.domain.DesktopIcon;
import java.util.List;
import java.util.Objects;

/**
 * Retrato da área de trabalho: os ícones com a posição atual, a área visível com a grade e as opções do
 * Windows que interferem no posicionamento.
 *
 * @param icons       ícones da área de trabalho, com a posição atual
 * @param grid        área visível do monitor principal e tamanho da célula
 * @param autoArrange {@code true} se "Organizar ícones automaticamente" está ligado: o Windows reposiciona
 *                    sozinho e ignora as posições gravadas
 * @param snapToGrid  {@code true} se "Alinhar ícones à grade" está ligado: o Windows ajusta cada posição à
 *                    célula mais próxima
 */
public record DesktopState(List<DesktopIcon> icons, DesktopGrid grid, boolean autoArrange, boolean snapToGrid) {

    // Constantes

    /** Nome do parâmetro dos ícones, usado na mensagem de valor nulo. */
    private static final String ICONS_NAME = "icons";

    /** Nome do parâmetro da grade, usado na mensagem de valor nulo. */
    private static final String GRID_NAME = "grid";

    /**
     * Valida os dados e guarda uma cópia imutável da lista de ícones.
     *
     * @throws NullPointerException se {@code icons} ou {@code grid} for nulo
     */
    public DesktopState {
        Objects.requireNonNull(icons, ICONS_NAME);
        Objects.requireNonNull(grid, GRID_NAME);
        icons = List.copyOf(icons);
    }
}
