package com.tidyfy.infrastructure.windows;

import com.tidyfy.domain.HelperId;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Comando que a tarefa agendada executa: o programa e os argumentos que vêm antes do {@code --run {id}}.
 * Empacotado, é só o {@code Tidyfy.exe}; em desenvolvimento, é o {@code javaw.exe} com o classpath e a classe
 * principal.
 *
 * @param executable    programa chamado pela tarefa
 * @param baseArguments argumentos que antecedem o {@code --run {id}} (pode ser vazio)
 */
public record TaskCommand(Path executable, List<String> baseArguments) {

    // Constantes

    /** Opção do Tidyfy que executa um ajudante. */
    private static final String RUN_OPTION = "--run";

    /** Separador dos argumentos na linha de comando. */
    private static final String ARGUMENT_SEPARATOR = " ";

    /** Aspas duplas. */
    private static final String QUOTE = "\"";

    /** Aspas duplas escapadas dentro de um argumento. */
    private static final String ESCAPED_QUOTE = "\\\"";

    /** Espaço. */
    private static final char SPACE = ' ';

    /** Tabulação. */
    private static final char TAB = '\t';

    /** Mensagem quando o executável é nulo. */
    private static final String NULL_EXECUTABLE_MESSAGE = "executable must not be null";

    /** Mensagem quando os argumentos base são nulos. */
    private static final String NULL_ARGUMENTS_MESSAGE = "baseArguments must not be null";

    /**
     * Valida os componentes e copia a lista de argumentos para que não possa ser alterada.
     */
    public TaskCommand {
        Objects.requireNonNull(executable, NULL_EXECUTABLE_MESSAGE);
        Objects.requireNonNull(baseArguments, NULL_ARGUMENTS_MESSAGE);
        baseArguments = List.copyOf(baseArguments);
    }

    /**
     * Cria um comando sem argumentos base (o executável empacotado).
     *
     * @param executable programa chamado pela tarefa
     * @return o comando
     */
    public static TaskCommand of(Path executable) {
        return new TaskCommand(executable, List.of());
    }

    /**
     * Monta a linha de argumentos da tarefa para o ajudante: os argumentos base e {@code --run {id}}.
     * O Agendador de Tarefas recebe uma única linha, então argumentos com espaço ou aspas são protegidos
     * com aspas (regra do {@code CommandLineToArgvW}).
     *
     * @param id identificador do ajudante
     * @return a linha de argumentos
     */
    public String argumentsFor(HelperId id) {
        Objects.requireNonNull(id);
        List<String> all = new ArrayList<>(baseArguments);
        all.add(RUN_OPTION);
        all.add(id.value().toString());
        return String.join(ARGUMENT_SEPARATOR, all.stream().map(TaskCommand::quote).toList());
    }

    /**
     * Envolve o argumento em aspas se estiver vazio ou tiver espaço, tabulação ou aspas.
     */
    private static String quote(String argument) {
        boolean needsQuotes = argument.isEmpty() || argument.indexOf(SPACE) >= 0 || argument.indexOf(TAB) >= 0
                || argument.contains(QUOTE);
        if (!needsQuotes) {
            return argument;
        }
        return QUOTE + argument.replace(QUOTE, ESCAPED_QUOTE) + QUOTE;
    }
}
