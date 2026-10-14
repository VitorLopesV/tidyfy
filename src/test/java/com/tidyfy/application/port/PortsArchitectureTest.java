package com.tidyfy.application.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Teste de arquitetura das portas: garante que as 5 interfaces existem com as assinaturas
 * do diagrama e que não dependem de JavaFX, infraestrutura nem apresentação.
 */
class PortsArchitectureTest {

    private static final List<String> FORBIDDEN =
            List.of("javafx", "com.tidyfy.infrastructure", "com.tidyfy.presentation");

    private static Set<String> signatures(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(m -> m.getName() + ":" + m.getReturnType().getSimpleName())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static void assertPort(Class<?> type, String... expected) {
        assertTrue(type.isInterface());
        assertEquals(new TreeSet<>(List.of(expected)), signatures(type));
    }

    @Test
    void helperRepositoryDeclaresDiagramMethods() {
        assertPort(HelperRepository.class,
                "save:void", "findById:Optional", "findAll:List", "countByType:int", "delete:void");
    }

    @Test
    void executionHistoryRepositoryDeclaresDiagramMethods() {
        assertPort(ExecutionHistoryRepository.class, "add:void", "findByHelper:List", "deleteExpired:void");
    }

    @Test
    void taskSchedulerDeclaresDiagramMethods() {
        assertPort(TaskScheduler.class, "register:void", "unregister:void", "registeredIds:List");
    }

    @Test
    void fileSystemDeclaresDiagramMethods() {
        assertPort(FileSystem.class,
                "listEntries:List", "exists:boolean", "sameContent:boolean", "move:void", "moveToTrash:void");
    }

    @Test
    void recycleBinDeclaresDiagramMethods() {
        assertPort(RecycleBin.class, "emptyAll:int", "emptyOlderThan:int");
    }

    @Test
    void portSignaturesDoNotDependOnOuterLayers() {
        List<Class<?>> ports = List.of(HelperRepository.class, ExecutionHistoryRepository.class,
                TaskScheduler.class, FileSystem.class, RecycleBin.class);
        for (Class<?> port : ports) {
            List<Type> types = new ArrayList<>();
            for (Method method : port.getDeclaredMethods()) {
                types.add(method.getGenericReturnType());
                types.addAll(List.of(method.getGenericParameterTypes()));
            }
            for (Type type : types) {
                for (String forbidden : FORBIDDEN) {
                    assertFalse(type.getTypeName().contains(forbidden),
                            port.getSimpleName() + " depende de " + type.getTypeName());
                }
            }
        }
    }
}
