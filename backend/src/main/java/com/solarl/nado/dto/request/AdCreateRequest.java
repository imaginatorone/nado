package com.solarl.nado.dto.request;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class AdCreateRequest {

    @NotBlank(message = "Заголовок обязателен")
    @Size(max = 255, message = "Заголовок не должен превышать 255 символов")
    private String title;

    @NotBlank(message = "Описание обязательно")
    private String description;

    @DecimalMin(value = "0.0", message = "Цена не может быть отрицательной")
    private BigDecimal price;

    @NotNull(message = "Категория обязательна")
    private Long categoryId;

    @Size(max = 100, message = "Регион не должен превышать 100 символов")
    private String region;
}
