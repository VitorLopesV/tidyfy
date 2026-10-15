package com.tidyfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Protege o contrato do empacotamento: o {@code main.class} do {@code pom.xml} (usado no manifesto do
 * JAR e no {@code jpackage}) aponta para {@link Main}, que tem {@code public static void main} e não
 * herda de {@code javafx.application.Application}, para o {@code --run} funcionar sem o JavaFX.
 */
class PackagingContractTest {

    /** Arquivo de build, relativo à pasta do projeto onde o Maven roda os testes. */
    private static final Path POM = Path.of("pom.xml");

    /** Propriedade do POM com a classe principal. */
    private static final Pattern MAIN_CLASS_PROPERTY = Pattern.compile("<main.class>([^<]+)</main.class>");

    /** Nome da superclasse do JavaFX que o ponto de entrada não pode estender. */
    private static final String JAVAFX_APPLICATION = "javafx.application.Application";

    /** Referência à propriedade usada no manifesto e no jpackage. */
    private static final String MAIN_CLASS_REFERENCE = "${main.class}";

    /** Perfil que gera o pacote executável. */
    private static final String PACKAGE_PROFILE = "<id>package</id>";

    /** Lê o conteúdo do POM. */
    private static String readPom() throws IOException {
        return Files.readString(POM, StandardCharsets.UTF_8);
    }

    /** Devolve a classe principal declarada no POM. */
    private static Class<?> declaredMainClass() throws Exception {
        Matcher matcher = MAIN_CLASS_PROPERTY.matcher(readPom());
        assertTrue(matcher.find(), "main.class deve estar declarado no pom.xml");
        return Class.forName(matcher.group(1).trim());
    }

    @Test
    void pomMainClassIsTheMainEntryPoint() throws Exception {
        assertEquals(Main.class, declaredMainClass());
    }

    @Test
    void mainClassHasPublicStaticMainMethod() throws Exception {
        Method main = declaredMainClass().getMethod("main", String[].class);

        assertTrue(Modifier.isStatic(main.getModifiers()));
        assertEquals(void.class, main.getReturnType());
    }

    @Test
    void mainClassDoesNotExtendJavaFxApplication() throws Exception {
        for (Class<?> type = declaredMainClass(); type != null; type = type.getSuperclass()) {
            assertFalse(JAVAFX_APPLICATION.equals(type.getName()),
                    "Main não pode herdar de Application, senão o --run exigiria o JavaFX");
        }
    }

    @Test
    void jarManifestAndPackageProfileUseTheMainClassProperty() throws Exception {
        String pom = readPom();

        assertTrue(pom.contains("<mainClass>" + MAIN_CLASS_REFERENCE + "</mainClass>"));
        assertTrue(pom.contains(PACKAGE_PROFILE));
        assertTrue(pom.contains("<argument>" + MAIN_CLASS_REFERENCE + "</argument>"));
    }
}
