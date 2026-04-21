package com.solarl.nado.dto.request;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class BidRequest {

    @NotNull(message = "Сумма ставки обязательна")
    @DecimalMin(value = "1.0", message = "Сумма должна быть больше 0")
    private BigDecimal amount;
}
