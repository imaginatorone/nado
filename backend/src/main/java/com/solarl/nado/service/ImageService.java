package com.solarl.nado.service;

import com.solarl.nado.config.StorageProperties;
import com.solarl.nado.entity.Ad;
import com.solarl.nado.entity.Image;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final AdRepository adRepository;
    private final StorageProperties storageProperties;
    private final FileValidationService fileValidationService;

    /**
     * Загрузка изображения с проверкой владельца — вызывается из контроллера.
     * Контроллер НЕ обращается к AdRepository напрямую.
     */
    @Transactional
    public Image uploadImageForAd(Long adId, Long userId, MultipartFile file) throws IOException {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Объявление не найдено"));
        if (!ad.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Нет прав на загрузку фото к этому объявлению");
        }
        return uploadImage(ad, file);
    }

    @Transactional
    public Image uploadImage(Ad ad, MultipartFile file) throws IOException {
        fileValidationService.validateImageFile(file);

        int currentCount = imageRepository.countByAdId(ad.getId());
        if (currentCount >= storageProperties.getMaxImagesPerAd()) {
            throw new IllegalArgumentException(
                    "Максимальное количество фото: " + storageProperties.getMaxImagesPerAd());
        }

        String ext = fileValidationService.getExtension(file.getOriginalFilename());
        String newFilename = UUID.randomUUID().toString() + ext;

        Path uploadPath = Paths.get(storageProperties.getDir(), String.valueOf(ad.getId()));
        Files.createDirectories(uploadPath);

        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Image image = Image.builder()
                .ad(ad)
                .filePath(ad.getId() + "/" + newFilename)
                .sortOrder(currentCount)
                .build();

        Image saved = imageRepository.save(image);
        log.info("Загружено изображение: id={}, adId={}, file={}", saved.getId(), ad.getId(), newFilename);
        return saved;
    }

    @Transactional
    public void deleteImage(Long imageId, Long userId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Изображение не найдено"));

        if (!image.getAd().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Нет прав на удаление этого изображения");
        }

        try {
            Path filePath = Paths.get(storageProperties.getDir(), image.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }

        imageRepository.delete(image);
        log.info("Удалено изображение: id={}, adId={}", imageId, image.getAd().getId());
    }

    public Path getImagePath(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Изображение не найдено"));
        return Paths.get(storageProperties.getDir(), image.getFilePath());
    }
}
