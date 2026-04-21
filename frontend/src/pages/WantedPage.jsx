import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { wantToBuyAPI, categoriesAPI } from '../api/api';

export default function WantedPage() {
  const navigate = useNavigate();
  const [requests, setRequests] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [expandedId, setExpandedId] = useState(null);
  const [matches, setMatches] = useState({});

  // форма
  const [form, setForm] = useState({ query: '', categoryId: '', priceFrom: '', priceTo: '', region: '' });
  const [formError, setFormError] = useState('');
  const [formLoading, setFormLoading] = useState(false);

  const loadRequests = useCallback(() => {
    wantToBuyAPI.getMy()
      .then(res => setRequests(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadRequests();
    categoriesAPI.getAll().then(res => setCategories(res.data)).catch(() => {});
  }, [loadRequests]);

  const handleCreate = async (e) => {
    e.preventDefault();
    setFormError('');
    if (!form.query.trim()) { setFormError('Укажите что ищете'); return; }

    setFormLoading(true);
    try {
      const data = {
        query: form.query.trim(),
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        priceFrom: form.priceFrom ? Number(form.priceFrom) : null,
        priceTo: form.priceTo ? Number(form.priceTo) : null,
        region: form.region.trim() || null,
      };
      await wantToBuyAPI.create(data);
      setForm({ query: '', categoryId: '', priceFrom: '', priceTo: '', region: '' });
      setShowForm(false);
      loadRequests();
    } catch (err) {
      setFormError(err.response?.data?.message || 'Ошибка создания');
    }
    setFormLoading(false);
  };

  const handleDeactivate = async (id) => {
    try { await wantToBuyAPI.deactivate(id); loadRequests(); }
    catch (err) { alert(err.response?.data?.message || 'Ошибка'); }
  };

  const handleDelete = async (id) => {
    if (!confirm('Удалить запрос?')) return;
    try { await wantToBuyAPI.delete(id); loadRequests(); }
    catch (err) { alert(err.response?.data?.message || 'Ошибка'); }
  };

  const toggleMatches = async (id) => {
    if (expandedId === id) { setExpandedId(null); return; }
    setExpandedId(id);
    if (!matches[id]) {
      try {
        const res = await wantToBuyAPI.getMatches(id);
        setMatches(prev => ({ ...prev, [id]: res.data }));
        await wantToBuyAPI.markSeen(id);
      } catch { /* пусто */ }
    }
  };

  const formatPrice = (p) => p ? Number(p).toLocaleString('ru-RU') + ' ₽' : '';

  if (loading) return (
    <div className="container" style={{ paddingTop: 40, textAlign: 'center' }}>
      <div className="spinner"></div>
    </div>
  );

  return (
    <div className="container" style={{ paddingTop: 32, paddingBottom: 40 }}>
      <div className="wanted-header">
        <h1>🔍 Хочу купить</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Отмена' : '+ Новый запрос'}
        </button>
      </div>

      {showForm && (
        <form className="wanted-form" onSubmit={handleCreate}>
          <div className="form-group">
            <label className="form-label">Что ищете?</label>
            <input className="form-control" placeholder="iPhone 15, велосипед, квартира..."
              value={form.query} onChange={e => setForm({ ...form, query: e.target.value })} />
          </div>
          <div className="wanted-form-row">
            <div className="form-group" style={{ flex: 1 }}>
              <label className="form-label">Категория</label>
              <select className="form-control" value={form.categoryId}
                onChange={e => setForm({ ...form, categoryId: e.target.value })}>
                <option value="">Любая</option>
                {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
            <div className="form-group" style={{ flex: 1 }}>
              <label className="form-label">Регион</label>
              <input className="form-control" placeholder="Москва"
                value={form.region} onChange={e => setForm({ ...form, region: e.target.value })} />
            </div>
          </div>
          <div className="wanted-form-row">
            <div className="form-group" style={{ flex: 1 }}>
              <label className="form-label">Цена от</label>
              <input className="form-control" type="number" placeholder="0"
                value={form.priceFrom} onChange={e => setForm({ ...form, priceFrom: e.target.value })} />
            </div>
            <div className="form-group" style={{ flex: 1 }}>
              <label className="form-label">Цена до</label>
              <input className="form-control" type="number" placeholder="∞"
                value={form.priceTo} onChange={e => setForm({ ...form, priceTo: e.target.value })} />
            </div>
          </div>
          {formError && <div className="form-error">{formError}</div>}
          <button className="btn btn-primary" type="submit" disabled={formLoading}>
            {formLoading ? '⏳' : 'Создать запрос'}
          </button>
        </form>
      )}

      {requests.length === 0 && !showForm && (
        <div className="empty-state" style={{ marginTop: 40 }}>
          <div className="empty-state-icon">🔍</div>
          <h3>Нет запросов</h3>
          <p className="form-subtitle">Создайте запрос — мы уведомим, когда появится подходящее объявление</p>
        </div>
      )}

      <div className="wanted-list">
        {requests.map(req => (
          <div key={req.id} className={`wanted-card ${!req.active ? 'inactive' : ''}`}>
            <div className="wanted-card-top">
              <div>
                <div className="wanted-card-query">{req.query}</div>
                <div className="wanted-card-meta">
                  {req.categoryName && <span>📂 {req.categoryName}</span>}
                  {req.region && <span>📍 {req.region}</span>}
                  {(req.priceFrom || req.priceTo) && (
                    <span>💰 {formatPrice(req.priceFrom)} — {formatPrice(req.priceTo) || '∞'}</span>
                  )}
                </div>
              </div>
              <div className="wanted-card-badges">
                <span className={`badge ${req.active ? 'badge-success' : 'badge-muted'}`}>
                  {req.active ? 'Активен' : 'Остановлен'}
                </span>
                {req.matchCount > 0 && (
                  <span className="badge badge-info">{req.matchCount} совпад.</span>
                )}
              </div>
            </div>

            <div className="wanted-card-actions">
              {req.matchCount > 0 && (
                <button className="btn btn-sm btn-ghost" onClick={() => toggleMatches(req.id)}>
                  {expandedId === req.id ? 'Скрыть совпадения' : `Показать совпадения (${req.matchCount})`}
                </button>
              )}
              {req.active && (
                <button className="btn btn-sm btn-ghost" onClick={() => handleDeactivate(req.id)}>
                  Остановить
                </button>
              )}
              <button className="btn btn-sm btn-ghost" onClick={() => handleDelete(req.id)}>
                Удалить
              </button>
            </div>

            {expandedId === req.id && matches[req.id] && (
              <div className="wanted-matches">
                {matches[req.id].length === 0 ? (
                  <div className="wanted-match-empty">Совпадений пока нет</div>
                ) : (
                  matches[req.id].map(m => (
                    <div key={m.matchId} className={`wanted-match-item ${!m.seen ? 'unseen' : ''}`}
                         onClick={() => navigate(`/ads/${m.adId}`)}
                         style={{ cursor: 'pointer' }}>
                      <div className="wanted-match-title">{m.adTitle}</div>
                      <div className="wanted-match-info">
                        <span className="wanted-match-price">{formatPrice(m.adPrice)}</span>
                        {m.adRegion && <span>📍 {m.adRegion}</span>}
                        {m.adSaleType === 'AUCTION' && <span className="badge badge-warning" style={{fontSize:'0.68rem'}}>🔨</span>}
                        <span className="wanted-match-score">⭐ {m.score}</span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
