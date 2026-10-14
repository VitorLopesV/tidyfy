package com.tidyfy.infrastructure.windows;

import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa o registro de tarefas do {@link SchtasksTaskScheduler} com um {@link CommandRunner} falso:
 * o {@code schtasks} real nunca é executado.
 */
class SchtasksTaskSchedulerRegisterTest {

    private static final Path LAUNCHER = Path.of("C:\\Program Files\\Tidyfy\\Tidyfy.exe");

    @TempDir
    Path tempDir;

    private final List<List<String>> commands = new ArrayList<>();
    private final List<String> xmlsSeen = new ArrayList<>();
    private final List<Path> xmlFilesSeen = new ArrayList<>();
    private CommandResult result = new CommandResult(0, "SUCCESS");

    private SchtasksTaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SchtasksTaskScheduler(command -> {
            commands.add(command);
            Path xml = Path.of(command.get(command.indexOf("/XML") + 1));
            xmlFilesSeen.add(xml);
            try {
                xmlsSeen.add(new String(Files.readAllBytes(xml), StandardCharsets.UTF_16));
            } catch (java.io.IOException e) {
                throw new UncheckedIOException(e);
            }
            return result;
        }, LAUNCHER, tempDir);
    }

    private static Helper helper(Schedule schedule) {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName("A"), schedule, true, Path.of("C:\\Pasta"));
    }

    private static Schedule schedule(int days, LocalTime... times) {
        return new Schedule(days, List.of(times), LocalDate.of(2026, 3, 5));
    }

    private static Document parse(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml.replace("encoding=\"UTF-16\"", "encoding=\"UTF-8\""))));
    }

    @Test
    void buildsCreateCommandWithTaskNameXmlAndForce() {
        Helper helper = helper(schedule(1, LocalTime.of(9, 0)));

        scheduler.register(helper);

        List<String> command = commands.get(0);
        assertEquals("schtasks", command.get(0));
        assertEquals("/Create", command.get(1));
        assertEquals("/TN", command.get(2));
        assertEquals("Tidyfy\\" + helper.id().value(), command.get(3));
        assertEquals("/XML", command.get(4));
        assertEquals(xmlFilesSeen.get(0).toString(), command.get(5));
        assertEquals("/F", command.get(6));
        assertEquals(7, command.size());
    }

    @Test
    void generatesOneTriggerForASingleTime() throws Exception {
        scheduler.register(helper(schedule(3, LocalTime.of(9, 30))));

        Document doc = parse(xmlsSeen.get(0));
        assertEquals(1, doc.getElementsByTagName("CalendarTrigger").getLength());
        assertEquals("2026-03-05T09:30:00", doc.getElementsByTagName("StartBoundary").item(0).getTextContent());
        assertEquals("3", doc.getElementsByTagName("DaysInterval").item(0).getTextContent());
    }

    @Test
    void generatesOneTriggerPerTime() throws Exception {
        scheduler.register(helper(schedule(2, LocalTime.of(8, 0), LocalTime.of(13, 15), LocalTime.of(20, 45))));

        Document doc = parse(xmlsSeen.get(0));
        assertEquals(3, doc.getElementsByTagName("CalendarTrigger").getLength());
        assertEquals("2026-03-05T08:00:00", doc.getElementsByTagName("StartBoundary").item(0).getTextContent());
        assertEquals("2026-03-05T13:15:00", doc.getElementsByTagName("StartBoundary").item(1).getTextContent());
        assertEquals("2026-03-05T20:45:00", doc.getElementsByTagName("StartBoundary").item(2).getTextContent());
        for (int i = 0; i < 3; i++) {
            assertEquals("2", doc.getElementsByTagName("DaysInterval").item(i).getTextContent());
        }
    }

    @Test
    void settingsAllowBatteryRunWhenAvailableAndInteractiveLogon() throws Exception {
        scheduler.register(helper(schedule(1, LocalTime.of(9, 0))));

        Document doc = parse(xmlsSeen.get(0));
        assertEquals("true", doc.getElementsByTagName("StartWhenAvailable").item(0).getTextContent());
        assertEquals("false", doc.getElementsByTagName("DisallowStartIfOnBatteries").item(0).getTextContent());
        assertEquals("false", doc.getElementsByTagName("StopIfGoingOnBatteries").item(0).getTextContent());
        assertEquals("IgnoreNew", doc.getElementsByTagName("MultipleInstancesPolicy").item(0).getTextContent());
        assertEquals("InteractiveToken", doc.getElementsByTagName("LogonType").item(0).getTextContent());
        assertEquals("LeastPrivilege", doc.getElementsByTagName("RunLevel").item(0).getTextContent());
    }

    @Test
    void actionCallsLauncherWithRunAndHelperId() throws Exception {
        Helper helper = helper(schedule(1, LocalTime.of(9, 0)));

        scheduler.register(helper);

        Document doc = parse(xmlsSeen.get(0));
        assertEquals(LAUNCHER.toString(), doc.getElementsByTagName("Command").item(0).getTextContent());
        assertEquals("--run " + helper.id().value(), doc.getElementsByTagName("Arguments").item(0).getTextContent());
    }

    @Test
    void escapesSpecialCharactersInLauncherPath() throws Exception {
        Path launcher = Path.of("C:\\Tom & Jerry's\\Tidyfy.exe");
        SchtasksTaskScheduler special = new SchtasksTaskScheduler(command -> new CommandResult(0, ""), launcher, tempDir);

        String xml = special.buildXml(helper(schedule(1, LocalTime.of(9, 0))));

        assertTrue(xml.contains("Tom &amp; Jerry&apos;s"));
        assertEquals(launcher.toString(), parse(xml).getElementsByTagName("Command").item(0).getTextContent());
    }

    @Test
    void registeringAgainUsesForceToReplaceTheTask() {
        Helper helper = helper(schedule(1, LocalTime.of(9, 0)));

        scheduler.register(helper);
        scheduler.register(helper);

        assertEquals(2, commands.size());
        assertEquals(commands.get(0).subList(0, 4), commands.get(1).subList(0, 4));
        assertTrue(commands.get(1).contains("/F"));
    }

    @Test
    void commandFailureBecomesUncheckedIOExceptionWithOutput() {
        result = new CommandResult(1, "ERROR: Access is denied");

        UncheckedIOException e = assertThrows(UncheckedIOException.class,
                () -> scheduler.register(helper(schedule(1, LocalTime.of(9, 0)))));

        assertTrue(e.getMessage().contains("ERROR: Access is denied"));
    }

    @Test
    void temporaryFileIsDeletedAfterSuccess() throws Exception {
        scheduler.register(helper(schedule(1, LocalTime.of(9, 0))));

        assertTrue(Files.notExists(xmlFilesSeen.get(0)));
        try (var files = Files.list(tempDir)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void temporaryFileIsDeletedAfterFailure() throws Exception {
        result = new CommandResult(1, "ERROR");

        assertThrows(UncheckedIOException.class,
                () -> scheduler.register(helper(schedule(1, LocalTime.of(9, 0)))));

        assertTrue(Files.notExists(xmlFilesSeen.get(0)));
        try (var files = Files.list(tempDir)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void xmlFileIsWrittenInUtf16WithBom() throws Exception {
        scheduler.register(helper(schedule(1, LocalTime.of(9, 0))));

        // O arquivo já foi apagado; o dublê o lê como UTF-16 (que consome o BOM) e o XML declara UTF-16.
        assertTrue(xmlsSeen.get(0).startsWith("<?xml version=\"1.0\" encoding=\"UTF-16\"?>"));
    }
}
