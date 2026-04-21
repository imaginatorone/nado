import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { adsAPI } from '../api/api';

const STATUS_TABS = [
  { key: null,                  label: 'Все' },
  { key: 'PENDING_MODERATION',  label: 'На проверке' },
  { key: 'PUBLISHED',           label: 'Опубликованы' },
  { key: 'REJECTED',            label: 'Отклонены' },
  { key: 'SOLD',                label: 'Проданы' },
  { key: 'ARCHIVED',            label: 'Архив' },
  { key: 'DRAFT',               label: 'Черновики' },
];

const STATUS_LABELS = {
  DRAFT:               { text: 'Черновик',     cls: 'badge-muted' },
  PENDING_MODERATION:  { text: 'На проверке',  cls: 'badge-warning' },
  PUBLISHED:           { text: 'Опубликовано', cls: 'badge-success' },
  REJECTED:            { text: 'Отклонено',    cls: 'badge-error' },
  SOLD:                { text: 'Продано',      cls: 'badge-info' },
  ARCHIVED:            { text: 'В архиве',     cls: 'badge-muted' },
  BLOCKED:             { text: 'Заблокировано',cls: 'badge-error' },
  REMOVED:             { text: 'Удалено',      cls: 'badge-muted' },
};

export default function MyAdsPage() {
  const [ads, setAds] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState(null);
  const navigate = useNavigate();

  const loadAds = useCallback(() => {
    setLoading(true);
    adsAPI.getMyCabinet(page, 12, activeTab)
      .then(res => {
        setAds(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [page, activeTab]);

  useEffect(() => { loadAds(); }, [loadAds]);

  const handleTabChange = (key) => {
    setActiveTab(key);
    setPage(0);
  };

  const handleDelete = async (id) => {
    if (!confirm('Удалить объявление?')) return;
    try {
      await adsAPI.delete(id);
      loadAds();
    } catch {}
  };

  const formatDate = (d) => {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('ru-RU', { day: 'numeric', month: 'short', year: 'numeric' });
  };

  const formatPrice = (p) => {
    if (!p) return 'Цена не указана';
    return Number(p).toLocaleString('ru-RU') + ' ₽';
  };

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.3 }}>
      <div className="page-header">
        <h1>Мои объявления</h1>
        <Link to="/ads/new" className="btn btn-primary">+ Подать объявление</Link>
      </div>

      {/* статусные табы — скроллятся горизонтально на mobile */}
      <div className="cabinet-tabs">
        {STATUS_TABS.map(tab => (
          <button
            key={tab.key || 'all'}
            className={`cabinet-tab ${activeTab === tab.key ? 'active' : ''}`}
            onClick={() => handleTabChange(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="loading"><div className="spinner"></div></div>
      ) : ads.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" strokeWidth="1.5">
              <path d="M16.5 9.4l-9-5.19M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/>
              <path d="M3.27 6.96L12 12.01l8.73-5.05M12 22.08V12"/>
            </svg>
          </div>
          <h3>{activeTab ? 'Нет объявлений с таким статусом' : 'Пока пусто — но вы же знаете, что кому-то это надо!'}</h3>
          <p>Создайте первое объявление и начните продавать</p>
          <Link to="/ads/new" className="btn btn-primary" style={{ marginTop: '16px' }}>+ Подать объявление</Link>
        </div>
      ) : (
        <>
          <p className="cabinet-count">Найдено: {totalElements}</p>
          <div className="cabinet-list">
            {ads.map(ad => {
              const status = STATUS_LABELS[ad.status] || { text: ad.status, cls: 'badge-muted' };
              return (
                <div key={ad.id} className="cabinet-card">
                  <div className="cabinet-card-img" onClick={() => navigate(`/ads/${ad.id}`)}>
                    {ad.imageUrl ? (
                      <img src={`/api${ad.imageUrl}`} alt={ad.title} loading="lazy" />
                    ) : (
                      <div className="cabinet-card-noimg">📷</div>
                    )}
                  </div>
                  <div className="cabinet-card-body">
                    <div className="cabinet-card-top">
                      <span className={`badge ${status.cls}`}>{status.text}</span>
                      <span className="cabinet-card-date">{formatDate(ad.createdAt)}</span>
                    </div>
                    <h3 className="cabinet-card-title" onClick={() => navigate(`/ads/${ad.id}`)}>{ad.title}</h3>
                    <div className="cabinet-card-price">{formatPrice(ad.price)}</div>
                    {ad.viewCount > 0 && <div className="cabinet-card-views">👁 {ad.viewCount}</div>}

                    {/* причина отклонения — видна владельцу */}
                    {(ad.status === 'REJECTED' || ad.status === 'BLOCKED') && ad.rejectionReason && (
                      <div className="cabinet-rejection">
                        <strong>{ad.status === 'BLOCKED' ? '🚫 Заблокировано:' : '⚠️ Причина отклонения:'}</strong>
                        <p>{ad.rejectionReason}</p>
                      </div>
                    )}

                    <div className="cabinet-card-actions">
                      {/* REJECTED → редактировать и отправить снова */}
                      {ad.status === 'REJECTED' && (
                        <Link to={`/ads/${ad.id}/edit`} className="btn btn-sm btn-primary">✏️ Исправить и отправить</Link>
                      )}
                      {ad.status === 'PUBLISHED' && (
                        <Link to={`/ads/${ad.id}/edit`} className="btn btn-sm btn-outline">✏️ Редактировать</Link>
                      )}
                      {ad.status === 'DRAFT' && (
                        <Link to={`/ads/${ad.id}/edit`} className="btn btn-sm btn-primary">✏️ Редактировать</Link>
                      )}
                      {(ad.status !== 'REMOVED' && ad.status !== 'BLOCKED') && (
                        <button className="btn btn-sm btn-ghost" onClick={() => handleDelete(ad.id)}>🗑️</button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </>
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Назад</button>
          {Array.from({ length: totalPages }, (_, i) => (
            <button key={i} className={i === page ? 'active' : ''} onClick={() => setPage(i)}>{i + 1}</button>
          ))}
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Далее →</button>
        </div>
      )}
    </motion.div>
  );
}
