package com.solarl.nado.controller;

import com.solarl.nado.service.PhoneVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/phone-verification")
@RequiredArgsConstructor
public class PhoneVerificationController {

    private final PhoneVerificationService verificationService;

    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        verificationService.requestCode(phone);
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");
        boolean verified = verificationService.verifyCode(phone, code);
        return ResponseEntity.ok(Map.of("verified", verified));
    }
}
