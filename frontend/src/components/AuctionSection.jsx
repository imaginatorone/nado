import { useState, useEffect, useCallback, useRef } from 'react';
import { auctionsAPI } from '../api/api';
import { useAuth } from '../context/AuthContext';

// таймер + текущая ставка + bid form + история ставок
export default function AuctionSection({ adId, adOwnerId }) {
  const { user, isAuthenticated } = useAuth();
  const [auction, setAuction] = useState(null);
  const [loading, setLoading] = useState(true);
  const [bidAmount, setBidAmount] = useState('');
  const [bidError, setBidError] = useState('');
  const [bidLoading, setBidLoading] = useState(false);
  const [timeLeft, setTimeLeft] = useState('');
  const timerRef = useRef(null);

  const loadAuction = useCallback(() => {
    auctionsAPI.getByAdId(adId)
      .then(res => setAuction(res.data))
      .catch(() => setAuction(null))
      .finally(() => setLoading(false));
  }, [adId]);

  useEffect(() => { loadAuction(); }, [loadAuction]);

  // обратный отсчёт
  useEffect(() => {
    if (!auction || auction.status !== 'ACTIVE') return;
    const tick = () => {
      const end = new Date(auction.endsAt).getTime();
      const now = Date.now();
      const diff = end - now;
      if (diff <= 0) {
        setTimeLeft('Завершён');
        clearInterval(timerRef.current);
        loadAuction();
        return;
      }
      const d = Math.floor(diff / 86400000);
      const h = Math.floor((diff % 86400000) / 3600000);
      const m = Math.floor((diff % 3600000) / 60000);
      const s = Math.floor((diff % 60000) / 1000);
      setTimeLeft(d > 0 ? `${d}д ${h}ч ${m}м` : `${h}ч ${m}м ${s}с`);
    };
    tick();
    timerRef.current = setInterval(tick, 1000);
    return () => clearInterval(timerRef.current);
  }, [auction, loadAuction]);

  const handleBid = async (e) => {
    e.preventDefault();
    setBidError('');
    const amount = parseFloat(bidAmount);
    if (!amount || amount <= 0) { setBidError('Введите сумму'); return; }

    setBidLoading(true);
    try {
      await auctionsAPI.placeBid(auction.id, { amount });
      setBidAmount('');
      loadAuction();
    } catch (err) {
      setBidError(err.response?.data?.message || 'Ошибка ставки');
    }
    setBidLoading(false);
  };

  const handleCancel = async () => {
    if (!confirm('Отменить аукцион?')) return;
    try {
      await auctionsAPI.cancel(auction.id);
      loadAuction();
    } catch (err) {
      alert(err.response?.data?.message || 'Ошибка отмены');
    }
  };

  if (loading) return <div className="auction-loading"><div className="spinner"></div></div>;
  if (!auction) return null;

  const isOwner = user?.id === adOwnerId;
  const isActive = auction.status === 'ACTIVE';
  const minBid = (parseFloat(auction.currentPrice) + parseFloat(auction.minStep)).toFixed(2);
  const formatPrice = (p) => p ? Number(p).toLocaleString('ru-RU') + ' ₽' : '—';

  const STATUS_MAP = {
    ACTIVE:    { text: 'Идёт торг',    cls: 'badge-success' },
    FINISHED:  { text: 'Завершён',     cls: 'badge-info' },
    NO_BIDS:   { text: 'Без ставок',   cls: 'badge-warning' },
    CANCELLED: { text: 'Отменён',      cls: 'badge-muted' },
  };

  const statusInfo = STATUS_MAP[auction.status] || { text: auction.status, cls: 'badge-muted' };

  return (
    <div className="auction-section">
      <div className="auction-header">
        <h3>🔨 Аукцион</h3>
        <span className={`badge ${statusInfo.cls}`}>{statusInfo.text}</span>
      </div>

      <div className="auction-stats">
        <div className="auction-stat">
          <span className="auction-stat-label">Текущая ставка</span>
          <span className="auction-stat-value auction-price">{formatPrice(auction.currentPrice)}</span>
        </div>
        <div className="auction-stat">
          <span className="auction-stat-label">Стартовая</span>
          <span className="auction-stat-value">{formatPrice(auction.startPrice)}</span>
        </div>
        <div className="auction-stat">
          <span className="auction-stat-label">Шаг</span>
          <span className="auction-stat-value">{formatPrice(auction.minStep)}</span>
        </div>
        <div className="auction-stat">
          <span className="auction-stat-label">Ставок</span>
          <span className="auction-stat-value">{auction.bidCount}</span>
        </div>
        {isActive && (
          <div className="auction-stat auction-timer">
            <span className="auction-stat-label">Осталось</span>
            <span className="auction-stat-value auction-countdown">{timeLeft}</span>
          </div>
        )}
      </div>

      {auction.winnerId && (
        <div className="auction-leader">
          🏆 Лидер: <strong>{auction.winnerName}</strong>
          {user?.id === auction.winnerId && <span className="badge badge-success" style={{marginLeft: 8}}>Это вы!</span>}
        </div>
      )}

      {/* форма ставки — для авторизованных, не-владельцев, при ACTIVE */}
      {isActive && isAuthenticated && !isOwner && (
        <form className="auction-bid-form" onSubmit={handleBid}>
          <div className="auction-bid-row">
            <input
              type="number"
              className="form-control"
              placeholder={`Мин. ${minBid}`}
              value={bidAmount}
              onChange={e => setBidAmount(e.target.value)}
              min={minBid}
              step="0.01"
            />
            <button className="btn btn-primary" disabled={bidLoading} type="submit">
              {bidLoading ? '⏳' : 'Сделать ставку'}
            </button>
          </div>
          {bidError && <div className="form-error">{bidError}</div>}
        </form>
      )}

      {/* owner actions */}
      {isOwner && isActive && auction.bidCount === 0 && (
        <div className="auction-owner-actions">
          <button className="btn btn-sm btn-ghost" onClick={handleCancel}>Отменить аукцион</button>
        </div>
      )}

      {/* история ставок */}
      {auction.recentBids && auction.recentBids.length > 0 && (
        <div className="auction-bids">
          <h4>Последние ставки</h4>
          <div className="auction-bids-list">
            {auction.recentBids.map((bid, i) => (
              <div key={bid.id} className={`auction-bid-item ${i === 0 ? 'leader' : ''}`}>
                <span className="auction-bid-name">{bid.bidderName}</span>
                <span className="auction-bid-amount">{formatPrice(bid.amount)}</span>
                <span className="auction-bid-time">
                  {new Date(bid.createdAt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
