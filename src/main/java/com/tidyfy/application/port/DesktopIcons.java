package com.tidyfy.application.port;

import com.tidyfy.domain.DesktopIcon;
import java.util.List;

/**
 * Porta da área de trabalho do usuário: lista os ícones e grava novas posições. Não move nem apaga arquivos.
 * Falhas (sem sessão gráfica, "Organizar ícones automaticamente" ligado, ícone que não existe mais) são
 * lançadas como {@link java.io.UncheckedIOException}.
 */
public interface DesktopIcons {

    /**
     * Lê os ícones, a área visível e a grade do monitor principal.
     *
     * @return o retrato atual da área de trabalho
     * @throws java.io.UncheckedIOException se a área de trabalho não puder ser lida
     */
    DesktopState read();

    /**
     * Grava as novas posições. Tudo ou nada: se algum ícone não puder ser movido, os já movidos voltam
     * para a posição original e a falha é lançada.
     *
     * @param placements ícones (pelo nome) com a posição desejada; lista vazia não faz nada
     * @throws java.io.UncheckedIOException se as posições não puderem ser gravadas
     */
    void move(List<DesktopIcon> placements);
}
