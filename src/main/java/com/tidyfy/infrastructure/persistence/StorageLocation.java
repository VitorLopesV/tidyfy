package com.tidyfy.infrastructure.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Local de armazenamento do Tidyfy: a pasta {@code %APPDATA%\Tidyfy}.
 * <p>
 * Concentra os caminhos dos arquivos de configuração, de histórico e dos logs.
 * O local pode ser trocado nos testes informando diretamente a pasta.
 */
public final class StorageLocation {

    /** Pasta do Tidyfy. */
    private final Path folder;

    // Constantes

    /** Nome da variável de ambiente com a pasta AppData do usuário. */
    private static final String APPDATA_VARIABLE = "APPDATA";

    /** Nome da pasta do Tidyfy dentro do AppData. */
    private static final String FOLDER_NAME = "Tidyfy";

    /** Nome do arquivo de configuração dos ajudantes. */
    private static final String HELPERS_FILE_NAME = "helpers.txt";

    /** Nome do arquivo de histórico de execuções. */
    private static final String HISTORY_FILE_NAME = "history.txt";

    /** Nome da pasta de logs. */
    private static final String LOGS_FOLDER_NAME = "logs";

    /** Nome da pasta AppData do usuário, usada quando a variável de ambiente não existe. */
    private static final String APPDATA_FALLBACK_FOLDER = "AppData";

    /** Nome da subpasta Roaming do AppData, usada quando a variável de ambiente não existe. */
    private static final String ROAMING_FALLBACK_FOLDER = "Roaming";

    /** Propriedade do sistema com a pasta do usuário. */
    private static final String USER_HOME_PROPERTY = "user.home";

    /** Mensagem de erro ao criar a pasta do Tidyfy. */
    private static final String CREATE_ERROR = "Não foi possível criar a pasta %s";

    /**
     * Cria um local apontando diretamente para a pasta informada.
     *
     * @param folder pasta do Tidyfy
     */
    public StorageLocation(Path folder) {
        this.folder = folder;
    }

    /**
     * Local padrão da máquina: {@code %APPDATA%\Tidyfy}.
     *
     * @return o local padrão
     */
    public static StorageLocation defaultLocation() {
        return forWindows(System::getenv, Path.of(System.getProperty(USER_HOME_PROPERTY)));
    }

    /**
     * Resolve o local a partir do ambiente informado.
     * Sem a variável {@code APPDATA}, usa {@code <userHome>\AppData\Roaming}.
     *
     * @param environment consulta de variáveis de ambiente (devolve {@code null} se não existir)
     * @param userHome    pasta do usuário
     * @return o local resolvido
     */
    public static StorageLocation forWindows(Function<String, String> environment, Path userHome) {
        String appData = environment.apply(APPDATA_VARIABLE);
        Path root = appData == null || appData.isBlank()
                ? userHome.resolve(APPDATA_FALLBACK_FOLDER).resolve(ROAMING_FALLBACK_FOLDER)
                : Path.of(appData);
        return new StorageLocation(root.resolve(FOLDER_NAME));
    }

    /**
     * Garante que a pasta do Tidyfy exista, criando-a (e as pastas pais) se necessário.
     *
     * @return a pasta do Tidyfy
     * @throws UncheckedIOException se a pasta não puder ser criada
     */
    public Path directory() {
        try {
            return Files.createDirectories(folder);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(CREATE_ERROR, folder), e);
        }
    }

    /**
     * Caminho do arquivo de configuração dos ajudantes. Não cria o arquivo.
     *
     * @return o caminho de {@code helpers.txt}
     */
    public Path helpersFile() {
        return folder.resolve(HELPERS_FILE_NAME);
    }

    /**
     * Caminho do arquivo de histórico. Não cria o arquivo.
     *
     * @return o caminho de {@code history.txt}
     */
    public Path historyFile() {
        return folder.resolve(HISTORY_FILE_NAME);
    }

    /**
     * Caminho da pasta de logs. Não cria a pasta.
     *
     * @return o caminho de {@code logs}
     */
    public Path logsDirectory() {
        return folder.resolve(LOGS_FOLDER_NAME);
    }
}
