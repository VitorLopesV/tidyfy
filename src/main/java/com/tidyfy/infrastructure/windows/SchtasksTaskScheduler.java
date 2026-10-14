package com.tidyfy.infrastructure.windows;

import com.tidyfy.application.port.TaskScheduler;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.Schedule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Agendador de tarefas baseado no {@code schtasks.exe} do Windows.
 * Existe uma única tarefa por ajudante ({@code Tidyfy\{id}}), definida em XML com um gatilho por horário.
 * O XML é gravado em arquivo temporário (UTF-16 com BOM, que é o que o {@code schtasks} espera), entregue a
 * {@code schtasks /Create /XML} e apagado em seguida, com sucesso ou falha. A opção {@code /F} substitui a
 * tarefa existente, então registrar de novo atualiza a tarefa.
 * A tarefa roda só com o usuário logado, mesmo na bateria, e executa assim que possível se perdeu o horário.
 */
public final class SchtasksTaskScheduler implements TaskScheduler {

    /** Executa o {@code schtasks}. */
    private final CommandRunner commandRunner;

    /** Executável do Tidyfy chamado pela tarefa. */
    private final Path launcher;

    /** Pasta onde o XML temporário é gravado. */
    private final Path tempDirectory;

    // Constantes

    /** Programa de linha de comando do Agendador de Tarefas. */
    private static final String SCHTASKS = "schtasks";

    /** Opção que cria a tarefa. */
    private static final String CREATE_OPTION = "/Create";

    /** Opção do nome da tarefa. */
    private static final String TASK_NAME_OPTION = "/TN";

    /** Opção do arquivo XML da tarefa. */
    private static final String XML_OPTION = "/XML";

    /** Opção que substitui a tarefa existente. */
    private static final String FORCE_OPTION = "/F";

    /** Modelo do nome da tarefa: pasta Tidyfy e id do ajudante. */
    private static final String TASK_NAME_TEMPLATE = "Tidyfy\\%s";

    /** Modelo dos argumentos da ação: executa o ajudante pelo id. */
    private static final String ARGUMENTS_TEMPLATE = "--run %s";

    /** Prefixo do arquivo XML temporário. */
    private static final String TEMP_FILE_PREFIX = "tidyfy-task-";

    /** Sufixo do arquivo XML temporário. */
    private static final String TEMP_FILE_SUFFIX = ".xml";

    /** Formato do início de cada gatilho (data e hora). */
    private static final DateTimeFormatter START_BOUNDARY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** Marca de ordem de bytes (BOM) gravada no início do arquivo UTF-16. */
    private static final String BOM = "﻿";

    /** Mensagem de falha do comando: id do ajudante, código de saída e saída. */
    private static final String COMMAND_FAILURE_MESSAGE = "schtasks failed for helper %s (exit code %d): %s";

    /** Mensagem de falha ao gravar o XML temporário. */
    private static final String WRITE_FAILURE_MESSAGE = "Could not write the task XML for helper %s";

    /** Opção que apaga a tarefa. */
    private static final String DELETE_OPTION = "/Delete";

    /** Opção que consulta a tarefa. */
    private static final String QUERY_OPTION = "/Query";

    /** Opção do formato de saída da consulta. */
    private static final String FORMAT_OPTION = "/FO";

    /** Formato CSV da consulta. */
    private static final String CSV_FORMAT = "CSV";

    /** Opção que omite o cabeçalho da consulta. */
    private static final String NO_HEADER_OPTION = "/NH";

    /** Prefixo (em minúsculas) do nome completo das tarefas do Tidyfy na saída da consulta. */
    private static final String TASK_PATH_PREFIX = "\\tidyfy\\";

    /** Aspas que envolvem os campos do CSV. */
    private static final char CSV_QUOTE = '"';

    /** Quebra de linha em qualquer formato. */
    private static final String LINE_BREAK_REGEX = "\\R";

    /** Quantidade de caracteres de um UUID escrito por extenso. */
    private static final int UUID_LENGTH = 36;

    /** Mensagem de falha da listagem: código de saída e saída. */
    private static final String LIST_FAILURE_MESSAGE = "schtasks could not list the tasks (exit code %d): %s";

    /** Cabeçalho do XML, com a codificação gravada no arquivo. */
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n";

    /** Abertura da tarefa. */
    private static final String TASK_OPEN =
            "<Task version=\"1.2\" xmlns=\"http://schemas.microsoft.com/windows/2004/02/mit/task\">\n";

    /** Fechamento da tarefa. */
    private static final String TASK_CLOSE = "</Task>\n";

    /** Abertura da lista de gatilhos. */
    private static final String TRIGGERS_OPEN = "  <Triggers>\n";

    /** Fechamento da lista de gatilhos. */
    private static final String TRIGGERS_CLOSE = "  </Triggers>\n";

    /** Modelo de um gatilho: início e intervalo em dias. */
    private static final String TRIGGER_TEMPLATE = """
              <CalendarTrigger>
                <StartBoundary>%s</StartBoundary>
                <Enabled>true</Enabled>
                <ScheduleByDay>
                  <DaysInterval>%d</DaysInterval>
                </ScheduleByDay>
              </CalendarTrigger>
            """;

    /** Principal (só com o usuário logado) e configurações (bateria, executar assim que possível). */
    private static final String PRINCIPALS_AND_SETTINGS = """
              <Principals>
                <Principal id="Author">
                  <LogonType>InteractiveToken</LogonType>
                  <RunLevel>LeastPrivilege</RunLevel>
                </Principal>
              </Principals>
              <Settings>
                <MultipleInstancesPolicy>IgnoreNew</MultipleInstancesPolicy>
                <DisallowStartIfOnBatteries>false</DisallowStartIfOnBatteries>
                <StopIfGoingOnBatteries>false</StopIfGoingOnBatteries>
                <StartWhenAvailable>true</StartWhenAvailable>
                <Enabled>true</Enabled>
              </Settings>
            """;

    /** Modelo das ações: comando e argumentos. */
    private static final String ACTIONS_TEMPLATE = """
              <Actions Context="Author">
                <Exec>
                  <Command>%s</Command>
                  <Arguments>%s</Arguments>
                </Exec>
              </Actions>
            """;

    /** Caractere {@code &} e sua entidade XML. */
    private static final String AMPERSAND = "&";

    /** Entidade XML do {@code &}. */
    private static final String AMPERSAND_ENTITY = "&amp;";

    /** Caractere {@code <}. */
    private static final String LESS_THAN = "<";

    /** Entidade XML do {@code <}. */
    private static final String LESS_THAN_ENTITY = "&lt;";

    /** Caractere {@code >}. */
    private static final String GREATER_THAN = ">";

    /** Entidade XML do {@code >}. */
    private static final String GREATER_THAN_ENTITY = "&gt;";

    /** Aspas duplas. */
    private static final String QUOTE = "\"";

    /** Entidade XML das aspas duplas. */
    private static final String QUOTE_ENTITY = "&quot;";

    /** Apóstrofo. */
    private static final String APOSTROPHE = "'";

    /** Entidade XML do apóstrofo. */
    private static final String APOSTROPHE_ENTITY = "&apos;";

    /**
     * Cria o agendador.
     *
     * @param commandRunner executa o {@code schtasks}
     * @param launcher      executável do Tidyfy que a tarefa chama com {@code --run {id}}
     * @param tempDirectory pasta (existente) onde o XML temporário é gravado
     */
    public SchtasksTaskScheduler(CommandRunner commandRunner, Path launcher, Path tempDirectory) {
        this.commandRunner = Objects.requireNonNull(commandRunner);
        this.launcher = Objects.requireNonNull(launcher);
        this.tempDirectory = Objects.requireNonNull(tempDirectory);
    }

    /**
     * Registra a tarefa do ajudante, criando ou substituindo a existente.
     *
     * @param helper ajudante a agendar
     * @throws UncheckedIOException se o XML não puder ser gravado ou o {@code schtasks} falhar
     */
    @Override
    public void register(Helper helper) {
        Objects.requireNonNull(helper);
        Path xmlFile = null;
        try {
            xmlFile = Files.createTempFile(tempDirectory, TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
            Files.write(xmlFile, (BOM + buildXml(helper)).getBytes(StandardCharsets.UTF_16LE));
            CommandResult result = commandRunner.run(createCommand(helper.id(), xmlFile));
            if (!result.succeeded()) {
                throw new UncheckedIOException(new IOException(String.format(
                        COMMAND_FAILURE_MESSAGE, helper.id().value(), result.exitCode(), result.output())));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(WRITE_FAILURE_MESSAGE, helper.id().value()), e);
        } finally {
            deleteQuietly(xmlFile);
        }
    }

    /**
     * Monta o comando que cria (ou substitui) a tarefa do ajudante a partir do XML.
     *
     * @param id      identificador do ajudante
     * @param xmlFile arquivo XML da tarefa
     * @return programa e argumentos
     */
    List<String> createCommand(HelperId id, Path xmlFile) {
        return List.of(SCHTASKS, CREATE_OPTION, TASK_NAME_OPTION, String.format(TASK_NAME_TEMPLATE, id.value()),
                XML_OPTION, xmlFile.toString(), FORCE_OPTION);
    }

    /**
     * Monta o XML da tarefa: um gatilho por horário, a cada {@code intervalDays} dias desde a data de início.
     *
     * @param helper ajudante a agendar
     * @return o texto XML (sem a marca de ordem de bytes)
     */
    String buildXml(Helper helper) {
        Schedule schedule = helper.schedule();
        StringBuilder xml = new StringBuilder(XML_HEADER).append(TASK_OPEN).append(TRIGGERS_OPEN);
        for (LocalTime time : schedule.times()) {
            String start = schedule.startDate().atTime(time).format(START_BOUNDARY_FORMAT);
            xml.append(String.format(TRIGGER_TEMPLATE, start, schedule.intervalDays()));
        }
        xml.append(TRIGGERS_CLOSE).append(PRINCIPALS_AND_SETTINGS)
                .append(String.format(ACTIONS_TEMPLATE, escape(launcher.toString()),
                        escape(String.format(ARGUMENTS_TEMPLATE, helper.id().value()))))
                .append(TASK_CLOSE);
        return xml.toString();
    }

    /**
     * Escapa os caracteres especiais do XML.
     */
    private static String escape(String text) {
        return text.replace(AMPERSAND, AMPERSAND_ENTITY).replace(LESS_THAN, LESS_THAN_ENTITY)
                .replace(GREATER_THAN, GREATER_THAN_ENTITY).replace(QUOTE, QUOTE_ENTITY)
                .replace(APOSTROPHE, APOSTROPHE_ENTITY);
    }

    /**
     * Apaga o arquivo temporário, ignorando falhas para não mascarar o resultado real do registro.
     */
    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Arquivo temporário: não vale mascarar o resultado do registro.
        }
    }

    /**
     * Remove a tarefa do ajudante. Tarefa inexistente não é erro: como a mensagem do {@code schtasks} é
     * localizada, depois de uma falha no {@code /Delete} a tarefa é consultada com {@code /Query}; se a consulta
     * também falhar, a tarefa não existe e nada é feito.
     * Tarefas antigas por horário ({@code Tidyfy\{id}\{n}}) não são tratadas: a tarefa agora é única por ajudante.
     *
     * @param id identificador do ajudante
     * @throws UncheckedIOException se o {@code schtasks} falhar e a tarefa ainda existir
     */
    @Override
    public void unregister(HelperId id) {
        Objects.requireNonNull(id);
        CommandResult result = commandRunner.run(deleteCommand(id));
        if (result.succeeded()) {
            return;
        }
        if (commandRunner.run(queryCommand(id)).succeeded()) {
            throw new UncheckedIOException(new IOException(String.format(
                    COMMAND_FAILURE_MESSAGE, id.value(), result.exitCode(), result.output())));
        }
    }

    /**
     * Monta o comando que apaga a tarefa do ajudante.
     *
     * @param id identificador do ajudante
     * @return programa e argumentos
     */
    List<String> deleteCommand(HelperId id) {
        return List.of(SCHTASKS, DELETE_OPTION, TASK_NAME_OPTION, String.format(TASK_NAME_TEMPLATE, id.value()),
                FORCE_OPTION);
    }

    /**
     * Monta o comando que consulta a tarefa do ajudante (falha se ela não existe).
     *
     * @param id identificador do ajudante
     * @return programa e argumentos
     */
    List<String> queryCommand(HelperId id) {
        return List.of(SCHTASKS, QUERY_OPTION, TASK_NAME_OPTION, String.format(TASK_NAME_TEMPLATE, id.value()));
    }

    /**
     * Monta o comando que lista todas as tarefas em CSV, sem cabeçalho.
     *
     * @return programa e argumentos
     */
    List<String> listCommand() {
        return List.of(SCHTASKS, QUERY_OPTION, FORMAT_OPTION, CSV_FORMAT, NO_HEADER_OPTION);
    }

    /**
     * Lista os ajudantes com tarefa em {@code Tidyfy\}. Lê o primeiro campo de cada linha do CSV; linhas
     * inválidas, mensagens informativas, tarefas de outras pastas e nomes que não são UUID são ignorados, o que
     * independe do idioma do Windows. Sem tarefas devolve lista vazia.
     *
     * @return ids dos ajudantes com tarefa, sem repetição
     * @throws UncheckedIOException se o {@code schtasks} falhar
     */
    @Override
    public List<HelperId> registeredIds() {
        CommandResult result = commandRunner.run(listCommand());
        if (!result.succeeded()) {
            throw new UncheckedIOException(new IOException(
                    String.format(LIST_FAILURE_MESSAGE, result.exitCode(), result.output())));
        }
        List<HelperId> ids = new ArrayList<>();
        for (String line : result.output().split(LINE_BREAK_REGEX)) {
            HelperId id = parseTaskId(line);
            if (id != null && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * Extrai o id do ajudante do primeiro campo da linha, ou {@code null} se a linha não for de uma tarefa dele.
     */
    private static HelperId parseTaskId(String line) {
        String text = line.strip();
        if (text.isEmpty() || text.charAt(0) != CSV_QUOTE) {
            return null;
        }
        int end = text.indexOf(CSV_QUOTE, 1);
        if (end < 0) {
            return null;
        }
        String name = text.substring(1, end);
        if (!name.toLowerCase(Locale.ROOT).startsWith(TASK_PATH_PREFIX)) {
            return null;
        }
        String uuid = name.substring(TASK_PATH_PREFIX.length());
        if (uuid.length() != UUID_LENGTH) {
            return null;
        }
        try {
            return new HelperId(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
