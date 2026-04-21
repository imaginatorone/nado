package com.solarl.nado.dto.request;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class WantToBuyRequest {

    @NotBlank(message = "Укажите, что вы ищете")
    @Size(max = 255, message = "Максимум 255 символов")
    private String query;

    private Long categoryId;

    @DecimalMin(value = "0", message = "Цена не может быть отрицательной")
    private BigDecimal priceFrom;

    @DecimalMin(value = "0", message = "Цена не может быть отрицательной")
    private BigDecimal priceTo;

    @Size(max = 100)
    private String region;
}
