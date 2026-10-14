package com.tidyfy.infrastructure.windows;

import java.io.IOException;
import java.util.List;

/**
 * Inicia o processo de um comando. Existe para que os testes substituam o início real por um
 * processo falso.
 */
@FunctionalInterface
interface ProcessStarter {

    /**
     * Inicia o processo, com a saída de erro unida à saída padrão.
     *
     * @param command programa e argumentos
     * @return o processo iniciado
     * @throws IOException se o processo não puder ser iniciado
     */
    Process start(List<String> command) throws IOException;
}
