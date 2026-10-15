package com.tidyfy.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifica a regra de dependência da arquitetura limpa: {@code presentation -> application -> domain} e
 * {@code infrastructure -> application -> domain}. Só as classes de montagem do pacote raiz
 * {@code com.tidyfy} (Main, AppContainer, HeadlessRunner...) conhecem todas as camadas.
 *
 * <p>As mesmas regras, parametrizadas pelo pacote raiz, são aplicadas a classes sintéticas que as violam
 * (pacote {@code fixture}) para provar que uma violação seria detectada. O código atual não tem violações,
 * portanto não há exceções registradas.
 */
class ArchitectureTest {

    private static final String ROOT = "com.tidyfy";
    private static final String FIXTURE_ROOT = "com.tidyfy.architecture.fixture";
    private static final String DOMAIN = ".domain..";
    private static final String APPLICATION = ".application..";
    private static final String INFRASTRUCTURE = ".infrastructure..";
    private static final String PRESENTATION = ".presentation..";
    private static final String JAVAFX = "javafx..";
    private static final String JAVA = "java..";
    private static final String SLICES_SUFFIX = ".(*)..";

    /** Classes de produção (sem as de teste) */
    private static JavaClasses production;

    /** Classes sintéticas que violam as regras */
    private static JavaClasses fixture;

    /** Importa as classes de produção e as sintéticas uma única vez. */
    @BeforeAll
    static void importClasses() {
        production = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ROOT);
        fixture = new ClassFileImporter().importPackages(FIXTURE_ROOT);
    }

    /**
     * O domínio só depende de Java puro.
     *
     * @param root pacote raiz das camadas
     * @return a regra
     */
    private static ArchRule domainIsPure(String root) {
        return noClasses().that().resideInAPackage(root + DOMAIN).should().dependOnClassesThat()
                .resideInAnyPackage(root + APPLICATION, root + INFRASTRUCTURE, root + PRESENTATION, JAVAFX,
                        "com.fasterxml..", "javax..", "org..");
    }

    /**
     * A aplicação só depende do domínio, dela mesma e de Java.
     *
     * @param root pacote raiz das camadas
     * @return a regra
     */
    private static ArchRule applicationDependsOnlyOnDomain(String root) {
        return classes().that().resideInAPackage(root + APPLICATION).should().onlyDependOnClassesThat()
                .resideInAnyPackage(root + DOMAIN, root + APPLICATION, JAVA);
    }

    /**
     * A apresentação não depende da infraestrutura.
     *
     * @param root pacote raiz das camadas
     * @return a regra
     */
    private static ArchRule presentationIgnoresInfrastructure(String root) {
        return noClasses().that().resideInAPackage(root + PRESENTATION).should().dependOnClassesThat()
                .resideInAPackage(root + INFRASTRUCTURE);
    }

    /**
     * A infraestrutura não depende da apresentação nem do JavaFX.
     *
     * @param root pacote raiz das camadas
     * @return a regra
     */
    private static ArchRule infrastructureIgnoresPresentation(String root) {
        return noClasses().that().resideInAPackage(root + INFRASTRUCTURE).should().dependOnClassesThat()
                .resideInAnyPackage(root + PRESENTATION, JAVAFX);
    }

    /**
     * Não há ciclos entre as camadas.
     *
     * @param root pacote raiz das camadas
     * @return a regra
     */
    private static ArchRule layersHaveNoCycles(String root) {
        return slices().matching(root + SLICES_SUFFIX).should().beFreeOfCycles();
    }

    /**
     * Nenhuma camada depende das classes de montagem do pacote raiz.
     *
     * @return a regra
     */
    private static ArchRule layersIgnoreAssembly() {
        return noClasses().that().resideInAnyPackage(ROOT + DOMAIN, ROOT + APPLICATION, ROOT + INFRASTRUCTURE,
                ROOT + PRESENTATION).should().dependOnClassesThat().resideInAPackage(ROOT);
    }

    /** O domínio não depende de outras camadas, do JavaFX nem de bibliotecas. */
    @Test
    void domainDependsOnlyOnPureJava() {
        domainIsPure(ROOT).check(production);
    }

    /** A aplicação só depende do domínio, dela mesma e de Java. */
    @Test
    void applicationDependsOnlyOnDomain() {
        applicationDependsOnlyOnDomain(ROOT).check(production);
    }

    /** A apresentação não depende da infraestrutura. */
    @Test
    void presentationDoesNotDependOnInfrastructure() {
        presentationIgnoresInfrastructure(ROOT).check(production);
    }

    /** A infraestrutura não depende da apresentação nem do JavaFX. */
    @Test
    void infrastructureDoesNotDependOnPresentationOrJavaFx() {
        infrastructureIgnoresPresentation(ROOT).check(production);
    }

    /** Não há ciclos entre as camadas. */
    @Test
    void layersHaveNoCyclicDependencies() {
        layersHaveNoCycles(ROOT).check(production);
    }

    /** As camadas não dependem das classes de montagem do pacote raiz. */
    @Test
    void layersDoNotDependOnAssemblyClasses() {
        layersIgnoreAssembly().check(production);
    }

    /** A regra do domínio detecta um domínio que depende da infraestrutura. */
    @Test
    void domainRuleDetectsViolation() {
        assertThrows(AssertionError.class, () -> domainIsPure(FIXTURE_ROOT).check(fixture));
    }

    /** A regra da aplicação detecta uma aplicação que depende da infraestrutura. */
    @Test
    void applicationRuleDetectsViolation() {
        assertThrows(AssertionError.class, () -> applicationDependsOnlyOnDomain(FIXTURE_ROOT).check(fixture));
    }

    /** A regra da apresentação detecta uma apresentação que depende da infraestrutura. */
    @Test
    void presentationRuleDetectsViolation() {
        assertThrows(AssertionError.class, () -> presentationIgnoresInfrastructure(FIXTURE_ROOT).check(fixture));
    }

    /** A regra da infraestrutura detecta uma infraestrutura que depende da apresentação. */
    @Test
    void infrastructureRuleDetectsViolation() {
        assertThrows(AssertionError.class, () -> infrastructureIgnoresPresentation(FIXTURE_ROOT).check(fixture));
    }

    /** A regra de ciclos detecta infraestrutura e apresentação dependendo uma da outra. */
    @Test
    void cycleRuleDetectsViolation() {
        assertThrows(AssertionError.class, () -> layersHaveNoCycles(FIXTURE_ROOT).check(fixture));
    }

    /** A regra de ciclos passa em um conjunto sem ciclos (o código de produção). */
    @Test
    void cycleRulePassesWithoutCycles() {
        assertDoesNotThrow(() -> layersHaveNoCycles(ROOT).check(production));
    }
}
