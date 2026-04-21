package com.solarl.nado.service;

import com.solarl.nado.dto.response.CategoryResponse;
import com.solarl.nado.entity.Category;
import com.solarl.nado.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("Получение категорий: иерархическое дерево")
    void getAllCategories_returnsHierarchicalTree() {
        // подготовка
        Category child1 = Category.builder().id(2L).name("Автомобили").children(new ArrayList<>()).build();
        Category child2 = Category.builder().id(3L).name("Мотоциклы").children(new ArrayList<>()).build();
        Category root = Category.builder().id(1L).name("Транспорт")
                .children(List.of(child1, child2)).build();
        child1.setParent(root);
        child2.setParent(root);

        when(categoryRepository.findAllRootCategories()).thenReturn(List.of(root));

        // выполнение
        List<CategoryResponse> result = categoryService.getAllCategories();

        // проверка
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Транспорт", result.get(0).getName());
        assertNull(result.get(0).getParentId());
        assertNotNull(result.get(0).getChildren());
        assertEquals(2, result.get(0).getChildren().size());
        assertEquals("Автомобили", result.get(0).getChildren().get(0).getName());
    }

    @Test
    @DisplayName("Получение категорий: пустой список")
    void getAllCategories_emptyList() {
        // подготовка
        when(categoryRepository.findAllRootCategories()).thenReturn(List.of());

        // выполнение
        List<CategoryResponse> result = categoryService.getAllCategories();

        // проверка
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
