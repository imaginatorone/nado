package com.solarl.nado.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdSearchRequest {
    private String query;
    private Long categoryId;
    private String region;
    private Long userId;
    private BigDecimal priceFrom;
    private BigDecimal priceTo;
    private Boolean titleOnly;
    private Boolean withPhoto;
    private Integer page;
    private Integer size;
}
