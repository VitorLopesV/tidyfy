package com.tidyfy;

import com.tidyfy.infrastructure.logging.ExecutionLog;
import com.tidyfy.infrastructure.persistence.StorageLocation;

import java.time.Clock;

/**
 * Ponto de entrada do Tidyfy, com três modos: sem argumentos abre a interface gráfica; com
 * {@code --run <id>} executa o ajudante sem interface e encerra com código de saída (0 em sucesso); com
 * {@code --remove-tasks} remove todas as tarefas do agendador (desinstalação) e encerra.
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
     * Dispara a sincronização do agendador em segundo plano. Qualquer falha aqui é ignorada: a interface
     * sempre abre.
     */
    private static void startSchedulerSync() {
        try {
            new SchedulerSyncStarter(() -> AppContainer.create().syncScheduler(), createLog()).start();
        } catch (RuntimeException ignored) {
            // Sem log ou sem thread: a sincronização é descartada e a interface abre normalmente.
        }
    }

    /**
     * Cria o log das execuções na pasta padrão do Tidyfy.
     */
    private static ExecutionLog createLog() {
        return new ExecutionLog(StorageLocation.defaultLocation().logsDirectory(), Clock.systemDefaultZone());
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
            startSchedulerSync();
            InterfaceLauncher.launch(AppContainer.create(), Clock.systemDefaultZone(), args);
            return;
        }
        ExecutionLog log = createLog();
        if (arguments.mode() == LaunchArguments.Mode.REMOVE_TASKS) {
            System.exit(new RemoveTasksRunner(() -> AppContainer.create().removeAllTasks(), log, System.err).run());
        }
        System.exit(new HeadlessRunner(() -> AppContainer.create().runHelper(), log, System.err).run(arguments));
    }
}
