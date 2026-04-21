package com.solarl.nado.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Request body для отклонения объявления модератором.
 */
@Data
public class ModerationRejectRequest {

    @NotBlank(message = "причина отклонения обязательна")
    private String reason;
}
