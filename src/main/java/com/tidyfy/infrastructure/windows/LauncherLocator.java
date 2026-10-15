package com.tidyfy.infrastructure.windows;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Descobre o comando que o Agendador do Windows deve chamar.
 * Empacotado (o processo atual é o {@code Tidyfy.exe}), a tarefa chama esse executável. Em desenvolvimento
 * ({@code mvn javafx:run}, IDE), o processo atual é o Java, então a tarefa chama o {@code javaw.exe} do
 * {@code java.home} com {@code -cp <classpath atual> com.tidyfy.Main}. Esse modo é só para desenvolvimento:
 * depende do classpath do momento e deixa de funcionar se o projeto for recompilado ou movido.
 * O ambiente (comando do processo, classpath e {@code java.home}) é informado por construtor para ser testável.
 */
public final class LauncherLocator {

    /** Caminho do executável do processo atual, se o sistema informou. */
    private final Optional<String> processCommand;

    /** Valor de {@code java.class.path}, ou nulo se não informado. */
    private final String classPath;

    /** Valor de {@code java.home}. */
    private final String javaHome;

    // Constantes

    /** Nome do executável empacotado do Tidyfy (comparado sem diferenciar maiúsculas). */
    private static final String PACKAGED_EXECUTABLE = "Tidyfy.exe";

    /** Pasta do executável do Java. */
    private static final String JAVA_BIN_FOLDER = "bin";

    /** Executável do Java sem janela de console, usado em desenvolvimento. */
    private static final String JAVA_EXECUTABLE = "javaw.exe";

    /** Opção do classpath. */
    private static final String CLASSPATH_OPTION = "-cp";

    /** Classe principal do Tidyfy. */
    private static final String MAIN_CLASS = "com.tidyfy.Main";

    /** Propriedade do sistema com o classpath. */
    private static final String CLASSPATH_PROPERTY = "java.class.path";

    /** Propriedade do sistema com a pasta de instalação do Java. */
    private static final String JAVA_HOME_PROPERTY = "java.home";

    /**
     * Cria o localizador com o ambiente informado.
     *
     * @param processCommand caminho do executável do processo atual, se conhecido
     * @param classPath      valor de {@code java.class.path} (nulo ou vazio omite o {@code -cp})
     * @param javaHome       valor de {@code java.home}
     */
    public LauncherLocator(Optional<String> processCommand, String classPath, String javaHome) {
        this.processCommand = processCommand;
        this.classPath = classPath;
        this.javaHome = javaHome;
    }

    /**
     * Cria o localizador com o ambiente do processo atual.
     *
     * @return o localizador do sistema
     */
    public static LauncherLocator fromSystem() {
        return new LauncherLocator(ProcessHandle.current().info().command(),
                System.getProperty(CLASSPATH_PROPERTY), System.getProperty(JAVA_HOME_PROPERTY));
    }

    /**
     * Resolve o comando da tarefa agendada.
     *
     * @return o executável empacotado, ou o {@code javaw.exe} com classpath e classe principal (desenvolvimento)
     */
    public TaskCommand locate() {
        Optional<Path> packaged = processCommand.map(Path::of)
                .filter(path -> path.getFileName() != null
                        && path.getFileName().toString().equalsIgnoreCase(PACKAGED_EXECUTABLE));
        if (packaged.isPresent()) {
            return TaskCommand.of(packaged.get());
        }
        Path javaw = Path.of(javaHome, JAVA_BIN_FOLDER, JAVA_EXECUTABLE);
        if (classPath == null || classPath.isBlank()) {
            return new TaskCommand(javaw, List.of(MAIN_CLASS));
        }
        return new TaskCommand(javaw, List.of(CLASSPATH_OPTION, classPath, MAIN_CLASS));
    }
}
