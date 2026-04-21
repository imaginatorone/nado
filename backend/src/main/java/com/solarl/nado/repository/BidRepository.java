package com.solarl.nado.repository;

import com.solarl.nado.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId);

    Optional<Bid> findFirstByAuctionIdOrderByAmountDesc(Long auctionId);

    long countByAuctionId(Long auctionId);
}
