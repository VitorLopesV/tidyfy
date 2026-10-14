package com.tidyfy.presentation.model;

import com.tidyfy.domain.FolderAlreadyInUseException;
import com.tidyfy.domain.HelperLimitReachedException;
import com.tidyfy.domain.HelperNameAlreadyUsedException;
import com.tidyfy.domain.ProtectedFolderException;
import java.io.UncheckedIOException;

/**
 * Converte uma exceção na mensagem em português exibida ao usuário.
 * <p>
 * A exceção de arquivo corrompido pertence à infraestrutura, que a apresentação não pode importar. Por isso
 * ela é reconhecida pelo nome simples da classe (ou de alguma superclasse), sem criar nenhuma porta nova.
 */
public final class ErrorMessageMapper {

    // Constantes

    /** Nome simples da exceção de armazenamento corrompido, da camada de infraestrutura. */
    private static final String CORRUPTED_STORAGE_SIMPLE_NAME = "CorruptedStorageException";

    /** Título do erro de arquivo corrompido. */
    private static final String CORRUPTED_TITLE = "Arquivo corrompido";

    /** Mensagem do erro de arquivo corrompido. */
    private static final String CORRUPTED_MESSAGE =
            "Um arquivo de dados do Tidyfy está corrompido ou tem uma versão desconhecida. "
                    + "O Tidyfy não alterou o arquivo e guardou uma cópia de segurança (.bak) na mesma pasta. "
                    + "Verifique o arquivo ou restaure a cópia de segurança e abra o Tidyfy de novo.";

    /** Título do erro de acesso aos arquivos. */
    private static final String IO_TITLE = "Não foi possível acessar os arquivos";

    /** Mensagem do erro de acesso aos arquivos. */
    private static final String IO_MESSAGE =
            "O Tidyfy não conseguiu ler ou gravar um arquivo. Verifique se o arquivo ou a pasta existe e se "
                    + "você tem permissão para usá-lo, e tente de novo.";

    /** Título do erro de limite de ajudantes. */
    private static final String LIMIT_TITLE = "Limite de ajudantes atingido";

    /** Título do erro de nome já usado. */
    private static final String NAME_TITLE = "Nome já usado";

    /** Título do erro de pasta já usada. */
    private static final String FOLDER_IN_USE_TITLE = "Pasta já usada";

    /** Título do erro de pasta protegida. */
    private static final String PROTECTED_TITLE = "Pasta protegida";

    /** Título do erro inesperado. */
    private static final String UNEXPECTED_TITLE = "Algo deu errado";

    /** Mensagem do erro inesperado. */
    private static final String UNEXPECTED_MESSAGE =
            "Ocorreu um erro inesperado. Tente de novo; se o problema continuar, feche e abra o Tidyfy. "
                    + "Os detalhes técnicos podem ajudar a entender o que houve.";

    /** Modelo do detalhe técnico com mensagem; recebe o tipo e a mensagem da exceção. */
    private static final String DETAILS_WITH_MESSAGE = "%s: %s";

    /**
     * Impede a criação de instâncias: a classe só tem métodos estáticos.
     */
    private ErrorMessageMapper() {
    }

    /**
     * Converte a exceção na mensagem exibida ao usuário. O detalhe técnico traz sempre o tipo da exceção e,
     * quando houver, a mensagem dela.
     *
     * @param error exceção ocorrida
     * @return título, mensagem e detalhe técnico
     * @throws NullPointerException se {@code error} for nulo
     */
    public static ErrorMessage toMessage(Throwable error) {
        String details = detailsOf(error);
        if (isCorruptedStorage(error)) {
            return new ErrorMessage(CORRUPTED_TITLE, CORRUPTED_MESSAGE, details);
        }
        if (error instanceof UncheckedIOException) {
            return new ErrorMessage(IO_TITLE, IO_MESSAGE, details);
        }
        if (error instanceof HelperLimitReachedException) {
            return domain(LIMIT_TITLE, error, details);
        }
        if (error instanceof HelperNameAlreadyUsedException) {
            return domain(NAME_TITLE, error, details);
        }
        if (error instanceof FolderAlreadyInUseException) {
            return domain(FOLDER_IN_USE_TITLE, error, details);
        }
        if (error instanceof ProtectedFolderException) {
            return domain(PROTECTED_TITLE, error, details);
        }
        return new ErrorMessage(UNEXPECTED_TITLE, UNEXPECTED_MESSAGE, details);
    }

    /**
     * Monta a mensagem de um erro de domínio, que já traz o texto em português.
     */
    private static ErrorMessage domain(String title, Throwable error, String details) {
        return new ErrorMessage(title, error.getMessage(), details);
    }

    /**
     * Indica se a exceção é a de armazenamento corrompido, olhando o nome da classe e o das superclasses.
     */
    private static boolean isCorruptedStorage(Throwable error) {
        for (Class<?> type = error.getClass(); type != null; type = type.getSuperclass()) {
            if (CORRUPTED_STORAGE_SIMPLE_NAME.equals(type.getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Monta o detalhe técnico: tipo da exceção e, se houver, a mensagem.
     */
    private static String detailsOf(Throwable error) {
        String type = error.getClass().getName();
        return error.getMessage() == null ? type : String.format(DETAILS_WITH_MESSAGE, type, error.getMessage());
    }
}
