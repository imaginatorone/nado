package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String type;
    private String payload;
    private boolean isRead;
    private LocalDateTime createdAt;
}
