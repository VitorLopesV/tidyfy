package com.tidyfy.infrastructure.windows;

import com.tidyfy.application.port.RecycleBin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Lixeira do Windows manipulada pelo {@code powershell.exe}.
 * Cada operação roda um script por {@code powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass
 * -Command "script"}, entregue como item de lista (sem passar por um shell, então não há escape a fazer).
 * Para esvaziar tudo, primeiro conta os itens (o script imprime só o número, com a saída em UTF-8) e, se
 * houver algum, chama {@code Clear-RecycleBin -Force}. Lixeira vazia não é erro: nesse caso o esvaziamento
 * nem é chamado, pois o {@code Clear-RecycleBin} falha quando não há nada a remover.
 * Para remover só o que é mais antigo, um único script lê, pelo Shell COM, o instante em que cada item foi
 * para a lixeira ({@code System.Recycle.DateDeleted}) e remove de forma permanente os anteriores ao
 * corte (estritamente: o item enviado exatamente no corte é mantido).
 * O corte vai ao script como texto ISO-8601 UTC com 7 casas de fração (precisão do .NET, 100 ns),
 * arredondado para cima ao múltiplo de 100 ns, o que mantém a comparação exata, pois as datas dos itens
 * também são múltiplos de 100 ns. O texto vem de um {@link Instant} formatado, nunca de entrada livre, então
 * é seguro embuti-lo entre aspas simples.
 * Verificado no Windows: a propriedade devolve {@code DateTime} com {@code Kind=Unspecified}, que é a HORA
 * LOCAL do usuário (sem marca de fuso), com precisão de 1 segundo. O script trata {@code Unspecified} como
 * local, {@code Local} converte e {@code Utc} fica como está, sempre comparando em UTC.
 * Limitações (o COM não é testável sem Windows): itens sem data são mantidos; na virada do horário de verão
 * a hora local ambígua pode deslocar a idade em até 1 hora.
 * Roteiro manual de verificação: (1) envie 3 arquivos à lixeira em momentos diferentes; (2) chame
 * {@code emptyOlderThan} com um corte entre eles; (3) confirme que sumiram só os anteriores ao corte e que o
 * número devolvido bate.
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

    /** Nome da operação de remover os itens antigos, para a mensagem de falha. */
    private static final String REMOVE_OLDER_OPERATION = "remove old items from";

    /** Mensagem quando o instante de corte é nulo. */
    private static final String NULL_CUTOFF_MESSAGE = "cutoff must not be null";

    /** Mensagem quando a saída da remoção não é um número: saída recebida. */
    private static final String UNEXPECTED_REMOVAL_OUTPUT_MESSAGE =
            "Unexpected PowerShell output when removing old recycle bin items: %s";

    /** Formato do corte entregue ao script: ISO-8601 em UTC com 7 casas de fração (o formato "o" do .NET). */
    private static final DateTimeFormatter CUTOFF_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSS'Z'").withZone(ZoneOffset.UTC);

    /** Nanossegundos em um tick do .NET (100 ns). */
    private static final long NANOS_PER_TICK = 100;

    /** Marcador do corte dentro do script. */
    private static final String CUTOFF_PLACEHOLDER = "@@CUTOFF@@";

    /**
     * Script que remove, de forma permanente, os itens enviados à lixeira antes do corte e imprime só a
     * quantidade removida. Lê a data do envio em {@code System.Recycle.DateDeleted} (hora local se {@code Unspecified}), mantém itens sem
     * data e o item enviado exatamente no corte (comparação estrita {@code -lt}), remove cada item com
     * {@code Remove-Item -LiteralPath -Recurse -Force}, apaga o arquivo de metadados {@code $I} irmão do
     * {@code $R} e conta apenas o que de fato foi removido; erro em um item não interrompe os demais.
     */
    private static final String REMOVE_OLDER_SCRIPT =
            "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; "
                    + "$cutoff = [DateTime]::ParseExact('" + CUTOFF_PLACEHOLDER + "', 'o', "
                    + "[System.Globalization.CultureInfo]::InvariantCulture, "
                    + "[System.Globalization.DateTimeStyles]::AdjustToUniversal); "
                    + "$removed = 0; "
                    + "foreach ($item in @((New-Object -ComObject Shell.Application).Namespace(0xA).Items())) { "
                    + "try { "
                    + "$deleted = $item.ExtendedProperty('System.Recycle.DateDeleted'); "
                    + "if ($null -eq $deleted) { continue }; "
                    + "if ($deleted.Kind -eq [System.DateTimeKind]::Unspecified) { "
                    + "$deleted = [DateTime]::SpecifyKind($deleted, [System.DateTimeKind]::Local) }; "
                    + "if ($deleted.Kind -eq [System.DateTimeKind]::Local) { $deleted = $deleted.ToUniversalTime() }; "
                    + "if ($deleted -lt $cutoff) { "
                    + "$path = $item.Path; "
                    + "Remove-Item -LiteralPath $path -Recurse -Force -ErrorAction Stop; "
                    + "$leaf = Split-Path -Leaf $path; "
                    + "if ($leaf.StartsWith('$R')) { "
                    + "Remove-Item -LiteralPath (Join-Path (Split-Path -Parent $path) ('$I' + $leaf.Substring(2))) "
                    + "-Force -ErrorAction SilentlyContinue }; "
                    + "$removed++ } "
                    + "} catch { } "
                    + "}; "
                    + "Write-Output $removed";

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
        return parseCount(output, UNEXPECTED_OUTPUT_MESSAGE);
    }

    /**
     * Lê o número da última linha não vazia da saída.
     *
     * @param output        saída do script
     * @param failureFormat mensagem de falha, com a saída como argumento
     * @return quantidade
     */
    private static int parseCount(String output, String failureFormat) {
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
            throw new UncheckedIOException(new IOException(failureFormat.formatted(output), e));
        }
    }

    /**
     * Remove de forma permanente os itens que foram para a lixeira antes do corte (corte exclusivo).
     *
     * @param cutoff instante de corte; item enviado exatamente nele é mantido
     * @return quantidade de itens removidos
     * @throws UncheckedIOException se o PowerShell falhar ou devolver uma saída inesperada
     */
    @Override
    public int emptyOlderThan(Instant cutoff) {
        Objects.requireNonNull(cutoff, NULL_CUTOFF_MESSAGE);
        String output = execute(olderThanCommand(cutoff), REMOVE_OLDER_OPERATION);
        return parseCount(output, UNEXPECTED_REMOVAL_OUTPUT_MESSAGE);
    }

    /**
     * Monta o comando que remove os itens anteriores ao corte.
     *
     * @param cutoff instante de corte
     * @return programa e argumentos
     */
    List<String> olderThanCommand(Instant cutoff) {
        return powerShell(REMOVE_OLDER_SCRIPT.replace(CUTOFF_PLACEHOLDER, formatCutoff(cutoff)));
    }

    /**
     * Formata o corte em ISO-8601 UTC com 7 casas de fração, arredondando para cima a 100 ns.
     *
     * @param cutoff instante de corte
     * @return texto aceito pelo formato "o" do .NET
     */
    private static String formatCutoff(Instant cutoff) {
        long nanosOverTick = cutoff.getNano() % NANOS_PER_TICK;
        Instant rounded = nanosOverTick == 0 ? cutoff : cutoff.plusNanos(NANOS_PER_TICK - nanosOverTick);
        return CUTOFF_FORMAT.format(rounded);
    }
}
