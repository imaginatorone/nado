package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TrustRatingResponse {
    private Long userId;
    private String userName;
    private int totalScore;
    private String level;

    // детализация
    private int profileScore;
    private int ageScore;
    private int dealsScore;
    private int reviewsScore;
    private int complaintsScore;

    // исходные данные
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private String region;
    private Integer completedDeals;
    private Integer complaints;
    private LocalDateTime memberSince;
}
