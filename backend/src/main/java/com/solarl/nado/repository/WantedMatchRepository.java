package com.solarl.nado.repository;

import com.solarl.nado.entity.WantedMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WantedMatchRepository extends JpaRepository<WantedMatch, Long> {

    // проверка дедупликации
    boolean existsByRequestIdAndAdId(Long requestId, Long adId);

    // матчи для конкретного запроса, отсортированные по релевантности
    List<WantedMatch> findByRequestIdOrderByScoreDescCreatedAtDesc(Long requestId);

    // непросмотренные матчи пользователя
    List<WantedMatch> findByRequestUserIdAndSeenFalseOrderByCreatedAtDesc(Long userId);

    // общее кол-во непросмотренных
    long countByRequestUserIdAndSeenFalse(Long userId);
}
