package com.tidyfy.infrastructure.windows;

import java.util.List;

/**
 * Porta interna da infraestrutura para executar processos externos (schtasks, PowerShell...).
 * Existe para que as classes que montam comandos possam ser testadas com dublês,
 * sem executar processos reais.
 */
public interface CommandRunner {

    /**
     * Executa o comando e espera o término.
     *
     * @param command programa e argumentos, um por item da lista
     * @return código de saída e saída de texto (padrão e erro juntos)
     * @throws java.io.UncheckedIOException se o processo não puder ser iniciado, for interrompido
     *                                      ou estourar o tempo limite
     */
    CommandResult run(List<String> command);
}
