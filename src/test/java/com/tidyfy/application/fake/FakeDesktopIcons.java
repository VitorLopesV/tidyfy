package com.tidyfy.application.fake;

import com.tidyfy.application.port.DesktopIcons;
import com.tidyfy.application.port.DesktopState;
import com.tidyfy.domain.DesktopGrid;
import com.tidyfy.domain.DesktopIcon;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Área de trabalho em memória, para os testes dos casos de uso. Guarda os ícones e a última gravação.
 */
public final class FakeDesktopIcons implements DesktopIcons {

    /** Grade padrão: 1000 x 600, células de 100 x 100. */
    public static final DesktopGrid DEFAULT_GRID = new DesktopGrid(0, 0, 1000, 600, 100, 100);

    /** Ícones atuais da área de trabalho. */
    private final List<DesktopIcon> icons = new ArrayList<>();

    /** Posições recebidas em {@link #move(List)}, na última chamada. */
    private List<DesktopIcon> moved = List.of();

    /** Quantidade de chamadas a {@link #move(List)}. */
    private int moveCalls;

    /** Grade devolvida na leitura. */
    private DesktopGrid grid = DEFAULT_GRID;

    /** Falha a lançar na próxima chamada; {@code null} quando não há. */
    private UncheckedIOException failure;

    /**
     * Adiciona um ícone à área de trabalho.
     *
     * @param icon ícone
     * @return este fake, para encadear
     */
    public FakeDesktopIcons with(DesktopIcon icon) {
        icons.add(icon);
        return this;
    }

    /**
     * Troca a grade devolvida na leitura.
     *
     * @param grid nova grade
     * @return este fake, para encadear
     */
    public FakeDesktopIcons withGrid(DesktopGrid grid) {
        this.grid = grid;
        return this;
    }

    /**
     * Faz a próxima chamada (leitura ou gravação) lançar a falha informada, uma vez só.
     *
     * @param failure falha a lançar
     */
    public void failNextWith(UncheckedIOException failure) {
        this.failure = failure;
    }

    /**
     * Posições gravadas na última chamada a {@link #move(List)}.
     *
     * @return posições, ou lista vazia se nada foi gravado
     */
    public List<DesktopIcon> moved() {
        return moved;
    }

    /**
     * Quantidade de chamadas a {@link #move(List)}.
     *
     * @return chamadas feitas
     */
    public int moveCalls() {
        return moveCalls;
    }

    @Override
    public DesktopState read() {
        throwIfFailing();
        return new DesktopState(icons, grid, false, false);
    }

    @Override
    public void move(List<DesktopIcon> placements) {
        throwIfFailing();
        moveCalls++;
        moved = List.copyOf(placements);
        for (DesktopIcon placement : placements) {
            icons.replaceAll(icon -> icon.name().equals(placement.name()) ? placement : icon);
        }
    }

    /** Lança a falha configurada, se houver, e a esquece. */
    private void throwIfFailing() {
        if (failure != null) {
            UncheckedIOException toThrow = failure;
            failure = null;
            throw toThrow;
        }
    }
}
