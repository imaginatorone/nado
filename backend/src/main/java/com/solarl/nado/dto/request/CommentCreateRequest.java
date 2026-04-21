package com.solarl.nado.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class CommentCreateRequest {

    @NotBlank(message = "Текст комментария обязателен")
    private String content;
}
