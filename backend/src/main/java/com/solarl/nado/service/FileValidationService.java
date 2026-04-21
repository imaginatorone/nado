package com.solarl.nado.service;

import com.solarl.nado.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

// не доверяет Content-Type из multipart — проверяет реальный контент через ImageIO
@Slf4j
@Service
@RequiredArgsConstructor
public class FileValidationService {

    private final StorageProperties storageProperties;

    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }
        validateExtension(file, storageProperties.getAllowedImageExtensions());
        validateImageContent(file);
    }

    // изображения проверяются через ImageIO — не доверяем расширению
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }
        validateExtension(file, storageProperties.getAllowedFileExtensions());


        String ext = getExtension(file.getOriginalFilename());
        if (storageProperties.getAllowedImageExtensions().contains(ext)) {
            validateImageContent(file);
        }
    }

    public String detectMimeType(Path path) {
        try {
            String detected = Files.probeContentType(path);
            return detected != null ? detected : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    // inline только для изображений — остальное как attachment (XSS protection)
    public String safeContentDisposition(String mimeType, String filename) {
        if (mimeType != null && mimeType.startsWith("image/")) {
            return "inline";
        }
        String safeName = filename != null ? filename.replaceAll("[^a-zA-Z0-9._-]", "_") : "file";
        return "attachment; filename=\"" + safeName + "\"";
    }

    public String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase(Locale.ROOT) : "";
    }



    private void validateExtension(MultipartFile file, List<String> allowed) {
        String ext = getExtension(file.getOriginalFilename());
        if (ext.isEmpty() || !allowed.contains(ext)) {
            throw new IllegalArgumentException(
                    "Недопустимый тип файла: " + ext + ". Разрешены: " + String.join(", ", allowed));
        }
    }

    private void validateImageContent(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            BufferedImage img = ImageIO.read(is);
            if (img == null) {
                throw new IllegalArgumentException(
                        "Файл не является допустимым изображением");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Не удалось прочитать файл как изображение", e);
        }
    }
}
