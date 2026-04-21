package com.solarl.nado.repository;

import com.solarl.nado.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Page<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Favorite> findByUserIdAndAdId(Long userId, Long adId);

    boolean existsByUserIdAndAdId(Long userId, Long adId);

    long countByAdId(Long adId);
}
