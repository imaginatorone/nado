-- «Хочу купить» — сохранённые поисковые запросы
CREATE TABLE want_to_buy (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id),
    query           VARCHAR(255)    NOT NULL,
    category_id     BIGINT          REFERENCES categories(id),
    price_from      DECIMAL(15, 2),
    price_to        DECIMAL(15, 2),
    region          VARCHAR(100),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    match_count     INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    last_matched_at TIMESTAMP
);

CREATE INDEX idx_wtb_user ON want_to_buy(user_id);
CREATE INDEX idx_wtb_active ON want_to_buy(active) WHERE active = TRUE;

-- Расширяем таблицу users полями для рейтинга доверия
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS region VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS completed_deals INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS complaints INT NOT NULL DEFAULT 0;
