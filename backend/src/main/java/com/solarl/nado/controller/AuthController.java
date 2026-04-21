package com.solarl.nado.controller;

import com.solarl.nado.dto.request.LoginRequest;
import com.solarl.nado.dto.request.RegisterRequest;
import com.solarl.nado.dto.response.AuthResponse;
import com.solarl.nado.service.AuthService;
import com.solarl.nado.service.CaptchaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Регистрация и вход")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // проверка капчи обязательна при регистрации
        if (request.getCaptchaId() == null || request.getCaptchaCode() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Введите текст с картинки"));
        }
        if (!captchaService.verify(request.getCaptchaId(), request.getCaptchaCode())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Неверный текст с картинки"));
        }

        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
