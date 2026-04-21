package com.solarl.nado.dto.request;

import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * Запрос на создание/открытие чата по объявлению.
 */
@Data
public class StartChatRequest {
    @NotNull(message = "ID объявления обязателен")
    private Long adId;
}
