package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private Long parentId;
    private List<CategoryResponse> children;
}
