package com.solarl.nado.repository;

import com.solarl.nado.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    Optional<Rating> findByReviewerIdAndSellerId(Long reviewerId, Long sellerId);

    @Query("SELECT COALESCE(AVG(r.score), 0) FROM Rating r WHERE r.seller.id = :sellerId")
    double getAverageScoreBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.seller.id = :sellerId")
    long countBySellerId(@Param("sellerId") Long sellerId);
}
