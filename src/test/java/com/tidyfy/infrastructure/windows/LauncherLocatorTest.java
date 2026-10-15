package com.tidyfy.infrastructure.windows;

import com.tidyfy.domain.HelperId;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testa a resolução do comando da tarefa agendada com ambientes informados por construtor.
 */
class LauncherLocatorTest {

    private static final String JAVA_HOME = "C:\\Program Files\\Java\\jdk-21";

    private static final HelperId ID = new HelperId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @Test
    void packagedExecutableIsUsedAloneWithoutBaseArguments() {
        TaskCommand command = new LauncherLocator(Optional.of("C:\\Program Files\\Tidyfy\\Tidyfy.exe"),
                "ignored.jar", JAVA_HOME).locate();

        assertEquals(Path.of("C:\\Program Files\\Tidyfy\\Tidyfy.exe"), command.executable());
        assertEquals(List.of(), command.baseArguments());
    }

    @Test
    void packagedExecutableNameIgnoresCase() {
        TaskCommand command = new LauncherLocator(Optional.of("C:\\App\\tidyfy.EXE"), "x", JAVA_HOME).locate();

        assertEquals(Path.of("C:\\App\\tidyfy.EXE"), command.executable());
        assertEquals(List.of(), command.baseArguments());
    }

    @Test
    void developmentModeUsesJavawWithClasspathAndMainClass() {
        TaskCommand command = new LauncherLocator(Optional.of("C:\\Java\\bin\\java.exe"),
                "C:\\proj\\target\\classes;C:\\lib\\a.jar", JAVA_HOME).locate();

        assertEquals(Path.of(JAVA_HOME, "bin", "javaw.exe"), command.executable());
        assertEquals(List.of("-cp", "C:\\proj\\target\\classes;C:\\lib\\a.jar", "com.tidyfy.Main"),
                command.baseArguments());
    }

    @Test
    void developmentClasspathWithSpacesIsQuotedInTheArgumentsLine() {
        TaskCommand command = new LauncherLocator(Optional.empty(), "C:\\my proj\\classes;C:\\lib\\a.jar", JAVA_HOME)
                .locate();

        assertEquals("-cp \"C:\\my proj\\classes;C:\\lib\\a.jar\" com.tidyfy.Main --run "
                + "00000000-0000-0000-0000-000000000001", command.argumentsFor(ID));
    }

    @Test
    void missingProcessInformationFallsBackToJavawFromJavaHome() {
        TaskCommand command = new LauncherLocator(Optional.empty(), null, JAVA_HOME).locate();

        assertEquals(Path.of(JAVA_HOME, "bin", "javaw.exe"), command.executable());
        assertEquals(List.of("com.tidyfy.Main"), command.baseArguments());
    }

    @Test
    void blankClasspathOmitsTheClasspathOption() {
        TaskCommand command = new LauncherLocator(Optional.empty(), "  ", JAVA_HOME).locate();

        assertEquals(List.of("com.tidyfy.Main"), command.baseArguments());
    }
}
