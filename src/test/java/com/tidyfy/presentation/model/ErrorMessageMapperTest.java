package com.tidyfy.presentation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.DesktopArrangerAlreadyExistsException;
import com.tidyfy.domain.FolderAlreadyInUseException;
import com.tidyfy.domain.HelperLimitReachedException;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.HelperNameAlreadyUsedException;
import com.tidyfy.domain.HelperType;
import com.tidyfy.domain.ProtectedFolderException;
import com.tidyfy.infrastructure.persistence.CorruptedStorageException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Testa a conversão de exceções nas mensagens exibidas ao usuário.
 */
class ErrorMessageMapperTest {

    @Test
    void corruptedStorageExplainsThatTheFileWasKeptAndABackupExists() {
        ErrorMessage message = ErrorMessageMapper.toMessage(
                new CorruptedStorageException(Path.of("helpers.txt"), new RuntimeException("json ruim")));

        assertEquals("Arquivo corrompido", message.title());
        assertTrue(message.message().contains("não alterou o arquivo"));
        assertTrue(message.message().contains(".bak"));
        assertTrue(message.details().startsWith(CorruptedStorageException.class.getName()));
        assertTrue(message.details().contains("json ruim"));
    }

    @Test
    void ioErrorSaysFilesCouldNotBeAccessed() {
        ErrorMessage message = ErrorMessageMapper.toMessage(new UncheckedIOException(new IOException("disco cheio")));

        assertEquals("Não foi possível acessar os arquivos", message.title());
        assertTrue(message.details().startsWith(UncheckedIOException.class.getName()));
    }

    @Test
    void domainErrorsKeepTheirOwnMessage() {
        HelperLimitReachedException limit = new HelperLimitReachedException(HelperType.CLEANER);
        HelperNameAlreadyUsedException name = new HelperNameAlreadyUsedException(new HelperName("Fotos"));
        ProtectedFolderException protectedFolder = new ProtectedFolderException(Path.of("C:\\Windows"));
        FolderAlreadyInUseException inUse =
                FolderAlreadyInUseException.forFolder(Path.of("C:\\Fotos"), new HelperName("Fotos"));

        assertDomain("Limite de ajudantes atingido", limit);
        assertDomain("Nome já usado", name);
        assertDomain("Pasta protegida", protectedFolder);
        assertDomain("Pasta já usada", inUse);
        assertDomain("Organizador da área de trabalho já existe", new DesktopArrangerAlreadyExistsException());
    }

    @Test
    void anyOtherErrorIsGenericWithTechnicalDetails() {
        ErrorMessage message = ErrorMessageMapper.toMessage(new IllegalStateException("quebrou"));

        assertEquals("Algo deu errado", message.title());
        assertEquals("java.lang.IllegalStateException: quebrou", message.details());
    }

    @Test
    void nullMessageLeavesOnlyTheTypeInTheDetails() {
        ErrorMessage message = ErrorMessageMapper.toMessage(new IllegalStateException());

        assertEquals("java.lang.IllegalStateException", message.details());
    }

    @Test
    void nullErrorIsRejected() {
        assertThrows(NullPointerException.class, () -> ErrorMessageMapper.toMessage(null));
    }

    private static void assertDomain(String title, RuntimeException error) {
        ErrorMessage message = ErrorMessageMapper.toMessage(error);

        assertEquals(title, message.title());
        assertEquals(error.getMessage(), message.message());
    }
}
