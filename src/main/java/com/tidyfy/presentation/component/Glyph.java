package com.tidyfy.presentation.component;

/**
 * Ícones de traço desenhados numa grade de 24 × 24.
 */
public enum Glyph {

    /** Sinal de mais. */
    PLUS(Glyph.PLUS_PATH),

    /** Sinal de menos. */
    MINUS(Glyph.MINUS_PATH),

    /** Xis de fechar. */
    CLOSE(Glyph.CLOSE_PATH),

    /** Marca de confirmação. */
    CHECK(Glyph.CHECK_PATH),

    /** Pasta. */
    FOLDER(Glyph.FOLDER_PATH),

    /** Pasta com linhas, para a organização. */
    FOLDER_SORTED(Glyph.FOLDER_PATH + Glyph.FOLDER_SORTED_LINES_PATH),

    /** Faíscas, para a limpeza. */
    SPARKLES(Glyph.SPARKLES_BIG_PATH + Glyph.SPARKLES_SMALL_PATH),

    /** Lixeira. */
    TRASH(Glyph.TRASH_PATH),

    /** Monitor com ícones, para a área de trabalho. */
    DESKTOP(Glyph.DESKTOP_PATH),

    /** Relógio. */
    CLOCK(Glyph.CLOCK_PATH),

    /** Lápis, para editar. */
    PENCIL(Glyph.PENCIL_PATH),

    /** Letra "i" num círculo. */
    INFO(Glyph.INFO_PATH),

    /** Pausa num círculo. */
    PAUSE(Glyph.PAUSE_PATH),

    /** Cadeado. */
    LOCK(Glyph.LOCK_PATH),

    /** Seta para a direita. */
    ARROW_RIGHT(Glyph.ARROW_RIGHT_PATH),

    /** Calendário. */
    CALENDAR(Glyph.CALENDAR_BODY_PATH + Glyph.CALENDAR_DETAILS_PATH),

    /** Triângulo de alerta. */
    ALERT(Glyph.ALERT_PATH);

    /** Traço do ícone, em notação SVG. */
    private final String path;

    // Constantes

    /** Traço do sinal de mais. */
    private static final String PLUS_PATH = "M12 5v14M5 12h14";

    /** Traço do sinal de menos. */
    private static final String MINUS_PATH = "M5 12h14";

    /** Traço do xis de fechar. */
    private static final String CLOSE_PATH = "M6 6l12 12M18 6L6 18";

    /** Traço da marca de confirmação. */
    private static final String CHECK_PATH = "M5 12.5l4.5 4.5L19 7.5";

    /** Traço da pasta. */
    private static final String FOLDER_PATH =
            "M3 7.5A2.5 2.5 0 0 1 5.5 5H9l2 2h7.5A2.5 2.5 0 0 1 21 9.5v8a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 17.5z";

    /** Linhas acrescentadas à pasta para indicar organização. */
    private static final String FOLDER_SORTED_LINES_PATH = "M8 12h8M8 15.5h5";

    /** Traço da faísca grande. */
    private static final String SPARKLES_BIG_PATH = "M10 3.5l1.6 4.4 4.4 1.6-4.4 1.6L10 15.5l-1.6-4.4L4 9.5l4.4-1.6z";

    /** Traço da faísca pequena. */
    private static final String SPARKLES_SMALL_PATH = "M17.5 13.5l.9 2.1 2.1.9-2.1.9-.9 2.1-.9-2.1-2.1-.9 2.1-.9z";

    /** Traço da lixeira. */
    private static final String TRASH_PATH =
            "M4 7h16M9 7V4.5h6V7M6 7l1 12.2A2 2 0 0 0 9 21h6a2 2 0 0 0 2-1.8L18 7M10 11v6M14 11v6";

    /** Traço do monitor com ícones. */
    private static final String DESKTOP_PATH =
            "M4.5 5h15A1.5 1.5 0 0 1 21 6.5v9a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 15.5v-9A1.5 1.5 0 0 1 4.5 5zM9 20h6M12 17v3M6.5 8h1.5M6.5 11h1.5M10 8h1.5";

    /** Traço do relógio. */
    private static final String CLOCK_PATH = "M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0-17 0M12 7.5V12l3 2";

    /** Traço do lápis. */
    private static final String PENCIL_PATH = "M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16zM13.5 6.5l4 4";

    /** Traço do ícone de informação. */
    private static final String INFO_PATH = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0M12 11v5M12 7.8v.1";

    /** Traço do ícone de pausa. */
    private static final String PAUSE_PATH = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0M10 9v6M14 9v6";

    /** Traço do cadeado. */
    private static final String LOCK_PATH =
            "M7 11h10a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2zM8 11V8a4 4 0 0 1 8 0v3";

    /** Traço da seta para a direita. */
    private static final String ARROW_RIGHT_PATH = "M5 12h14M13 6l6 6-6 6";

    /** Traço do corpo do calendário. */
    private static final String CALENDAR_BODY_PATH =
            "M6 5h12a2.5 2.5 0 0 1 2.5 2.5V18a2.5 2.5 0 0 1-2.5 2.5H6A2.5 2.5 0 0 1 3.5 18V7.5A2.5 2.5 0 0 1 6 5z";

    /** Traço da faixa e das argolas do calendário. */
    private static final String CALENDAR_DETAILS_PATH = "M3.5 10h17M8 3v4M16 3v4";

    /** Traço do triângulo de alerta. */
    private static final String ALERT_PATH = "M12 4l9 16H3zM12 10v4M12 17v.1";

    /**
     * Cria um ícone.
     *
     * @param path traço do ícone, em notação SVG
     */
    Glyph(String path) {
        this.path = path;
    }

    /**
     * Devolve o traço do ícone.
     *
     * @return traço em notação SVG
     */
    public String path() {
        return path;
    }
}
