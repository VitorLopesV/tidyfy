package com.tidyfy.domain;

/**
 * Tipo de ação que um ajudante realizou (ou tentou realizar) em uma execução.
 */
public enum ActionType {

    /** Arquivo movido para uma subpasta (organização de pasta). */
    MOVED,

    /** Arquivo duplicado enviado para a lixeira (organização de pasta). */
    DUPLICATE_TRASHED,

    /** Arquivo enviado para a lixeira (limpeza de pasta). */
    TRASHED,

    /** Lixeira do Windows esvaziada (limpeza da lixeira). */
    RECYCLE_BIN_EMPTIED,

    /** Falha ao executar; a causa fica na mensagem da ação. */
    FAILED
}
