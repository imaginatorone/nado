package com.solarl.nado.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.solarl.nado.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByAdIdOrderByCreatedAtDesc(Long adId, Pageable pageable);
}
