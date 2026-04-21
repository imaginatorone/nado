-- аукционы: расширение доменной модели для Phase 3

-- категории: разрешение аукционов
ALTER TABLE categories ADD COLUMN IF NOT EXISTS auction_allowed BOOLEAN NOT NULL DEFAULT TRUE;

-- аукционы: расширение статусов и полей
ALTER TABLE auctions ALTER COLUMN status TYPE VARCHAR(30);
ALTER TABLE auctions ADD COLUMN IF NOT EXISTS bid_extension_minutes INT NOT NULL DEFAULT 5;
ALTER TABLE auctions ADD COLUMN IF NOT EXISTS last_bid_at TIMESTAMP;
ALTER TABLE auctions ADD COLUMN IF NOT EXISTS final_price DECIMAL(15,2);
ALTER TABLE auctions ADD COLUMN IF NOT EXISTS bid_count INT NOT NULL DEFAULT 0;

-- обратная совместимость: ACTIVE → ACTIVE (без изменений)
-- новые статусы: AWAITING_PAYMENT, NO_BIDS, FAILED — будут записаны scheduled job'ом

-- пост-аукционная сделка
CREATE TABLE auction_outcomes (
    id          BIGSERIAL PRIMARY KEY,
    auction_id  BIGINT NOT NULL REFERENCES auctions(id) ON DELETE CASCADE,
    buyer_id    BIGINT NOT NULL REFERENCES users(id),
    seller_id   BIGINT NOT NULL REFERENCES users(id),
    final_price DECIMAL(15,2) NOT NULL,
    status      VARCHAR(30) NOT NULL DEFAULT 'PENDING_CONTACT',
    notes       TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_auction_outcome UNIQUE (auction_id)
);

CREATE INDEX idx_auction_outcomes_buyer ON auction_outcomes(buyer_id);
CREATE INDEX idx_auction_outcomes_seller ON auction_outcomes(seller_id);
