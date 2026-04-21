package com.solarl.nado.controller;

import com.solarl.nado.dto.request.UpdateProfileRequest;
import com.solarl.nado.dto.response.UserPrivateResponse;
import com.solarl.nado.dto.response.UserPublicResponse;
import com.solarl.nado.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Приватный профиль — только для владельца */
    @GetMapping("/me")
    public ResponseEntity<UserPrivateResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    /** Обновление профиля — типизированный DTO вместо Map */
    @PutMapping("/me")
    public ResponseEntity<UserPrivateResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<UserPrivateResponse> uploadAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(userService.uploadAvatar(file));
    }

    /** Публичный профиль — без email и phone */
    @GetMapping("/{id}")
    public ResponseEntity<UserPublicResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}
