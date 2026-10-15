package com.tidyfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Protege o contrato do instalador: o perfil Maven {@code installer} existe, parte da imagem do perfil
 * {@code package}, tem UUID de atualização fixo e válido, e a personalização do {@code main.wxs} é um XML
 * bem formado que chama {@code --remove-tasks} na desinstalação. Não executa o {@code jpackage} nem precisa
 * do WiX.
 */
class InstallerContractTest {

    /** Arquivo de build. */
    private static final Path POM = Path.of("pom.xml");

    /** Personalização do modelo do WiX usada pelo jpackage. */
    private static final Path MAIN_WXS = Path.of("src", "installer", "resources", "main.wxs");

    /** Trecho do POM que começa no perfil do instalador. */
    private static final Pattern INSTALLER_PROFILE =
            Pattern.compile("<id>installer</id>(.*?)</profile>", Pattern.DOTALL);

    /** UUID de atualização declarado no perfil. */
    private static final Pattern UPGRADE_UUID =
            Pattern.compile("<installer.upgrade.uuid>([^<]+)</installer.upgrade.uuid>");

    /** Devolve o texto do perfil {@code installer}. */
    private static String installerProfile() throws IOException {
        Matcher matcher = INSTALLER_PROFILE.matcher(Files.readString(POM, StandardCharsets.UTF_8));
        assertTrue(matcher.find(), "o perfil installer deve existir no pom.xml");
        return matcher.group(1);
    }

    /** Lê o {@code main.wxs} como XML (falha se não for bem formado). */
    private static Document parseWxs() throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder().parse(MAIN_WXS.toFile());
    }

    @Test
    void pomIsWellFormedXml() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        assertNotNull(factory.newDocumentBuilder().parse(POM.toFile()));
    }

    @Test
    void installerProfileBuildsAnMsiFromThePackageProfileImage() throws Exception {
        String profile = installerProfile();

        assertTrue(profile.contains("<argument>msi</argument>"));
        assertTrue(profile.contains("<argument>--app-image</argument>"));
        assertTrue(profile.contains("${project.build.directory}/dist/${app.name}"));
        assertTrue(profile.contains("<argument>--win-menu</argument>"));
        assertTrue(profile.contains("<argument>--win-shortcut</argument>"));
        assertTrue(profile.contains("<argument>--win-per-user-install</argument>"));
        assertTrue(profile.contains("<argument>--resource-dir</argument>"));
        assertTrue(profile.contains("<phase>verify</phase>"));
    }

    @Test
    void installerUpgradeUuidIsFixedAndValid() throws Exception {
        String profile = installerProfile();
        Matcher matcher = UPGRADE_UUID.matcher(profile);

        assertTrue(matcher.find(), "o UUID de atualização deve ser fixo no pom.xml");
        String value = matcher.group(1).trim();
        assertEquals(value, UUID.fromString(value).toString());
        assertTrue(profile.contains("<argument>--win-upgrade-uuid</argument>"));
        assertTrue(profile.contains("<argument>${installer.upgrade.uuid}</argument>"));
    }

    @Test
    void installerVersionIsNumericForTheMsi() throws Exception {
        Matcher matcher = Pattern.compile("<installer.version>([^<]+)</installer.version>").matcher(installerProfile());

        assertTrue(matcher.find());
        assertTrue(matcher.group(1).matches("\\d+(\\.\\d+){0,2}"));
    }

    @Test
    void installerProfileDoesNotChangeThePackageProfileOrTheDefaultBuild() throws Exception {
        String pom = Files.readString(POM, StandardCharsets.UTF_8);
        int installerStart = pom.indexOf("<id>installer</id>");
        int packageStart = pom.indexOf("<id>package</id>");

        assertTrue(packageStart >= 0 && packageStart < installerStart);
        assertFalse(pom.substring(0, pom.indexOf("<profiles>")).contains("bin/jpackage"),
                "o jpackage só pode ser chamado dentro de perfis");
        assertFalse(pom.contains("<activeByDefault>true</activeByDefault>"));
    }

    @Test
    void mainWxsIsWellFormedXml() throws Exception {
        assertEquals("Wix", parseWxs().getDocumentElement().getLocalName());
    }

    @Test
    void mainWxsRunsRemoveTasksOnUninstallBeforeFilesAreRemoved() throws Exception {
        Document wxs = parseWxs();
        Element action = findById(wxs.getElementsByTagNameNS("*", "CustomAction"), "TidyfyRemoveTasks");

        assertNotNull(action, "a ação TidyfyRemoveTasks deve existir");
        assertEquals("INSTALLDIR", action.getAttribute("Directory"));
        assertTrue(action.getAttribute("ExeCommand").endsWith("--remove-tasks"));
        assertEquals("ignore", action.getAttribute("Return"));

        Element scheduled = null;
        NodeList customs = wxs.getElementsByTagNameNS("*", "Custom");
        for (int i = 0; i < customs.getLength(); i++) {
            Element custom = (Element) customs.item(i);
            if ("TidyfyRemoveTasks".equals(custom.getAttribute("Action"))) {
                scheduled = custom;
            }
        }
        assertNotNull(scheduled, "a ação deve estar agendada em InstallExecuteSequence");
        assertEquals("InstallExecuteSequence", scheduled.getParentNode().getLocalName());
        assertEquals("InstallInitialize", scheduled.getAttribute("After"));
        assertTrue(scheduled.getTextContent().contains("REMOVE=\"ALL\""));
    }

    @Test
    void mainWxsKeepsTheJpackageTemplateEssentials() throws Exception {
        String text = Files.readString(MAIN_WXS, StandardCharsets.UTF_8);

        assertTrue(text.contains("$(var.JpProductUpgradeCode)"));
        assertTrue(text.contains("<RemoveExistingProducts Before=\"CostInitialize\"/>"));
        assertTrue(text.contains("overrides.wxi"));
    }

    /** Procura o elemento com o {@code Id} informado. */
    private static Element findById(NodeList nodes, String id) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            if (id.equals(element.getAttribute("Id"))) {
                return element;
            }
        }
        return null;
    }
}
