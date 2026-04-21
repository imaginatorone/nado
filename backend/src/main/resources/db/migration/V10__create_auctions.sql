-- модуль аукционов
CREATE TABLE auctions (
    id            BIGSERIAL       PRIMARY KEY,
    ad_id         BIGINT          NOT NULL UNIQUE REFERENCES ads(id),
    start_price   DECIMAL(15, 2)  NOT NULL,
    current_price DECIMAL(15, 2),
    min_step      DECIMAL(15, 2)  NOT NULL DEFAULT 100,
    ends_at       TIMESTAMP       NOT NULL,
    status        VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    winner_id     BIGINT          REFERENCES users(id),
    created_at    TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE bids (
    id          BIGSERIAL       PRIMARY KEY,
    auction_id  BIGINT          NOT NULL REFERENCES auctions(id),
    bidder_id   BIGINT          NOT NULL REFERENCES users(id),
    amount      DECIMAL(15, 2)  NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auctions_status ON auctions(status);
CREATE INDEX idx_auctions_ends ON auctions(ends_at);
CREATE INDEX idx_bids_auction ON bids(auction_id, created_at DESC);
