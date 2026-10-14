package com.tidyfy;

import com.tidyfy.infrastructure.logging.ExecutionLog;
import com.tidyfy.infrastructure.persistence.StorageLocation;

import java.time.Clock;

/**
 * Ponto de entrada do Tidyfy, com dois modos: sem argumentos abre a interface gráfica; com
 * {@code --run <id>} executa o ajudante sem interface e encerra com código de saída (0 em sucesso).
 * A decisão é tomada antes de qualquer código do JavaFX: esta classe não o referencia, e o JavaFX só é
 * carregado por {@link InterfaceLauncher}, que o modo {@code --run} nunca usa.
 */
public final class Main {

    /**
     * Construtor privado: classe de ponto de entrada.
     */
    private Main() {
    }

    /**
     * Interpreta os argumentos e abre a interface ou executa o ajudante. No modo interface não chama
     * {@link System#exit(int)}; no modo {@code --run} (ou com argumentos inválidos) encerra o processo
     * com o código de saída.
     *
     * @param args argumentos de linha de comando
     */
    public static void main(String[] args) {
        LaunchArguments arguments = LaunchArguments.parse(args);
        if (arguments.mode() == LaunchArguments.Mode.INTERFACE) {
            InterfaceLauncher.launch(args);
            return;
        }
        ExecutionLog log = new ExecutionLog(StorageLocation.defaultLocation().logsDirectory(),
                Clock.systemDefaultZone());
        System.exit(new HeadlessRunner(() -> AppContainer.create().runHelper(), log, System.err).run(arguments));
    }
}
