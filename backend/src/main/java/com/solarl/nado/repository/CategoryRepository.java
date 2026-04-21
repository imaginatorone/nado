package com.solarl.nado.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.solarl.nado.entity.Category;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL ORDER BY c.id")
    List<Category> findAllRootCategories();

    List<Category> findByParentId(Long parentId);
}
