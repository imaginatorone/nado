package com.solarl.nado.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.solarl.nado.entity.Image;
import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByAdIdOrderBySortOrder(Long adId);
    int countByAdId(Long adId);
}
