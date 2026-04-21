package com.solarl.nado.dto.request;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class RatingCreateRequest {

    @NotNull(message = "Оценка обязательна")
    @Min(value = 1, message = "Оценка от 1 до 5")
    @Max(value = 5, message = "Оценка от 1 до 5")
    private Integer score;

    @Size(max = 1000, message = "Отзыв не должен превышать 1000 символов")
    private String review;
}
