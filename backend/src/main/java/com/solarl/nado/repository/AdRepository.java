package com.solarl.nado.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.solarl.nado.entity.Ad;

import java.util.List;
import java.util.Optional;

public interface AdRepository extends JpaRepository<Ad, Long>, JpaSpecificationExecutor<Ad> {

    // публичная выдача — только PUBLISHED
    @Query("SELECT a FROM Ad a WHERE a.status = 'PUBLISHED' ORDER BY a.createdAt DESC")
    Page<Ad> findAllPublished(Pageable pageable);

    // кабинет владельца — всё кроме REMOVED
    @Query("SELECT a FROM Ad a WHERE a.user.id = :userId AND a.status <> 'REMOVED' ORDER BY a.createdAt DESC")
    Page<Ad> findByOwner(@Param("userId") Long userId, Pageable pageable);

    // кабинет владельца — конкретный статус
    Page<Ad> findByUserIdAndStatus(Long userId, Ad.Status status, Pageable pageable);

    // модерация — только PENDING_MODERATION
    @Query("SELECT a FROM Ad a WHERE a.status = 'PENDING_MODERATION' ORDER BY a.submittedAt ASC")
    Page<Ad> findPendingModeration(Pageable pageable);

    @Query("SELECT a FROM Ad a LEFT JOIN FETCH a.images LEFT JOIN FETCH a.category LEFT JOIN FETCH a.user WHERE a.id = :id")
    Optional<Ad> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT COUNT(a) FROM Ad a WHERE a.user.id = :userId AND a.status <> 'REMOVED'")
    long countByUserId(@Param("userId") Long userId);

    // подсчёт для модерации
    long countByStatus(Ad.Status status);

    // для matching (wanted) — только PUBLISHED
    @Query("SELECT a FROM Ad a WHERE a.status = 'PUBLISHED' AND a.category.id = :categoryId")
    List<Ad> findPublishedByCategory(@Param("categoryId") Long categoryId);

    // полнотекстовый поиск через PostgreSQL FTS
    @Query(value = "SELECT * FROM ads WHERE status = 'PUBLISHED' " +
            "AND search_vector @@ plainto_tsquery('russian', :query) " +
            "ORDER BY ts_rank(search_vector, plainto_tsquery('russian', :query)) DESC",
            countQuery = "SELECT count(*) FROM ads WHERE status = 'PUBLISHED' " +
                    "AND search_vector @@ plainto_tsquery('russian', :query)",
            nativeQuery = true)
    Page<Ad> fullTextSearch(@Param("query") String query, Pageable pageable);

    // аналитика для admin dashboard
    @Query("SELECT a.status, COUNT(a) FROM Ad a GROUP BY a.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT a.category.name, COUNT(a) FROM Ad a WHERE a.status = 'PUBLISHED' GROUP BY a.category.name ORDER BY COUNT(a) DESC")
    List<Object[]> topCategories();

    @Query("SELECT CAST(a.createdAt AS date), COUNT(a) FROM Ad a WHERE a.createdAt >= :since GROUP BY CAST(a.createdAt AS date) ORDER BY CAST(a.createdAt AS date)")
    List<Object[]> dailyAdCounts(@Param("since") java.time.LocalDateTime since);
}
