package com.solarl.nado.repository;

import com.solarl.nado.entity.AuctionOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuctionOutcomeRepository extends JpaRepository<AuctionOutcome, Long> {

    Optional<AuctionOutcome> findByAuctionId(Long auctionId);
}
