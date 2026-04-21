package com.solarl.nado.controller;

import com.solarl.nado.service.ImageService;
import com.solarl.nado.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import com.solarl.nado.exception.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final UserService userService;

    @PostMapping("/ads/{adId}/images")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @PathVariable Long adId,
            @RequestParam("file") MultipartFile file) throws IOException {

        Long currentUserId = userService.getCurrentUserId();
        var image = imageService.uploadImageForAd(adId, currentUserId, file);

        return ResponseEntity.ok(Map.of(
                "id", image.getId(),
                "url", "/images/" + image.getId()
        ));
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) throws MalformedURLException {
        Path filePath = imageService.getImagePath(id);
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            throw new ResourceNotFoundException("Файл не найден");
        }

        MediaType mediaType;
        try {
            String detectedType = Files.probeContentType(filePath);
            mediaType = detectedType != null
                    ? MediaType.parseMediaType(detectedType)
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (IOException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + filePath.getFileName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(mediaType)
                .body(resource);
    }

    @DeleteMapping("/images/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        Long currentUserId = userService.getCurrentUserId();
        imageService.deleteImage(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
