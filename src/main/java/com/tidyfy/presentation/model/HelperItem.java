package com.tidyfy.presentation.model;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Ajudante como a lista e os formulários o exibem.
 *
 * @param id              identificador do ajudante
 * @param name            nome definido pelo usuário
 * @param kind            tipo do ajudante
 * @param folder          pasta-alvo; {@code null} quando o alvo é a lixeira do Windows
 * @param intervalDays    intervalo, em dias, entre as execuções
 * @param times           horários do dia em que o ajudante executa (guardados em ordem crescente)
 * @param enabled         {@code true} quando o ajudante está ligado
 * @param nextRun         texto da próxima execução (ex.: "Hoje, 18:00"); {@code null} quando desconhecida
 * @param nextRunProgress quanto do intervalo até a próxima execução já passou, de 0 a 1
 */
public record HelperItem(
        String id,
        String name,
        HelperKind kind,
        Path folder,
        int intervalDays,
        List<LocalTime> times,
        boolean enabled,
        String nextRun,
        double nextRunProgress) {

    // Constantes

    /** Nome do campo {@code id} nas mensagens de validação. */
    private static final String FIELD_ID = "id";

    /** Nome do campo {@code name} nas mensagens de validação. */
    private static final String FIELD_NAME = "name";

    /** Nome do campo {@code kind} nas mensagens de validação. */
    private static final String FIELD_KIND = "kind";

    /**
     * Valida os campos obrigatórios e ordena os horários.
     */
    public HelperItem {
        Objects.requireNonNull(id, FIELD_ID);
        Objects.requireNonNull(name, FIELD_NAME);
        Objects.requireNonNull(kind, FIELD_KIND);
        times = times.stream().sorted().toList();
    }

    /**
     * Informa se o alvo do ajudante é a lixeira do Windows.
     *
     * @return {@code true} quando não há pasta-alvo
     */
    public boolean targetsRecycleBin() {
        return folder == null;
    }

    /**
     * Cria uma cópia do ajudante com o estado ligado/desligado trocado.
     *
     * @param value novo estado
     * @return o ajudante atualizado
     */
    public HelperItem withEnabled(boolean value) {
        return new HelperItem(id, name, kind, folder, intervalDays, times, value, nextRun, nextRunProgress);
    }
}
