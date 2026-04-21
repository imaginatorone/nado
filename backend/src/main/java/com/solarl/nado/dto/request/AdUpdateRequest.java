package com.solarl.nado.dto.request;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class AdUpdateRequest {

    @Size(max = 255, message = "Заголовок не должен превышать 255 символов")
    private String title;

    private String description;

    @DecimalMin(value = "0.0", message = "Цена не может быть отрицательной")
    private BigDecimal price;

    private Long categoryId;
}
