import { useState, useEffect } from 'react';
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

export default function AdDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const [ad, setAd] = useState(null);
  const [selectedImage, setSelectedImage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

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

  if (loading) return <div className="loading"><div className="spinner"></div></div>;
  if (error) return <div className="empty-state"><h3>{error}</h3></div>;
  if (!ad) return null;

  const isOwner = user && user.id === ad.userId;
  const isAdmin = user && user.role === 'ADMIN';
  const hasImages = ad.images && ad.images.length > 0;

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      <div className="ad-detail">
        <div className="ad-detail-main">
          <div className="ad-detail-gallery">
            {hasImages ? (
              <AnimatePresence mode="wait">
                <motion.img
                  key={selectedImage}
                  src={imagesAPI.getUrl(ad.images[selectedImage].id)}
                  alt={ad.title}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.2 }}
                />
              </AnimatePresence>
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
            <div className="ad-detail-price">{formatPrice(ad.price)}</div>
            {ad.saleType === 'AUCTION' && (
              <div style={{ marginBottom: '12px' }}>
                <span className="badge badge-warning" style={{ fontSize: '0.78rem' }}>🔨 Аукцион</span>
              </div>
            )}
            <div style={{ fontSize: '0.813rem', color: 'var(--text-muted)', marginBottom: '16px' }}>
              Опубликовано {formatDate(ad.createdAt)}
            </div>

            {ad.saleType === 'AUCTION' && (
              <AuctionSection adId={ad.id} adOwnerId={ad.userId} />
            )}

            {/* Chat button for non-owners */}
            {isAuthenticated && !isOwner && (
              <Link
                to={`/chats?adId=${ad.id}`}
                className="btn btn-primary btn-lg"
                style={{ width: '100%', marginBottom: '12px' }}
              >
                <IconChat size={16} style={{marginRight: 6, verticalAlign: 'middle'}} /> Написать продавцу
              </Link>
            )}

            {!isAuthenticated && (
              <Link
                to="/login"
                className="btn btn-primary btn-lg"
                style={{ width: '100%', marginBottom: '12px' }}
              >
                <IconChat size={16} style={{marginRight: 6, verticalAlign: 'middle'}} /> Войдите, чтобы написать
              </Link>
            )}

            {(isOwner || isAdmin) && (
              <div style={{ display: 'flex', gap: '8px' }}>
                {isOwner && (
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
