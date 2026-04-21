package com.solarl.nado.service;

import com.solarl.nado.dto.response.CategoryResponse;
import com.solarl.nado.entity.Category;
import com.solarl.nado.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    @Cacheable("categories")
    public List<CategoryResponse> getAllCategories() {
        List<Category> roots = categoryRepository.findAllRootCategories();
        return roots.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private CategoryResponse mapToResponse(Category category) {
        List<CategoryResponse> children = null;
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            children = category.getChildren().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .children(children)
                .build();
    }
}
