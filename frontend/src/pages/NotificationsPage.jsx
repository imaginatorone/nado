import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { notificationsAPI } from '../api/api';

const TYPE_CONFIG = {
  AD_APPROVED:           { icon: '✅', label: 'Объявление одобрено',     color: 'var(--success)' },
  AD_REJECTED:           { icon: '❌', label: 'Объявление отклонено',    color: 'var(--error)' },
  AD_BLOCKED:            { icon: '🚫', label: 'Объявление заблокировано', color: 'var(--error)' },
  AUCTION_OUTBID:        { icon: '⚡', label: 'Вашу ставку перебили',    color: 'var(--warning)' },
  AUCTION_WON:           { icon: '🏆', label: 'Вы выиграли аукцион!',    color: 'var(--success)' },
  AUCTION_FINISHED_SELLER: { icon: '🔨', label: 'Аукцион завершён',      color: 'var(--info)' },
  AUCTION_NO_BIDS:       { icon: '😔', label: 'Аукцион без ставок',      color: 'var(--text-muted)' },
  WANTED_MATCH:          { icon: '🔍', label: 'Найдено совпадение',      color: 'var(--primary)' },
  NEW_MESSAGE:           { icon: '💬', label: 'Новое сообщение',         color: 'var(--info)' },
  NEW_RATING:            { icon: '⭐', label: 'Новый отзыв',             color: 'var(--warning)' },
  SYSTEM:                { icon: '📢', label: 'Системное',               color: 'var(--text-muted)' },
};

function parsePayload(str) {
  try { return JSON.parse(str); } catch { return {}; }
}

function formatTime(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const now = new Date();
  const diff = now - d;
  if (diff < 60000) return 'только что';
  if (diff < 3600000) return `${Math.floor(diff / 60000)} мин назад`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} ч назад`;
  return d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' });
}

export default function NotificationsPage() {
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  const loadNotifications = useCallback((p = 0) => {
    setLoading(true);
    notificationsAPI.getList(p, 20)
      .then(res => {
        if (p === 0) setNotifications(res.data);
        else setNotifications(prev => [...prev, ...res.data]);
        setHasMore(res.data.length === 20);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { loadNotifications(0); }, [loadNotifications]);

  const handleMarkAllRead = async () => {
    try {
      await notificationsAPI.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    } catch {}
  };

  const handleClick = async (notif) => {
    if (!notif.isRead) {
      try {
        await notificationsAPI.markAsRead(notif.id);
        setNotifications(prev =>
          prev.map(n => n.id === notif.id ? { ...n, isRead: true } : n));
      } catch {}
    }
    // навигация по типу
    const payload = parsePayload(notif.payload);
    if (payload.adId) navigate(`/ads/${payload.adId}`);
    else if (payload.requestId) navigate('/wanted');
  };

  const loadMore = () => {
    const next = page + 1;
    setPage(next);
    loadNotifications(next);
  };

  const unreadCount = notifications.filter(n => !n.isRead).length;

  return (
    <div className="container" style={{ paddingTop: 32, paddingBottom: 40 }}>
      <div className="notif-header">
        <h1>🔔 Уведомления</h1>
        {unreadCount > 0 && (
          <button className="btn btn-sm btn-ghost" onClick={handleMarkAllRead}>
            Прочитать все ({unreadCount})
          </button>
        )}
      </div>

      {notifications.length === 0 && !loading && (
        <div className="empty-state" style={{ marginTop: 40 }}>
          <div className="empty-state-icon">🔔</div>
          <h3>Нет уведомлений</h3>
          <p className="form-subtitle">Здесь появятся уведомления о ваших объявлениях, аукционах и поисковых запросах</p>
        </div>
      )}

      <div className="notif-list">
        {notifications.map(notif => {
          const config = TYPE_CONFIG[notif.type] || TYPE_CONFIG.SYSTEM;
          const payload = parsePayload(notif.payload);
          return (
            <div key={notif.id}
                 className={`notif-item ${!notif.isRead ? 'unread' : ''}`}
                 onClick={() => handleClick(notif)}>
              <div className="notif-icon" style={{ color: config.color }}>{config.icon}</div>
              <div className="notif-body">
                <div className="notif-label">{config.label}</div>
                {payload.adTitle && <div className="notif-title">{payload.adTitle}</div>}
                {payload.reason && <div className="notif-reason">Причина: {payload.reason}</div>}
                {payload.finalPrice && <div className="notif-price">{Number(payload.finalPrice).toLocaleString('ru-RU')} ₽</div>}
                {payload.winnerName && <div className="notif-detail">Покупатель: {payload.winnerName}</div>}
                {payload.senderName && <div className="notif-detail">От: {payload.senderName}</div>}
              </div>
              <div className="notif-time">{formatTime(notif.createdAt)}</div>
            </div>
          );
        })}
      </div>

      {hasMore && notifications.length > 0 && (
        <div style={{ textAlign: 'center', marginTop: 16 }}>
          <button className="btn btn-ghost" onClick={loadMore} disabled={loading}>
            {loading ? '⏳' : 'Загрузить ещё'}
          </button>
        </div>
      )}

      {loading && notifications.length === 0 && (
        <div style={{ textAlign: 'center', paddingTop: 40 }}><div className="spinner"></div></div>
      )}
    </div>
  );
}
