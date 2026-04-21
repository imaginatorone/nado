package com.solarl.nado.service;

import com.solarl.nado.config.StorageProperties;
import com.solarl.nado.dto.request.UpdateProfileRequest;
import com.solarl.nado.dto.response.PhoneResponse;
import com.solarl.nado.dto.response.ToggleActiveResponse;
import com.solarl.nado.dto.response.UserPrivateResponse;
import com.solarl.nado.dto.response.UserPublicResponse;
import com.solarl.nado.entity.User;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.repository.RatingRepository;
import com.solarl.nado.repository.UserRepository;
import com.solarl.nado.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AdRepository adRepository;
    private final RatingRepository ratingRepository;
    private final StorageProperties storageProperties;
    private final FileValidationService fileValidationService;
    private final AuthFacade authFacade;


    @Transactional(readOnly = true)
    public UserPrivateResponse getCurrentUser() {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        return mapToPrivateResponse(user);
    }


    @Transactional(readOnly = true)
    public UserPublicResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        return mapToPublicResponse(user);
    }

    @Transactional
    public UserPrivateResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUserEntity();
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getRegion() != null) {
            user.setRegion(request.getRegion());
        }
        user = userRepository.save(user);
        return mapToPrivateResponse(user);
    }

    @Transactional
    public UserPrivateResponse uploadAvatar(MultipartFile file) throws IOException {
        fileValidationService.validateImageFile(file);

        User user = getCurrentUserEntity();

        Path uploadPath = Paths.get(storageProperties.getDir(), "avatars");
        Files.createDirectories(uploadPath);

        String ext = fileValidationService.getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + ext;
        Path target = uploadPath.resolve(filename);
        file.transferTo(target.toFile());

        user.setAvatarUrl("/uploads/avatars/" + filename);
        user = userRepository.save(user);
        return mapToPrivateResponse(user);
    }


    @Transactional(readOnly = true)
    public List<UserPrivateResponse> getAllUsersForAdmin() {
        return userRepository.findAll().stream()
                .map(this::mapToPrivateResponse)
                .collect(Collectors.toList());
    }


    @Transactional
    public User toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        user.setActive(!user.getActive());
        return userRepository.save(user);
    }


    @Transactional
    public ToggleActiveResponse toggleUserActiveAndRespond(Long userId) {
        User user = toggleUserActive(userId);
        return ToggleActiveResponse.builder()
                .id(user.getId())
                .active(user.getActive())
                .build();
    }


    @Transactional
    public void banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getRole() == User.Role.ADMIN) {
            throw new IllegalArgumentException("Нельзя заблокировать администратора");
        }
        user.setActive(false);
        userRepository.save(user);
    }


    public PhoneResponse revealPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return PhoneResponse.builder()
                    .phone(null)
                    .masked(null)
                    .revealed(false)
                    .build();
        }
        return PhoneResponse.builder()
                .phone(phone)
                .masked(maskPhone(phone))
                .revealed(true)
                .build();
    }

    public long countAll() {
        return userRepository.count();
    }

    // делегация в AuthFacade — единственный вход в identity context
    public Long getCurrentUserId() {
        return authFacade.getCurrentUserId();
    }

    public User getCurrentUserEntity() {
        return authFacade.getCurrentNadoUser();
    }

    public String getCurrentUserRole() {
        return authFacade.getCurrentRole();
    }



    private UserPrivateResponse mapToPrivateResponse(User user) {
        long adsCount = adRepository.countByUserId(user.getId());
        long reviewsCount = ratingRepository.countBySellerId(user.getId());

        return UserPrivateResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .active(user.getActive())
                .phoneVerified(user.getPhoneVerified())
                .emailVerified(user.getEmailVerified())
                .region(user.getRegion())
                .avatarUrl(user.getAvatarUrl())
                .completedDeals(user.getCompletedDeals())
                .complaints(user.getComplaints())
                .adsCount(adsCount)
                .reviewsCount(reviewsCount)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserPublicResponse mapToPublicResponse(User user) {
        long adsCount = adRepository.countByUserId(user.getId());
        long reviewsCount = ratingRepository.countBySellerId(user.getId());

        return UserPublicResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .role(user.getRole().name())
                .region(user.getRegion())
                .avatarUrl(user.getAvatarUrl())
                .completedDeals(user.getCompletedDeals())
                .adsCount(adsCount)
                .reviewsCount(reviewsCount)
                .createdAt(user.getCreatedAt())
                .build();
    }


    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.length() < 4) return "***";
        return digits.substring(0, Math.min(4, digits.length()))
                + " *** ** "
                + digits.substring(digits.length() - 2);
    }
}
