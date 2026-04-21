-- V17: расширение lifecycle объявлений
-- status column расширяется: ACTIVE → DRAFT/PENDING_MODERATION/PUBLISHED/REJECTED/ARCHIVED/REMOVED
-- ACTIVE маппится на PUBLISHED для обратной совместимости

-- расширяем колонку status (VARCHAR(20) → VARCHAR(30))
ALTER TABLE ads ALTER COLUMN status TYPE VARCHAR(30);

-- lifecycle-поля
ALTER TABLE ads ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE ads ADD COLUMN IF NOT EXISTS moderated_by BIGINT REFERENCES users(id);
ALTER TABLE ads ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP;
ALTER TABLE ads ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;
ALTER TABLE ads ADD COLUMN IF NOT EXISTS moderated_at TIMESTAMP;

-- sale mode
ALTER TABLE ads ADD COLUMN IF NOT EXISTS sale_type VARCHAR(20) NOT NULL DEFAULT 'FIXED_PRICE';

-- миграция существующих данных: ACTIVE → PUBLISHED
UPDATE ads SET status = 'PUBLISHED', published_at = created_at WHERE status = 'ACTIVE';
UPDATE ads SET status = 'REMOVED' WHERE status = 'DELETED';

CREATE INDEX IF NOT EXISTS idx_ads_user_status ON ads(user_id, status);
