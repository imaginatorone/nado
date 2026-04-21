import { useState, useEffect, useCallback, useRef } from 'react';
import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { adsAPI } from '../api/api';
import AdCard from '../components/AdCard';
import { SkeletonGrid } from '../components/SkeletonCard';
import ErrorState from '../components/ErrorState';
import { IconCompass, IconPlus } from '../components/Icons';

const EMPTY_STATES = [
  { title: 'Пока тихо', sub: 'Станьте первым — разместите объявление!' },
  { title: 'Строим великое', sub: 'Объявлений пока нет. Будьте первопроходцем!' },
  { title: 'Это место для вас', sub: 'Ваше объявление могло бы быть прямо здесь.' },
  { title: 'Первый шаг', sub: 'Каждая площадка начиналась с одного объявления.' },
  { title: 'Новые горизонты', sub: 'Объявлений пока нет, но это ненадолго.' },
  { title: 'Старт дан', sub: 'Площадка пуста — самое время действовать.' },
  { title: 'Территория свободна', sub: 'Ни одного объявления. Может, ваше станет первым?' },
  { title: 'Чистый холст', sub: 'Здесь пока пусто. Это ваш шанс!' },
];

const HERO_LINES = [
  'Найдите то, что вам надо',
  'Продайте то, что уже не надо',
  'Найдите мастера на все руки',
  'Закажите услугу рядом с вами',
  'Откройте лучшие предложения',
  'Найдите работу или сотрудника',
  'Купите выгодно у реальных людей',
];

export default function HomePage() {
  const [ads, setAds] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(false);
  const [emptyMsg] = useState(() => EMPTY_STATES[Math.floor(Math.random() * EMPTY_STATES.length)]);
  const [heroIndex, setHeroIndex] = useState(0);
  const sentinelRef = useRef(null);

  useEffect(() => {
    const interval = setInterval(() => {
      setHeroIndex(prev => (prev + 1) % HERO_LINES.length);
    }, 3500);
    return () => clearInterval(interval);
  }, []);

  const loadAds = useCallback(() => {
    setLoading(true);
    setError(false);
    adsAPI.getAll(0, 20)
      .then(res => {
        setAds(res.data.content);
        setTotalPages(res.data.totalPages);
        setPage(0);
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { loadAds(); }, [loadAds]);

  const loadMore = useCallback(() => {
    const nextPage = page + 1;
    if (nextPage >= totalPages || loadingMore) return;
    setLoadingMore(true);
    adsAPI.getAll(nextPage, 20)
      .then(res => {
        setAds(prev => [...prev, ...res.data.content]);
        setPage(nextPage);
      })
      .finally(() => setLoadingMore(false));
  }, [page, totalPages, loadingMore]);

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) loadMore(); },
      { rootMargin: '200px' }
    );
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [loadMore]);

  const hasMore = page < totalPages - 1;

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3 }}
    >
      <div className="hero">
        <h1 className="hero-rotating">
          <AnimatePresence mode="wait">
            <motion.span
              key={heroIndex}
              initial={{ opacity: 0, y: 20, filter: 'blur(8px)' }}
              animate={{ opacity: 1, y: 0, filter: 'blur(0px)' }}
              exit={{ opacity: 0, y: -20, filter: 'blur(8px)' }}
              transition={{ duration: 0.5, ease: 'easeInOut' }}
              className="hero-line"
            >
              {HERO_LINES[heroIndex].split(' ').map((word, i) => {
                const highlighted = ['надо', 'мастера', 'услугу', 'предложения', 'работу', 'выгодно'];
                const isHighlighted = highlighted.some(h => word.toLowerCase().includes(h));
                return (
                  <span key={i}>
                    {isHighlighted ? <span className="brand-text">{word}</span> : word}
                    {' '}
                  </span>
                );
              })}
            </motion.span>
          </AnimatePresence>
        </h1>
        <p>Объявления, услуги и работа от реальных людей.</p>
      </div>

      <div className="page-header">
        <h1>Свежие объявления</h1>
      </div>

      {loading ? (
        <SkeletonGrid count={8} />
      ) : error ? (
        <ErrorState onRetry={loadAds} />
      ) : ads.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon"><IconCompass /></div>
          <h3>{emptyMsg.title}</h3>
          <p>{emptyMsg.sub}</p>
          <Link to="/ads/new" className="btn btn-primary" style={{ marginTop: '20px' }}>
            <IconPlus size={14} style={{marginRight: 4, verticalAlign: 'middle'}} /> Подать объявление
          </Link>
        </div>
      ) : (
        <>
          <div className="ads-grid">
            {ads.map((ad, i) => (
              <motion.div
                key={ad.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: Math.min(i * 0.04, 0.4), duration: 0.3 }}
              >
                <AdCard ad={ad} />
              </motion.div>
            ))}
          </div>

          {/* Infinite scroll sentinel */}
          {hasMore && (
            <div ref={sentinelRef} className="infinite-scroll-sentinel">
              {loadingMore && <SkeletonGrid count={4} />}
            </div>
          )}

          {!hasMore && ads.length > 0 && (
            <div className="infinite-scroll-end">
              Все объявления загружены ✓
            </div>
          )}
        </>
      )}
    </motion.div>
  );
}
