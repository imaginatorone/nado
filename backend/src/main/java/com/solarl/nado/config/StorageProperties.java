package com.solarl.nado.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Единая конфигурация хранилища файлов.
 * Канонический property key: app.upload.dir — никаких дублей.
 */
@Data
@ConfigurationProperties(prefix = "app.upload")
public class StorageProperties {
    /** Каталог для загрузки файлов */
    private String dir = "./uploads";

    /** Максимум изображений на одно объявление */
    private int maxImagesPerAd = 10;

    /** Допустимые расширения для изображений (аватары, фото объявлений) */
    private List<String> allowedImageExtensions = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    /** Допустимые расширения для всех файлов (чат-вложения) */
    private List<String> allowedFileExtensions = List.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp",
            ".pdf", ".doc", ".docx",
            ".mp4", ".mov", ".avi"
    );
}
