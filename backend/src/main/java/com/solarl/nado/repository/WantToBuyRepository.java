package com.solarl.nado.repository;

import com.solarl.nado.entity.WantToBuy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface WantToBuyRepository extends JpaRepository<WantToBuy, Long> {

    List<WantToBuy> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<WantToBuy> findByUserIdAndActiveTrue(Long userId);

    /**
     * Найти активные запросы «Хочу купить», которые совпадают с новым объявлением.
     * Совпадение по: ключевым словам в query (ILIKE), категории (nullable), диапазону цены.
     */
    @Query("SELECT w FROM WantToBuy w WHERE w.active = true " +
           "AND (:title IS NULL OR LOWER(:title) LIKE CONCAT('%', LOWER(w.query), '%')) " +
           "AND (w.category IS NULL OR w.category.id = :categoryId) " +
           "AND (w.priceFrom IS NULL OR :price >= w.priceFrom) " +
           "AND (w.priceTo IS NULL OR :price <= w.priceTo)")
    List<WantToBuy> findMatchingRequests(
            @Param("title") String title,
            @Param("categoryId") Long categoryId,
            @Param("price") BigDecimal price);
}
