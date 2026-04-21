-- Phase 5: in-app notifications
CREATE TABLE notifications (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(50)     NOT NULL,
    payload     TEXT            NOT NULL DEFAULT '{}',
    is_read     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_created ON notifications(user_id, created_at DESC);

-- дедупликация: не спамить одинаковым уведомлением
-- (type + userId + payload hash — проверяется на уровне сервиса)
