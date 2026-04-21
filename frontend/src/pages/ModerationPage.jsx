import { useState, useEffect, useCallback } from 'react';
import { motion } from 'framer-motion';
import { moderationAPI } from '../api/api';

export default function ModerationPage() {
  const [ads, setAds] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(null);
  const [rejectId, setRejectId] = useState(null);
  const [rejectReason, setRejectReason] = useState('');
  const [blockId, setBlockId] = useState(null);
  const [blockReason, setBlockReason] = useState('');

  const loadAds = useCallback(() => {
    setLoading(true);
    moderationAPI.getPending(page, 20)
      .then(res => {
        setAds(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(() => { loadAds(); }, [loadAds]);

  const handleApprove = async (adId) => {
    setActionLoading(adId);
    try {
      await moderationAPI.approve(adId);
      setAds(prev => prev.filter(a => a.id !== adId));
      setTotalElements(prev => prev - 1);
    } catch {}
    setActionLoading(null);
  };

  const handleReject = async () => {
    if (!rejectReason.trim()) return;
    setActionLoading(rejectId);
    try {
      await moderationAPI.reject(rejectId, rejectReason);
      setAds(prev => prev.filter(a => a.id !== rejectId));
      setTotalElements(prev => prev - 1);
    } catch {}
    setRejectId(null);
    setRejectReason('');
    setActionLoading(null);
  };

  const handleBlock = async () => {
    setActionLoading(blockId);
    try {
      await moderationAPI.block(blockId, blockReason);
      setAds(prev => prev.filter(a => a.id !== blockId));
      setTotalElements(prev => prev - 1);
    } catch {}
    setBlockId(null);
    setBlockReason('');
    setActionLoading(null);
  };

  const formatDate = (d) => {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('ru-RU', {
      day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
    });
  };

  const formatPrice = (p) => {
    if (!p) return '—';
    return Number(p).toLocaleString('ru-RU') + ' ₽';
  };

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.3 }}>
      <div className="page-header">
        <h1>Модерация</h1>
        <span className="badge badge-warning" style={{ fontSize: '1rem', padding: '6px 16px' }}>
          Ожидают: {totalElements}
        </span>
      </div>

      {loading ? (
        <div className="loading"><div className="spinner"></div></div>
      ) : ads.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon" style={{ fontSize: '3rem' }}>✅</div>
          <h3>Очередь пуста</h3>
          <p>Все объявления проверены</p>
        </div>
      ) : (
        <div className="moderation-list">
          {ads.map(ad => (
            <div key={ad.id} className="moderation-card">
              <div className="moderation-card-header">
                <div>
                  <h3 className="moderation-card-title">{ad.title}</h3>
                  <div className="moderation-card-meta">
                    {ad.userName} · {ad.userEmail} · {formatDate(ad.submittedAt)}
                  </div>
                </div>
                <div className="moderation-card-price">{formatPrice(ad.price)}</div>
              </div>

              <div className="moderation-card-desc">{ad.description}</div>

              {ad.categoryName && (
                <div className="moderation-card-cat">📂 {ad.categoryName}</div>
              )}

              <div className="moderation-card-actions">
                <button
                  className="btn btn-sm btn-success"
                  disabled={actionLoading === ad.id}
                  onClick={() => handleApprove(ad.id)}
                >
                  ✓ Одобрить
                </button>
                <button
                  className="btn btn-sm btn-warning"
                  disabled={actionLoading === ad.id}
                  onClick={() => { setRejectId(ad.id); setRejectReason(''); }}
                >
                  ✗ Отклонить
                </button>
                <button
                  className="btn btn-sm btn-error"
                  disabled={actionLoading === ad.id}
                  onClick={() => { setBlockId(ad.id); setBlockReason(''); }}
                >
                  🚫 Заблокировать
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* диалог отклонения */}
      {rejectId && (
        <div className="modal-overlay" onClick={() => setRejectId(null)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <h3>Отклонить объявление</h3>
            <p className="form-subtitle">Укажите причину — пользователь увидит её в кабинете</p>
            <textarea
              className="form-control"
              rows={3}
              placeholder="Причина отклонения..."
              value={rejectReason}
              onChange={e => setRejectReason(e.target.value)}
              autoFocus
            />
            <div className="modal-actions">
              <button className="btn btn-ghost" onClick={() => setRejectId(null)}>Отмена</button>
              <button
                className="btn btn-warning"
                disabled={!rejectReason.trim()}
                onClick={handleReject}
              >
                Отклонить
              </button>
            </div>
          </div>
        </div>
      )}

      {/* диалог блокировки */}
      {blockId && (
        <div className="modal-overlay" onClick={() => setBlockId(null)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <h3>Заблокировать объявление</h3>
            <p className="form-subtitle">Объявление будет скрыто и не может быть переотправлено</p>
            <textarea
              className="form-control"
              rows={3}
              placeholder="Причина блокировки (необязательно)..."
              value={blockReason}
              onChange={e => setBlockReason(e.target.value)}
              autoFocus
            />
            <div className="modal-actions">
              <button className="btn btn-ghost" onClick={() => setBlockId(null)}>Отмена</button>
              <button className="btn btn-error" onClick={handleBlock}>Заблокировать</button>
            </div>
          </div>
        </div>
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Назад</button>
          <span style={{ padding: '8px 12px', color: 'var(--text-muted)' }}>
            {page + 1} / {totalPages}
          </span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Далее →</button>
        </div>
      )}
    </motion.div>
  );
}
