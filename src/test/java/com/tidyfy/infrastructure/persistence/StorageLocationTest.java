package com.tidyfy.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes de {@link StorageLocation}. Usam apenas diretórios temporários, nunca o AppData real.
 */
class StorageLocationTest {

    /** Pasta temporária do teste. */
    @TempDir
    Path temp;

    /** A pasta do Tidyfy fica dentro do AppData informado pela variável de ambiente. */
    @Test
    void returnsTidyfyFolderInsideAppData() {
        Path appData = temp.resolve("Roaming");
        StorageLocation location = StorageLocation.forWindows(
                Map.of("APPDATA", appData.toString())::get, temp.resolve("home"));

        assertEquals(appData.resolve("Tidyfy"), location.directory());
    }

    /** Sem a variável APPDATA, usa user.home\AppData\Roaming. */
    @Test
    void fallsBackToUserHomeWhenAppDataVariableIsMissing() {
        Path home = temp.resolve("home");
        StorageLocation location = StorageLocation.forWindows(name -> null, home);

        assertEquals(home.resolve("AppData").resolve("Roaming").resolve("Tidyfy"), location.directory());
    }

    /** Variável APPDATA em branco também cai no fallback. */
    @Test
    void fallsBackWhenAppDataVariableIsBlank() {
        Path home = temp.resolve("home");
        StorageLocation location = StorageLocation.forWindows(name -> "  ", home);

        assertEquals(home.resolve("AppData").resolve("Roaming").resolve("Tidyfy"), location.directory());
    }

    /** Cria a pasta e as pastas pais quando não existem. */
    @Test
    void createsFolderAndParentsWhenMissing() {
        Path folder = temp.resolve("a").resolve("b").resolve("Tidyfy");

        Path result = new StorageLocation(folder).directory();

        assertEquals(folder, result);
        assertTrue(Files.isDirectory(folder));
    }

    /** Não falha nem apaga o conteúdo quando a pasta já existe. */
    @Test
    void doesNotFailWhenFolderAlreadyExists() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("Tidyfy"));
        Path existing = Files.createFile(folder.resolve("helpers.txt"));
        StorageLocation location = new StorageLocation(folder);

        assertDoesNotThrow(location::directory);
        assertTrue(Files.exists(existing));
    }

    /** Falha de E/S vira UncheckedIOException (aqui, um arquivo ocupa o lugar da pasta). */
    @Test
    void wrapsIoFailureInUncheckedIOException() throws IOException {
        Path blocker = Files.createFile(temp.resolve("blocker"));
        StorageLocation location = new StorageLocation(blocker.resolve("Tidyfy"));

        assertThrows(UncheckedIOException.class, location::directory);
    }

    /** Devolve os caminhos dos arquivos e da pasta de logs sem criá-los. */
    @Test
    void returnsFilePathsWithoutCreatingThem() {
        Path folder = temp.resolve("Tidyfy");
        StorageLocation location = new StorageLocation(folder);

        assertEquals(folder.resolve("helpers.txt"), location.helpersFile());
        assertEquals(folder.resolve("history.txt"), location.historyFile());
        assertEquals(folder.resolve("logs"), location.logsDirectory());
        assertFalse(Files.exists(folder));
    }
}
