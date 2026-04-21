import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { adsAPI, imagesAPI } from '../api/api';
import { useAuth } from '../context/AuthContext';
import PhoneReveal from '../components/PhoneReveal';
import AuctionSection from '../components/AuctionSection';
import { IconChat, IconCamera } from '../components/Icons';

function formatPrice(price) {
  if (!price) return 'Цена не указана';
  return new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 0 }).format(price);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' });
}

/* Lightbox: fullscreen photo viewer with arrows + swipe */
function Lightbox({ images, startIndex, onClose }) {
  const [idx, setIdx] = useState(startIndex);
  const touchRef = useRef(null);

  const prev = () => setIdx(i => (i > 0 ? i - 1 : images.length - 1));
  const next = () => setIdx(i => (i < images.length - 1 ? i + 1 : 0));

  useEffect(() => {
    const handleKey = (e) => {
      if (e.key === 'Escape') onClose();
      if (e.key === 'ArrowLeft') prev();
      if (e.key === 'ArrowRight') next();
    };
    window.addEventListener('keydown', handleKey);
    document.body.style.overflow = 'hidden';
    return () => {
      window.removeEventListener('keydown', handleKey);
      document.body.style.overflow = '';
    };
  }, [onClose]);

  const onTouchStart = (e) => { touchRef.current = e.touches[0].clientX; };
  const onTouchEnd = (e) => {
    if (touchRef.current === null) return;
    const diff = touchRef.current - e.changedTouches[0].clientX;
    if (Math.abs(diff) > 50) { diff > 0 ? next() : prev(); }
    touchRef.current = null;
  };

  return (
    <div className="lightbox-overlay" onClick={onClose}>
      <button className="lightbox-close" onClick={onClose}>×</button>
      {images.length > 1 && (
        <>
          <button className="lightbox-arrow lightbox-arrow-left" onClick={(e) => { e.stopPropagation(); prev(); }}>‹</button>
          <button className="lightbox-arrow lightbox-arrow-right" onClick={(e) => { e.stopPropagation(); next(); }}>›</button>
        </>
      )}
      <img
        className="lightbox-image"
        src={imagesAPI.getUrl(images[idx].id)}
        alt=""
        onClick={(e) => e.stopPropagation()}
        onTouchStart={onTouchStart}
        onTouchEnd={onTouchEnd}
        draggable={false}
      />
      {images.length > 1 && (
        <div className="lightbox-counter">{idx + 1} / {images.length}</div>
      )}
    </div>
  );
}

export default function AdDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const [ad, setAd] = useState(null);
  const [selectedImage, setSelectedImage] = useState(0);
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const touchRef = useRef(null);

  useEffect(() => {
    setLoading(true);
    adsAPI.getById(id)
      .then(res => setAd(res.data))
      .catch(() => setError('Объявление не найдено'))
      .finally(() => setLoading(false));
  }, [id]);

  const handleDelete = async () => {
    if (!window.confirm('Удалить это объявление?')) return;
    try {
      await adsAPI.delete(id);
      navigate('/');
    } catch {
      alert('Ошибка удаления');
    }
  };

  const prevImage = useCallback(() => {
    if (!ad?.images?.length) return;
    setSelectedImage(i => (i > 0 ? i - 1 : ad.images.length - 1));
  }, [ad]);

  const nextImage = useCallback(() => {
    if (!ad?.images?.length) return;
    setSelectedImage(i => (i < ad.images.length - 1 ? i + 1 : 0));
  }, [ad]);

  // swipe on gallery
  const onGalleryTouchStart = (e) => { touchRef.current = e.touches[0].clientX; };
  const onGalleryTouchEnd = (e) => {
    if (touchRef.current === null) return;
    const diff = touchRef.current - e.changedTouches[0].clientX;
    if (Math.abs(diff) > 50) { diff > 0 ? nextImage() : prevImage(); }
    else { setLightboxOpen(true); } // tap = open fullscreen
    touchRef.current = null;
  };

  if (loading) return <div className="loading"><div className="spinner"></div></div>;
  if (error) return <div className="empty-state"><h3>{error}</h3></div>;
  if (!ad) return null;

  const isOwner = user && user.id === ad.userId;
  const isAdmin = user && user.role === 'ADMIN';
  const hasImages = ad.images && ad.images.length > 0;
  const multipleImages = hasImages && ad.images.length > 1;
  const isClosed = ad.status === 'SOLD' || ad.status === 'ARCHIVED';
  const isPublished = ad.status === 'PUBLISHED';

  const handleClose = async (type) => {
    const labels = { sold: 'Пометить как продано?', archive: 'Снять с публикации?' };
    if (!window.confirm(labels[type])) return;
    try {
      if (type === 'sold') await adsAPI.markSold(ad.id);
      else await adsAPI.archive(ad.id);
      setAd(prev => ({ ...prev, status: type === 'sold' ? 'SOLD' : 'ARCHIVED' }));
    } catch {
      alert('Ошибка');
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      {lightboxOpen && hasImages && (
        <Lightbox
          images={ad.images}
          startIndex={selectedImage}
          onClose={() => setLightboxOpen(false)}
        />
      )}

      <div className="ad-detail">
        <div className="ad-detail-main">
          <div
            className="ad-detail-gallery"
            onTouchStart={onGalleryTouchStart}
            onTouchEnd={onGalleryTouchEnd}
          >
            {hasImages ? (
              <>
                <AnimatePresence mode="wait">
                  <motion.img
                    key={selectedImage}
                    src={imagesAPI.getUrl(ad.images[selectedImage].id)}
                    alt={ad.title}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.2 }}
                    onClick={() => setLightboxOpen(true)}
                    style={{ cursor: 'zoom-in' }}
                  />
                </AnimatePresence>
                {multipleImages && (
                  <>
                    <button className="gallery-arrow gallery-arrow-left" onClick={(e) => { e.stopPropagation(); prevImage(); }}>‹</button>
                    <button className="gallery-arrow gallery-arrow-right" onClick={(e) => { e.stopPropagation(); nextImage(); }}>›</button>
                    <div className="gallery-counter">{selectedImage + 1} / {ad.images.length}</div>
                  </>
                )}
              </>
            ) : (
              <IconCamera size={48} style={{color: 'var(--text-muted)'}} />
            )}
          </div>

          {hasImages && ad.images.length > 1 && (
            <div className="ad-detail-thumbs">
              {ad.images.map((img, i) => (
                <div
                  key={img.id}
                  className={`ad-detail-thumb ${i === selectedImage ? 'active' : ''}`}
                  onClick={() => setSelectedImage(i)}
                >
                  <img src={imagesAPI.getUrl(img.id)} alt="" />
                </div>
              ))}
            </div>
          )}

          <span className="ad-card-category">{ad.categoryName}</span>
          <h1>{ad.title}</h1>
          <div className="ad-detail-description">{ad.description}</div>
        </div>

        <div className="ad-detail-sidebar">
          <motion.div
            className="ad-detail-price-card"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.15 }}
          >
            {isClosed ? (
              <div className="ad-closed-badge">
                <span className={`badge ${ad.status === 'SOLD' ? 'badge-success' : 'badge-ghost'}`} style={{ fontSize: '1rem', padding: '8px 18px' }}>
                  {ad.status === 'SOLD' ? '✓ Продано' : 'Снято с публикации'}
                </span>
              </div>
            ) : (
              <div className="ad-detail-price">{formatPrice(ad.price)}</div>
            )}
            {ad.saleType === 'AUCTION' && !isClosed && (
              <div style={{ marginBottom: '12px' }}>
                <span className="badge badge-warning" style={{ fontSize: '0.78rem' }}>🔨 Аукцион</span>
              </div>
            )}
            <div style={{ fontSize: '0.813rem', color: 'var(--text-muted)', marginBottom: '16px' }}>
              Опубликовано {formatDate(ad.createdAt)}
            </div>

            {ad.saleType === 'AUCTION' && isPublished && (
              <AuctionSection adId={ad.id} adOwnerId={ad.userId} />
            )}

            {isAuthenticated && !isOwner && isPublished && (
              <Link
                to={`/chats?adId=${ad.id}`}
                className="btn btn-primary btn-lg"
                style={{ width: '100%', marginBottom: '12px' }}
              >
                <IconChat size={16} style={{marginRight: 6, verticalAlign: 'middle'}} /> Написать продавцу
              </Link>
            )}

            {!isAuthenticated && isPublished && (
              <Link
                to="/login"
                className="btn btn-primary btn-lg"
                style={{ width: '100%', marginBottom: '12px' }}
              >
                <IconChat size={16} style={{marginRight: 6, verticalAlign: 'middle'}} /> Войдите, чтобы написать
              </Link>
            )}

            {/* Закрыть объявление */}
            {isOwner && isPublished && (
              <div style={{ display: 'flex', gap: '8px', marginBottom: '10px' }}>
                <button onClick={() => handleClose('sold')} className="btn btn-success btn-sm" style={{ flex: 1 }}>
                  ✓ Продано
                </button>
                <button onClick={() => handleClose('archive')} className="btn btn-ghost btn-sm" style={{ flex: 1 }}>
                  Снять с публикации
                </button>
              </div>
            )}

            {(isOwner || isAdmin) && !isClosed && (
              <div style={{ display: 'flex', gap: '8px' }}>
                {isOwner && isPublished && (
                  <Link to={`/ads/${ad.id}/edit`} className="btn btn-outline btn-sm" style={{ flex: 1 }}>
                    Редактировать
                  </Link>
                )}
                <button onClick={handleDelete} className="btn btn-danger btn-sm" style={{ flex: 1 }}>
                  Удалить
                </button>
              </div>
            )}

            {/* Статистика — только для владельца */}
            {isOwner && (
              <div className="ad-owner-stats">
                <div className="ad-stat">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  <span>{ad.viewCount || 0} просмотров</span>
                </div>
                <div className="ad-stat">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>
                  <span>{ad.favoriteCount || 0} в избранном</span>
                </div>
                <div className="ad-stat">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
                  <span>{ad.commentCount || 0} комментариев</span>
                </div>
              </div>
            )}
          </motion.div>

          <motion.div
            className="ad-detail-seller"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.25 }}
          >
            <h3>Продавец</h3>
            <div className="seller-info">
              <div className="seller-info-item">
                <span className="label">Имя</span>
                <Link to={`/profile/${ad.userId}`} className="seller-name-link">{ad.userName}</Link>
              </div>
              <div className="seller-info-item">
                <span className="label">Телефон</span>
                <PhoneReveal adId={ad.id} />
              </div>
              <Link to={`/profile/${ad.userId}`} className="btn btn-outline btn-sm" style={{ marginTop: 10, width: '100%', textAlign: 'center' }}>
                Все объявления продавца
              </Link>
            </div>
          </motion.div>
        </div>
      </div>
    </motion.div>
  );
}
