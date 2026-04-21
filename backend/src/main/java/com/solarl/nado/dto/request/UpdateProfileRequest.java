package com.solarl.nado.dto.request;

import lombok.Data;
import javax.validation.constraints.Size;

/**
 * Запрос на обновление профиля пользователя.
 */
@Data
public class UpdateProfileRequest {
    @Size(min = 1, max = 100, message = "Имя должно содержать от 1 до 100 символов")
    private String name;

    @Size(max = 20, message = "Телефон не должен превышать 20 символов")
    private String phone;

    @Size(max = 100, message = "Регион не должен превышать 100 символов")
    private String region;
}
