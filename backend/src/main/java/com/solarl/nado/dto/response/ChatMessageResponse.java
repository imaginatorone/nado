package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private String content;
    private String attachmentUrl;
    private String attachmentType;
    private boolean read;
    private boolean mine;
    private LocalDateTime createdAt;
}
