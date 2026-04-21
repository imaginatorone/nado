package com.solarl.nado.dto.request;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AuctionCreateRequest {

    @NotNull(message = "ID объявления обязателен")
    private Long adId;

    @NotNull(message = "Стартовая цена обязательна")
    @DecimalMin(value = "1.0", message = "Стартовая цена должна быть больше 0")
    private BigDecimal startPrice;

    @DecimalMin(value = "1.0", message = "Минимальный шаг должен быть больше 0")
    private BigDecimal minStep;

    @NotNull(message = "Дата окончания обязательна")
    private LocalDateTime endsAt;

    @Min(value = 1, message = "Продление минимум 1 минута")
    @Max(value = 30, message = "Продление максимум 30 минут")
    private Integer bidExtensionMinutes;
}
