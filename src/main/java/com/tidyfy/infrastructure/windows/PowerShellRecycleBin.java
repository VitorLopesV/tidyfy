package com.tidyfy.infrastructure.windows;

import com.tidyfy.application.port.RecycleBin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Lixeira do Windows manipulada pelo {@code powershell.exe}.
 * Cada operação roda um script por {@code powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass
 * -Command "script"}, entregue como item de lista (sem passar por um shell, então não há escape a fazer).
 * Para esvaziar tudo, primeiro conta os itens (o script imprime só o número, com a saída em UTF-8) e, se
 * houver algum, chama {@code Clear-RecycleBin -Force}. Lixeira vazia não é erro: nesse caso o esvaziamento
 * nem é chamado, pois o {@code Clear-RecycleBin} falha quando não há nada a remover.
 * Saída que não termina em número ou código de saída diferente de zero viram {@link UncheckedIOException}.
 */
public final class PowerShellRecycleBin implements RecycleBin {

    /** Executa o PowerShell. */
    private final CommandRunner commandRunner;

    // Constantes

    /** Programa do PowerShell. */
    private static final String POWERSHELL = "powershell.exe";

    /** Opção que não carrega o perfil do usuário. */
    private static final String NO_PROFILE_OPTION = "-NoProfile";

    /** Opção que impede perguntas ao usuário. */
    private static final String NON_INTERACTIVE_OPTION = "-NonInteractive";

    /** Opção da política de execução. */
    private static final String EXECUTION_POLICY_OPTION = "-ExecutionPolicy";

    /** Valor da política de execução que não bloqueia o script. */
    private static final String BYPASS = "Bypass";

    /** Opção que recebe o script. */
    private static final String COMMAND_OPTION = "-Command";

    /** Script que imprime a quantidade de itens da lixeira (pasta shell 0xA), com a saída em UTF-8. */
    private static final String COUNT_SCRIPT =
            "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; "
                    + "Write-Output ((New-Object -ComObject Shell.Application).Namespace(0xA).Items().Count)";

    /** Script que esvazia a lixeira sem pedir confirmação. */
    private static final String CLEAR_SCRIPT = "Clear-RecycleBin -Force -ErrorAction SilentlyContinue";

    /** Separador de linhas aceito na saída. */
    private static final String LINE_SEPARATOR_REGEX = "\\R";

    /** Mensagem de falha do comando: operação, código de saída e saída. */
    private static final String COMMAND_FAILURE_MESSAGE =
            "PowerShell failed to %s the recycle bin (exit code %d): %s";

    /** Mensagem quando a saída da contagem não é um número: saída recebida. */
    private static final String UNEXPECTED_OUTPUT_MESSAGE =
            "Unexpected PowerShell output when counting recycle bin items: %s";

    /** Nome da operação de contar, para a mensagem de falha. */
    private static final String COUNT_OPERATION = "count";

    /** Nome da operação de esvaziar, para a mensagem de falha. */
    private static final String CLEAR_OPERATION = "empty";

    /** Mensagem quando a operação ainda não existe. */
    private static final String EMPTY_OLDER_NOT_IMPLEMENTED = "emptyOlderThan ainda não foi implementado";

    /** Mensagem quando o executor é nulo. */
    private static final String NULL_RUNNER_MESSAGE = "commandRunner must not be null";

    /**
     * Cria a lixeira.
     *
     * @param commandRunner executor dos comandos do PowerShell
     */
    public PowerShellRecycleBin(CommandRunner commandRunner) {
        this.commandRunner = Objects.requireNonNull(commandRunner, NULL_RUNNER_MESSAGE);
    }

    /**
     * Conta os itens da lixeira e, se houver algum, esvazia.
     *
     * @return quantidade de itens que havia (e foram removidos); zero se já estava vazia
     * @throws UncheckedIOException se o PowerShell falhar ou devolver uma saída inesperada
     */
    @Override
    public int emptyAll() {
        int count = parseCount(execute(countCommand(), COUNT_OPERATION));
        if (count > 0) {
            execute(clearCommand(), CLEAR_OPERATION);
        }
        return count;
    }

    /**
     * Monta o comando que conta os itens da lixeira.
     *
     * @return programa e argumentos
     */
    List<String> countCommand() {
        return powerShell(COUNT_SCRIPT);
    }

    /**
     * Monta o comando que esvazia a lixeira.
     *
     * @return programa e argumentos
     */
    List<String> clearCommand() {
        return powerShell(CLEAR_SCRIPT);
    }

    /**
     * Monta a chamada do PowerShell para um script.
     *
     * @param script script a executar
     * @return programa e argumentos
     */
    private static List<String> powerShell(String script) {
        return List.of(POWERSHELL, NO_PROFILE_OPTION, NON_INTERACTIVE_OPTION,
                EXECUTION_POLICY_OPTION, BYPASS, COMMAND_OPTION, script);
    }

    /**
     * Executa o comando e exige sucesso.
     *
     * @param command   comando a executar
     * @param operation nome da operação, para a mensagem de falha
     * @return saída do comando
     */
    private String execute(List<String> command, String operation) {
        CommandResult result = commandRunner.run(command);
        if (!result.succeeded()) {
            throw new UncheckedIOException(new IOException(
                    COMMAND_FAILURE_MESSAGE.formatted(operation, result.exitCode(), result.output())));
        }
        return result.output();
    }

    /**
     * Lê o número da última linha não vazia da saída.
     *
     * @param output saída do script de contagem
     * @return quantidade de itens
     */
    private static int parseCount(String output) {
        String last = "";
        for (String line : output.split(LINE_SEPARATOR_REGEX)) {
            if (!line.isBlank()) {
                last = line.strip();
            }
        }
        try {
            int count = Integer.parseInt(last);
            if (count < 0) {
                throw new NumberFormatException(last);
            }
            return count;
        } catch (NumberFormatException e) {
            throw new UncheckedIOException(new IOException(UNEXPECTED_OUTPUT_MESSAGE.formatted(output), e));
        }
    }

    // Região da emptyOlderThan (issue I14 substitui o corpo)

    /**
     * Ainda não implementado.
     *
     * @param cutoff instante de corte
     * @return nunca retorna
     * @throws UnsupportedOperationException sempre
     */
    @Override
    public int emptyOlderThan(Instant cutoff) {
        throw new UnsupportedOperationException(EMPTY_OLDER_NOT_IMPLEMENTED);
    }
}
