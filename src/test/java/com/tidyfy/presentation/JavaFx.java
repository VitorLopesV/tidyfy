package com.tidyfy.presentation;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Inicia o toolkit do JavaFX uma vez e executa o código do teste na thread da interface. */
public final class JavaFx {

    private static boolean started;

    private JavaFx() {
    }

    public static <T> T call(Callable<T> work) {
        start();
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(work.call());
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        try {
            return result.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw rethrow(e.getCause());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void run(FxWork work) {
        call(() -> {
            work.run();
            return null;
        });
    }

    /** Coloca a tela numa janela de 1280 × 900 (sem mostrá-la) com o tema aplicado, como no aplicativo. */
    public static Parent mount(Parent view) {
        return mount(view, 1280, 900);
    }

    public static Parent mount(Parent view, double width, double height) {
        Scene scene = new Scene(view, width, height);
        scene.getStylesheets().add(TidyfyApplication.stylesheet());
        new Stage().setScene(scene);
        view.applyCss();
        view.layout();
        return view;
    }

    private static synchronized void start() {
        if (started) {
            return;
        }
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // O toolkit já foi iniciado por outra classe de teste.
        }
        Platform.setImplicitExit(false);
        started = true;
    }

    private static RuntimeException rethrow(Throwable failure) {
        if (failure instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(failure);
    }

    @FunctionalInterface
    public interface FxWork {
        void run() throws Exception;
    }
}
