package com.tidyfy.domain;

/**
 * O que a Limpeza vai limpar: a lixeira do Windows ou uma pasta.
 * Só existem esses dois alvos.
 */
public sealed interface CleanupTarget permits RecycleBinTarget, FolderTarget {
}
