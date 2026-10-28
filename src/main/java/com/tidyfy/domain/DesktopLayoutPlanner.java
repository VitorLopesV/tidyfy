package com.tidyfy.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Calcula a posição de cada ícone da área de trabalho para um {@link DesktopLayout}. É uma função pura:
 * não acessa o Windows nem o disco, só recebe os ícones, a área visível com a grade e o layout.
 * A grade tem {@code largura / célula} colunas e {@code altura / célula} linhas. As posições ficam sempre
 * dentro da área visível e cada ícone ocupa uma célula própria. A grade parte da borda do layout: de
 * esquerda e de cima nos layouts à esquerda, no topo e por tipo; da direita no layout à direita; de baixo no
 * layout de base. Nas bordas, a última coluna encosta na direita e a última linha na base.
 * A ordem é estável e determinística: por nome sem diferenciar maiúsculas de minúsculas (empate: nome
 * exato e, por fim, a ordem recebida). Quando não couberem todos, os que sobram vão para
 * {@link DesktopLayoutPlan#leftOut()} e nada é sobreposto.
 * <p>
 * No layout por tipo, o nome do ícone decide o grupo, pela extensão: atalhos primeiro ({@code lnk}, {@code url}
 * ou nome sem extensão, pois o Explorer esconde o {@code .lnk}), depois as categorias de
 * {@link DefaultFileCategories} e, por fim, "outros". Cada grupo começa numa coluna nova e usa colunas
 * seguidas se tiver mais ícones do que linhas; grupos vazios não ocupam coluna. Pastas da área de trabalho
 * não são distinguíveis pelo nome, então caem no grupo dos atalhos (sem extensão) ou em "outros".
 */
public final class DesktopLayoutPlanner {

    /** Categorias que formam as colunas do layout por tipo. */
    private final List<FileCategory> categories;

    // Constantes

    /** Extensões dos atalhos do Windows. */
    private static final Set<String> SHORTCUT_EXTENSIONS = Set.of("lnk", "url");

    /** Posição do grupo dos atalhos no layout por tipo. */
    private static final int SHORTCUTS_GROUP = 0;

    /** Quantidade mínima de colunas e de linhas para o layout de bordas ter centro livre. */
    private static final int MIN_BORDER_SIZE = 2;

    /** Nome do parâmetro das categorias, usado na mensagem de valor nulo. */
    private static final String CATEGORIES_NAME = "categories";

    /** Nome do parâmetro dos ícones, usado na mensagem de valor nulo. */
    private static final String ICONS_NAME = "icons";

    /** Nome do parâmetro da grade, usado na mensagem de valor nulo. */
    private static final String GRID_NAME = "grid";

    /** Nome do parâmetro do layout, usado na mensagem de valor nulo. */
    private static final String LAYOUT_NAME = "layout";

    /** Ordem dos ícones: nome sem diferenciar maiúsculas de minúsculas e, no empate, o nome exato. */
    private static final Comparator<DesktopIcon> BY_NAME =
            Comparator.comparing(DesktopIcon::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(DesktopIcon::name);

    /**
     * Cria o planejador com as categorias padrão ({@link DefaultFileCategories}).
     */
    public DesktopLayoutPlanner() {
        this(DefaultFileCategories.all());
    }

    /**
     * Cria o planejador com as categorias informadas.
     *
     * @param categories as categorias do layout por tipo, na ordem das colunas
     * @throws NullPointerException se {@code categories} for nulo
     */
    public DesktopLayoutPlanner(List<FileCategory> categories) {
        this.categories = List.copyOf(Objects.requireNonNull(categories, CATEGORIES_NAME));
    }

    /**
     * Calcula as posições dos ícones.
     *
     * @param icons  os ícones da área de trabalho (a posição atual não é usada para posicionar)
     * @param grid   a área visível e a grade da área de trabalho
     * @param layout o layout desejado
     * @return as posições novas e os ícones que não couberam; plano vazio se não há ícones
     * @throws NullPointerException se algum argumento for nulo
     */
    public DesktopLayoutPlan plan(List<DesktopIcon> icons, DesktopGrid grid, DesktopLayout layout) {
        Objects.requireNonNull(icons, ICONS_NAME);
        Objects.requireNonNull(grid, GRID_NAME);
        Objects.requireNonNull(layout, LAYOUT_NAME);
        int columns = grid.width() / grid.cellWidth();
        int rows = grid.height() / grid.cellHeight();
        List<DesktopIcon> ordered = new ArrayList<>(icons);
        ordered.sort(BY_NAME);
        if (layout == DesktopLayout.BY_TYPE_LEFT) {
            return planByType(ordered, grid, columns, rows);
        }
        List<IconPosition> slots = slots(layout, grid, columns, rows);
        List<DesktopIcon> placements = new ArrayList<>();
        List<DesktopIcon> leftOut = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            DesktopIcon icon = ordered.get(i);
            if (i < slots.size()) {
                placements.add(new DesktopIcon(icon.name(), slots.get(i)));
            } else {
                leftOut.add(icon);
            }
        }
        return new DesktopLayoutPlan(placements, leftOut);
    }

    /**
     * Gera as posições disponíveis do layout, na ordem em que os ícones as ocupam.
     *
     * @param layout  o layout (diferente de {@link DesktopLayout#BY_TYPE_LEFT})
     * @param grid    a área visível e a grade
     * @param columns quantidade de colunas
     * @param rows    quantidade de linhas
     * @return as posições, possivelmente vazia se a área não comporta nem uma célula
     */
    private static List<IconPosition> slots(DesktopLayout layout, DesktopGrid grid, int columns, int rows) {
        List<IconPosition> slots = new ArrayList<>();
        switch (layout) {
            case LEFT -> {
                for (int column = 0; column < columns; column++) {
                    for (int row = 0; row < rows; row++) {
                        slots.add(new IconPosition(fromLeft(grid, column), fromTop(grid, row)));
                    }
                }
            }
            case RIGHT -> {
                for (int column = 0; column < columns; column++) {
                    for (int row = 0; row < rows; row++) {
                        slots.add(new IconPosition(fromRight(grid, column), fromTop(grid, row)));
                    }
                }
            }
            case TOP -> {
                for (int row = 0; row < rows; row++) {
                    for (int column = 0; column < columns; column++) {
                        slots.add(new IconPosition(fromLeft(grid, column), fromTop(grid, row)));
                    }
                }
            }
            case BOTTOM -> {
                for (int row = 0; row < rows; row++) {
                    for (int column = 0; column < columns; column++) {
                        slots.add(new IconPosition(fromLeft(grid, column), fromBottom(grid, row)));
                    }
                }
            }
            case BORDERS -> slots.addAll(borderSlots(grid, columns, rows));
            case BY_TYPE_LEFT -> throw new IllegalArgumentException(layout.name());
        }
        return slots;
    }

    /**
     * Gera as células do perímetro, no sentido horário a partir do canto superior esquerdo: o topo, a
     * direita, a base e a esquerda. Com menos de duas colunas ou duas linhas não há centro livre e as células
     * seguem a ordem do layout de topo.
     *
     * @param grid    a área visível e a grade
     * @param columns quantidade de colunas
     * @param rows    quantidade de linhas
     * @return as posições do perímetro, sem repetir cantos
     */
    private static List<IconPosition> borderSlots(DesktopGrid grid, int columns, int rows) {
        List<IconPosition> slots = new ArrayList<>();
        if (columns < MIN_BORDER_SIZE || rows < MIN_BORDER_SIZE) {
            return slots(DesktopLayout.TOP, grid, columns, rows);
        }
        for (int column = 0; column < columns; column++) {
            slots.add(borderCell(grid, columns, rows, column, 0));
        }
        for (int row = 1; row < rows; row++) {
            slots.add(borderCell(grid, columns, rows, columns - 1, row));
        }
        for (int column = columns - 2; column >= 0; column--) {
            slots.add(borderCell(grid, columns, rows, column, rows - 1));
        }
        for (int row = rows - 2; row >= 1; row--) {
            slots.add(borderCell(grid, columns, rows, 0, row));
        }
        return slots;
    }

    /**
     * Posição de uma célula do perímetro: a última coluna encosta na borda direita e a última linha na base.
     *
     * @param grid    a área visível e a grade
     * @param columns quantidade de colunas
     * @param rows    quantidade de linhas
     * @param column  coluna da célula, a partir da esquerda
     * @param row     linha da célula, a partir do topo
     * @return a posição da célula
     */
    private static IconPosition borderCell(DesktopGrid grid, int columns, int rows, int column, int row) {
        int x = column == columns - 1 ? fromRight(grid, 0) : fromLeft(grid, column);
        int y = row == rows - 1 ? fromBottom(grid, 0) : fromTop(grid, row);
        return new IconPosition(x, y);
    }

    /**
     * Posiciona os ícones por tipo: cada grupo começa numa coluna nova, a partir da esquerda.
     *
     * @param ordered os ícones já em ordem de nome
     * @param grid    a área visível e a grade
     * @param columns quantidade de colunas
     * @param rows    quantidade de linhas
     * @return as posições e os ícones que não couberam
     */
    private DesktopLayoutPlan planByType(List<DesktopIcon> ordered, DesktopGrid grid, int columns, int rows) {
        List<List<DesktopIcon>> groups = new ArrayList<>();
        for (int i = 0; i <= categories.size() + 1; i++) {
            groups.add(new ArrayList<>());
        }
        for (DesktopIcon icon : ordered) {
            groups.get(groupOf(icon)).add(icon);
        }
        List<DesktopIcon> placements = new ArrayList<>();
        List<DesktopIcon> leftOut = new ArrayList<>();
        int firstColumn = 0;
        for (List<DesktopIcon> group : groups) {
            for (int i = 0; i < group.size(); i++) {
                DesktopIcon icon = group.get(i);
                int column = rows == 0 ? columns : firstColumn + i / rows;
                if (column < columns) {
                    placements.add(new DesktopIcon(icon.name(),
                            new IconPosition(fromLeft(grid, column), fromTop(grid, i % rows))));
                } else {
                    leftOut.add(icon);
                }
            }
            if (!group.isEmpty() && rows > 0) {
                firstColumn += (group.size() + rows - 1) / rows;
            }
        }
        return new DesktopLayoutPlan(placements, leftOut);
    }

    /**
     * Escolhe o grupo do ícone pela extensão do nome: atalhos, uma das categorias ou "outros".
     *
     * @param icon o ícone
     * @return o índice do grupo: 0 para atalhos, 1 a N para as categorias e N + 1 para "outros"
     */
    private int groupOf(DesktopIcon icon) {
        Optional<FileExtension> extension = FileExtension.of(icon.name());
        if (extension.isEmpty() || SHORTCUT_EXTENSIONS.contains(extension.get().value())) {
            return SHORTCUTS_GROUP;
        }
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).extensions().contains(extension.get())) {
                return SHORTCUTS_GROUP + 1 + i;
            }
        }
        return categories.size() + 1;
    }

    /**
     * Posição horizontal da coluna contada a partir da borda esquerda.
     *
     * @param grid   a área visível e a grade
     * @param column coluna, a partir de zero
     * @return a coordenada x
     */
    private static int fromLeft(DesktopGrid grid, int column) {
        return grid.left() + column * grid.cellWidth();
    }

    /**
     * Posição horizontal da coluna contada a partir da borda direita.
     *
     * @param grid   a área visível e a grade
     * @param column coluna, a partir de zero (a mais à direita)
     * @return a coordenada x
     */
    private static int fromRight(DesktopGrid grid, int column) {
        return grid.left() + grid.width() - (column + 1) * grid.cellWidth();
    }

    /**
     * Posição vertical da linha contada a partir da borda superior.
     *
     * @param grid a área visível e a grade
     * @param row  linha, a partir de zero
     * @return a coordenada y
     */
    private static int fromTop(DesktopGrid grid, int row) {
        return grid.top() + row * grid.cellHeight();
    }

    /**
     * Posição vertical da linha contada a partir da base.
     *
     * @param grid a área visível e a grade
     * @param row  linha, a partir de zero (a mais baixa)
     * @return a coordenada y
     */
    private static int fromBottom(DesktopGrid grid, int row) {
        return grid.top() + grid.height() - (row + 1) * grid.cellHeight();
    }
}
