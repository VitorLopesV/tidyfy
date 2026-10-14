package com.tidyfy.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Teste de fumaça da configuração do Jackson.
 * <p>
 * Prova que um {@link ObjectMapper} com {@link JavaTimeModule} converte os tipos de
 * {@code java.time} e um {@link Path} (como texto) em JSON e de volta sem perdas.
 */
class JacksonSetupTest {

    /** Conversor JSON configurado como será usado na persistência */
    private ObjectMapper mapper;

    /**
     * Cria o conversor com o módulo de datas e datas em texto ISO-8601.
     */
    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Verifica a ida e volta de uma data.
     */
    @Test
    void roundTripsLocalDate() throws IOException {
        LocalDate date = LocalDate.of(2026, 3, 15);
        String json = mapper.writeValueAsString(date);
        assertEquals("\"2026-03-15\"", json);
        assertEquals(date, mapper.readValue(json, LocalDate.class));
    }

    /**
     * Verifica a ida e volta de um horário.
     */
    @Test
    void roundTripsLocalTime() throws IOException {
        LocalTime time = LocalTime.of(8, 30);
        String json = mapper.writeValueAsString(time);
        assertEquals("\"08:30:00\"", json);
        assertEquals(time, mapper.readValue(json, LocalTime.class));
    }

    /**
     * Verifica a ida e volta de um instante.
     */
    @Test
    void roundTripsInstant() throws IOException {
        Instant instant = Instant.parse("2026-03-15T10:15:30Z");
        String json = mapper.writeValueAsString(instant);
        assertEquals("\"2026-03-15T10:15:30Z\"", json);
        assertEquals(instant, mapper.readValue(json, Instant.class));
    }

    /**
     * Verifica a ida e volta de um caminho convertido para texto.
     */
    @Test
    void roundTripsPathAsText() throws IOException {
        Path path = Path.of("C:", "Users", "nome", "Downloads");
        String json = mapper.writeValueAsString(path.toString());
        Path restored = Path.of(mapper.readValue(json, String.class));
        assertEquals(path, restored);
    }
}
