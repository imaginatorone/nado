package com.solarl.nado.repository;

import com.solarl.nado.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    Optional<Auction> findByAdId(Long adId);

    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND a.endsAt <= :now")
    List<Auction> findExpiredAuctions(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM Auction a JOIN FETCH a.ad WHERE a.status = 'ACTIVE' ORDER BY a.endsAt ASC")
    List<Auction> findActiveAuctions();

    // dashboard analytics
    long countByStatus(Auction.AuctionStatus status);
}
