package com.solarl.nado.dto.request;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class LoginRequest {

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    private String password;
}
