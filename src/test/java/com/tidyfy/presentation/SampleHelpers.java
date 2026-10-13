package com.tidyfy.presentation;

import com.tidyfy.presentation.model.HelperItem;
import com.tidyfy.presentation.model.HelperKind;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Ajudantes de exemplo usados pelos testes de tela.
 */
public final class SampleHelpers {

    private SampleHelpers() {
    }

    public static HelperItem organizer(String id, String name) {
        return new HelperItem(id, name, HelperKind.ORGANIZER, Path.of("C:\\Users\\ana\\Downloads"), 1,
                List.of(LocalTime.of(9, 0), LocalTime.of(18, 0)), true, "Hoje, 18:00", 0.5);
    }

    public static HelperItem folderCleaner(String id, String name) {
        return new HelperItem(id, name, HelperKind.CLEANER, Path.of("C:\\Temp"), 3,
                List.of(LocalTime.of(8, 0)), false, null, 0);
    }

    public static HelperItem recycleBinCleaner(String id, String name) {
        return new HelperItem(id, name, HelperKind.CLEANER, null, 7,
                List.of(LocalTime.of(20, 0)), true, "Domingo, 20:00", 0.3);
    }

    /** {@code count} ajudantes alternando entre os três exemplos acima. */
    public static List<HelperItem> many(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> switch (i % 3) {
                    case 0 -> organizer("o" + i, "Organizar Downloads");
                    case 1 -> folderCleaner("c" + i, "Limpar arquivos temporários");
                    default -> recycleBinCleaner("b" + i, "Esvaziar lixeira toda semana");
                })
                .toList();
    }
}
