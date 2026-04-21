package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Ответ на переключение активности пользователя.
 */
@Data
@Builder
public class ToggleActiveResponse {
    private Long id;
    private boolean active;
}
