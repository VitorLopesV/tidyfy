package com.tidyfy.presentation.component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/**
 * Carrega FXML com o controller criado no código, para permitir injeção por construtor.
 */
public final class Fxml {

    // Constantes

    /** Modelo da mensagem de falha ao carregar; recebe o endereço do FXML. */
    private static final String LOAD_ERROR = "Não foi possível carregar %s";

    /**
     * Impede a criação de instâncias: a classe só tem métodos estáticos.
     */
    private Fxml() {
    }

    /**
     * Carrega um FXML usando o controller informado.
     *
     * @param location   endereço do FXML
     * @param controller controller já criado, que recebe os campos {@code @FXML}
     * @param <T>        tipo do nó raiz do FXML
     * @return o nó raiz carregado
     * @throws UncheckedIOException se o FXML não puder ser carregado
     */
    public static <T extends Parent> T load(URL location, Object controller) {
        FXMLLoader loader = new FXMLLoader(location);
        loader.setController(controller);
        return load(loader);
    }

    /**
     * Carrega um FXML de componente {@code fx:root}: o próprio componente é a raiz e o controller.
     *
     * @param location  endereço do FXML
     * @param component componente que será a raiz e o controller
     * @throws UncheckedIOException se o FXML não puder ser carregado
     */
    public static void loadRoot(URL location, Parent component) {
        FXMLLoader loader = new FXMLLoader(location);
        loader.setRoot(component);
        loader.setController(component);
        load(loader);
    }

    /**
     * Executa o carregamento, convertendo a falha de leitura em exceção não verificada.
     *
     * @param loader carregador já configurado
     * @param <T>    tipo do nó raiz do FXML
     * @return o nó raiz carregado
     */
    private static <T> T load(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(LOAD_ERROR, loader.getLocation()), e);
        }
    }
}
