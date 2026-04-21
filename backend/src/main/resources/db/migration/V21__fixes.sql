ALTER TABLE auctions ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS dedup_key VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_notifications_dedup ON notifications(user_id, type, dedup_key);
