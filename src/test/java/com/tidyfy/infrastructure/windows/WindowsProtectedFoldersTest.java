package com.tidyfy.infrastructure.windows;

import com.tidyfy.domain.ProtectedFolderException;
import com.tidyfy.domain.ProtectedFolders;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da montagem das pastas protegidas a partir de um ambiente injetado.
 */
class WindowsProtectedFoldersTest {

    private static final Path USER_HOME = Path.of("C:\\Users\\ana");

    private static Map<String, String> fullEnvironment() {
        Map<String, String> env = new HashMap<>();
        env.put("WINDIR", "C:\\Windows");
        env.put("ProgramFiles", "C:\\Program Files");
        env.put("ProgramFiles(x86)", "C:\\Program Files (x86)");
        env.put("ProgramData", "C:\\ProgramData");
        return env;
    }

    private static ProtectedFolders build(Map<String, String> env) {
        return WindowsProtectedFolders.from(env::get, USER_HOME);
    }

    private static void assertRefused(ProtectedFolders folders, String path) {
        assertThrows(ProtectedFolderException.class, () -> folders.assertAllowed(Path.of(path)));
    }

    @Test
    void refusesSystemFoldersFromEnvironment() {
        ProtectedFolders folders = build(fullEnvironment());
        assertRefused(folders, "C:\\Windows\\System32");
        assertRefused(folders, "C:\\Program Files\\X");
        assertRefused(folders, "C:\\Program Files (x86)\\Y");
        assertRefused(folders, "C:\\ProgramData\\Z");
        assertRefused(folders, "C:\\");
        assertRefused(folders, "C:\\Users\\ana");
    }

    @Test
    void acceptsFolderInsideUserHome() {
        ProtectedFolders folders = build(fullEnvironment());
        assertDoesNotThrow(() -> folders.assertAllowed(Path.of("C:\\Users\\ana\\Downloads")));
    }

    @Test
    void usesCustomPathsFromEnvironment() {
        Map<String, String> env = fullEnvironment();
        env.put("ProgramFiles", "E:\\Apps");
        assertRefused(build(env), "E:\\Apps\\Tool");
    }

    @Test
    void derivesMissingVariablesFromWindowsDriveRoot() {
        Map<String, String> env = new HashMap<>();
        env.put("WINDIR", "D:\\Windows");
        ProtectedFolders folders = build(env);
        assertRefused(folders, "D:\\Windows\\System32");
        assertRefused(folders, "D:\\Program Files\\X");
        assertRefused(folders, "D:\\Program Files (x86)\\Y");
        assertRefused(folders, "D:\\ProgramData\\Z");
        assertDoesNotThrow(() -> folders.assertAllowed(Path.of("C:\\Program Files\\X")));
    }

    @Test
    void treatsBlankVariableAsMissing() {
        Map<String, String> env = fullEnvironment();
        env.put("ProgramData", "   ");
        env.put("WINDIR", "D:\\Windows");
        assertRefused(build(env), "D:\\ProgramData\\Z");
    }

    @Test
    void usesSystemRootWhenWindirIsMissing() {
        Map<String, String> env = new HashMap<>();
        env.put("SystemRoot", "D:\\Windows");
        ProtectedFolders folders = build(env);
        assertRefused(folders, "D:\\Windows\\System32");
        assertRefused(folders, "D:\\ProgramData\\Z");
    }

    @Test
    void usesLastResortWhenEnvironmentIsEmpty() {
        ProtectedFolders folders = build(new HashMap<>());
        assertRefused(folders, "C:\\Windows\\System32");
        assertRefused(folders, "C:\\Program Files\\X");
        assertRefused(folders, "C:\\Program Files (x86)\\Y");
        assertRefused(folders, "C:\\ProgramData\\Z");
        assertRefused(folders, "C:\\Users\\ana");
    }

    @Test
    void failsClearlyWhenPathIsInvalid() {
        Map<String, String> env = fullEnvironment();
        env.put("ProgramData", "C:\\Pro<gram>Data");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> build(env));
        assertTrue(error.getMessage().contains("Pro<gram>Data"));
    }
}
