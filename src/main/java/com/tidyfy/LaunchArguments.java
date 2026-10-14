package com.tidyfy;

import com.tidyfy.domain.HelperId;

import java.util.Arrays;
import java.util.UUID;

/**
 * Interpretação dos argumentos de linha de comando do Tidyfy. Sem argumentos abre a interface;
 * {@code --run <id>} executa um ajudante sem interface; qualquer outra coisa é inválida e traz uma mensagem.
 * Não depende de JavaFX nem do Windows, então é testável sem abrir a interface.
 *
 * @param mode     modo de execução pedido
 * @param helperId ajudante a executar (só em {@link Mode#RUN}); nulo nos demais modos
 * @param error    mensagem do erro (só em {@link Mode#INVALID}); nula nos demais modos
 */
public record LaunchArguments(Mode mode, HelperId helperId, String error) {

    /** Modos de execução do Tidyfy. */
    public enum Mode {
        /** Abre a interface gráfica. */
        INTERFACE,
        /** Executa um ajudante e encerra, sem interface. */
        RUN,
        /** Argumentos inválidos. */
        INVALID
    }

    // Constantes

    /** Argumento que pede a execução de um ajudante. */
    public static final String RUN_ARGUMENT = "--run";

    /** Mensagem quando o identificador do ajudante não foi informado. */
    private static final String MISSING_ID = "Informe o identificador do ajudante: --run <id>";

    /** Modelo da mensagem quando o identificador não é um UUID. */
    private static final String INVALID_ID = "Identificador de ajudante inválido: %s";

    /** Modelo da mensagem quando há argumentos além do identificador. */
    private static final String EXTRA_ARGUMENTS = "Argumentos demais após o identificador: %s";

    /** Modelo da mensagem quando o argumento não é conhecido. */
    private static final String UNKNOWN_ARGUMENT = "Argumento desconhecido: %s (uso: --run <id>)";

    /** Separador dos argumentos nas mensagens. */
    private static final String SEPARATOR = " ";

    /**
     * Interpreta os argumentos de linha de comando.
     *
     * @param args argumentos recebidos pelo {@code main}
     * @return o modo pedido, ou {@link Mode#INVALID} com a mensagem do problema
     */
    public static LaunchArguments parse(String[] args) {
        if (args.length == 0) {
            return new LaunchArguments(Mode.INTERFACE, null, null);
        }
        if (!RUN_ARGUMENT.equals(args[0])) {
            return invalid(String.format(UNKNOWN_ARGUMENT, args[0]));
        }
        if (args.length == 1) {
            return invalid(MISSING_ID);
        }
        if (args.length > 2) {
            return invalid(String.format(EXTRA_ARGUMENTS,
                    String.join(SEPARATOR, Arrays.copyOfRange(args, 2, args.length))));
        }
        try {
            return new LaunchArguments(Mode.RUN, new HelperId(UUID.fromString(args[1])), null);
        } catch (IllegalArgumentException e) {
            return invalid(String.format(INVALID_ID, args[1]));
        }
    }

    /**
     * Cria o resultado de argumentos inválidos.
     */
    private static LaunchArguments invalid(String error) {
        return new LaunchArguments(Mode.INVALID, null, error);
    }
}
