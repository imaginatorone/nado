import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { favoritesAPI } from '../api/api';
import AdCard from '../components/AdCard';
import { SkeletonGrid } from '../components/SkeletonCard';

export default function FavoritesPage() {
  const [ads, setAds] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    favoritesAPI.getMyFavorites(0, 50)
      .then(res => setAds(res.data.content || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
      <div className="container">
        <div className="page-header">
          <h1>Избранное</h1>
        </div>

        {loading ? (
          <SkeletonGrid count={6} />
        ) : ads.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon"><svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" strokeWidth="1.5"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg></div>
            <h3>Пока пусто</h3>
            <p>Нажмите на сердечко на карточке объявления, чтобы добавить в избранное</p>
          </div>
        ) : (
          <div className="ads-grid">
            {ads.map(ad => <AdCard key={ad.id} ad={ad} />)}
          </div>
        )}
      </div>
    </motion.div>
  );
}
