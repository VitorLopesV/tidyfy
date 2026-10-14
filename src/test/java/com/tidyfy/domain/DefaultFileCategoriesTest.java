package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link DefaultFileCategories}.
 */
class DefaultFileCategoriesTest {

    private static FileCategory categoryNamed(String name) {
        return DefaultFileCategories.all().stream()
            .filter(category -> category.name().equals(name))
            .findFirst().orElseThrow();
    }

    private static Set<String> extensionsOf(String name) {
        return categoryNamed(name).extensions().stream().map(FileExtension::value).collect(Collectors.toSet());
    }

    @Test
    void hasTheFiveCategoriesWithTheirTargetFolders() {
        List<FileCategory> all = DefaultFileCategories.all();
        assertEquals(5, all.size());
        assertEquals(Set.of("Compactados", "Documentos", "Executáveis", "Imagens", "Vídeos"),
            all.stream().map(FileCategory::name).collect(Collectors.toSet()));
        all.forEach(category -> assertEquals(category.name(), category.targetFolderName()));
    }

    @Test
    void doesNotHaveCategoriesForFoldersAndOthers() {
        List<String> names = DefaultFileCategories.all().stream().map(FileCategory::name).toList();
        assertFalse(names.contains("Pastas"));
        assertFalse(names.contains("Outros"));
    }

    @Test
    void imagesHaveTheDefinedExtensions() {
        assertEquals(Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "heic", "tiff", "ico"),
            extensionsOf("Imagens"));
    }

    @Test
    void videosHaveTheDefinedExtensions() {
        assertEquals(Set.of("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v"), extensionsOf("Vídeos"));
    }

    @Test
    void documentsHaveTheDefinedExtensions() {
        assertEquals(Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods",
            "odp", "csv"), extensionsOf("Documentos"));
    }

    @Test
    void archivesHaveTheDefinedExtensions() {
        assertEquals(Set.of("zip", "rar", "7z", "tar", "gz"), extensionsOf("Compactados"));
    }

    @Test
    void executablesHaveTheDefinedExtensions() {
        assertEquals(Set.of("exe", "msi", "bat"), extensionsOf("Executáveis"));
    }

    @Test
    void noExtensionAppearsInTwoCategories() {
        Set<FileExtension> seen = new HashSet<>();
        for (FileCategory category : DefaultFileCategories.all()) {
            for (FileExtension extension : category.extensions()) {
                assertTrue(seen.add(extension), "Extensão repetida: " + extension.value());
            }
        }
    }

    @Test
    void matchesUsesNormalizedExtension() {
        FileCategory images = categoryNamed("Imagens");
        assertTrue(images.matches(Path.of("Foto.JPG")));
        assertFalse(images.matches(Path.of("relatorio.pdf")));
    }

    @Test
    void musicHasNoCategory() {
        assertTrue(DefaultFileCategories.all().stream().noneMatch(category -> category.matches(Path.of("a.mp3"))));
    }
}
