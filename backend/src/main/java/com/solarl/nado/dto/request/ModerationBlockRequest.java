package com.solarl.nado.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Request body для блокировки объявления модератором.
 */
@Data
public class ModerationBlockRequest {

    @NotBlank(message = "причина блокировки обязательна")
    private String reason;
}
