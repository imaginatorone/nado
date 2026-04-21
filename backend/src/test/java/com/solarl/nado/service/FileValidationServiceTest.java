package com.solarl.nado.service;

import com.solarl.nado.config.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты валидации файлов — проверка расширений и реального содержимого.
 */
class FileValidationServiceTest {

    private FileValidationService service;

    @BeforeEach
    void setUp() {
        StorageProperties props = new StorageProperties();
        props.setAllowedImageExtensions(List.of(".jpg", ".jpeg", ".png", ".gif", ".webp"));
        props.setAllowedFileExtensions(List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf", ".doc", ".docx", ".mp4"));
        service = new FileValidationService(props);
    }

    @Test
    @DisplayName("Допустимое изображение: валидация проходит")
    void validateImageFile_validJpeg_passes() throws IOException {
        MockMultipartFile file = createValidImageFile("test.jpg", "jpg");
        assertDoesNotThrow(() -> service.validateImageFile(file));
    }

    @Test
    @DisplayName("Допустимое PNG: валидация проходит")
    void validateImageFile_validPng_passes() throws IOException {
        MockMultipartFile file = createValidImageFile("test.png", "png");
        assertDoesNotThrow(() -> service.validateImageFile(file));
    }

    @Test
    @DisplayName("Недопустимое расширение: .exe отклоняется")
    void validateImageFile_exeExtension_rejected() {
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe",
                "application/octet-stream", new byte[]{0x4D, 0x5A});

        assertThrows(IllegalArgumentException.class, () -> service.validateImageFile(file));
    }

    @Test
    @DisplayName("Подделка расширения: .jpg с exe-содержимым отклоняется")
    void validateImageFile_fakeJpg_rejected() {
        MockMultipartFile file = new MockMultipartFile("file", "fake.jpg",
                "image/jpeg", new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00});

        assertThrows(IllegalArgumentException.class, () -> service.validateImageFile(file));
    }

    @Test
    @DisplayName("Пустой файл: отклоняется")
    void validateImageFile_empty_rejected() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg",
                "image/jpeg", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> service.validateImageFile(file));
    }

    @Test
    @DisplayName("Файл чата: PDF допустим")
    void validateFile_pdf_passes() {
        // PDF magic bytes
        byte[] pdfContent = "%PDF-1.4 fake content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf",
                "application/pdf", pdfContent);

        assertDoesNotThrow(() -> service.validateFile(file));
    }

    @Test
    @DisplayName("Файл чата: .sh недопустим")
    void validateFile_shell_rejected() {
        MockMultipartFile file = new MockMultipartFile("file", "script.sh",
                "text/x-shellscript", "#!/bin/bash\nrm -rf /".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.validateFile(file));
    }

    @Test
    @DisplayName("Безопасный Content-Disposition: image → inline")
    void safeContentDisposition_image_inline() {
        String result = service.safeContentDisposition("image/jpeg", "photo.jpg");
        assertEquals("inline", result);
    }

    @Test
    @DisplayName("Безопасный Content-Disposition: PDF → attachment")
    void safeContentDisposition_pdf_attachment() {
        String result = service.safeContentDisposition("application/pdf", "doc.pdf");
        assertTrue(result.startsWith("attachment"));
    }

    private MockMultipartFile createValidImageFile(String name, String format) throws IOException {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format.equals("jpg") ? "jpeg" : format, baos);
        return new MockMultipartFile("file", name, "image/" + format, baos.toByteArray());
    }
}
