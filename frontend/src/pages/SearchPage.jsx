import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { adsAPI } from '../api/api';
import AdCard from '../components/AdCard';

export default function SearchPage() {
  const [searchParams] = useSearchParams();
  const [ads, setAds] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const query = searchParams.get('query') || '';
  const categoryId = searchParams.get('categoryId') || '';
  const titleOnly = searchParams.get('titleOnly') === 'true';
  const withPhoto = searchParams.get('withPhoto') === 'true';
  const priceFrom = searchParams.get('priceFrom') || '';
  const priceTo = searchParams.get('priceTo') || '';

  useEffect(() => {
    setLoading(true);
    const params = { page, size: 12 };
    if (query) params.query = query;
    if (categoryId) params.categoryId = categoryId;
    if (titleOnly) params.titleOnly = true;
    if (withPhoto) params.withPhoto = true;
    if (priceFrom) params.priceFrom = priceFrom;
    if (priceTo) params.priceTo = priceTo;

    adsAPI.search(params)
      .then(res => {
        setAds(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [query, categoryId, titleOnly, withPhoto, priceFrom, priceTo, page]);

  if (loading) return <div className="loading"><div className="spinner"></div></div>;

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3 }}
    >
      <div className="page-header">
        <h1>
          {query ? `Результаты по «${query}»` : 'Результаты поиска'}
          <span style={{ fontSize: '1rem', color: 'var(--text-muted)', fontWeight: 400, marginLeft: '12px' }}>
            {totalElements} объявлений
          </span>
        </h1>
      </div>

      {ads.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon"><svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" strokeWidth="1.5"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg></div>
          <h3>Не нашли? Бывает!</h3>
          <p>Может, это ещё никому не <em>надо</em> было продавать. Попробуйте другой запрос</p>
        </div>
      ) : (
        <div className="ads-grid">
          {ads.map((ad, i) => (
            <motion.div
              key={ad.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.05 }}
            >
              <AdCard ad={ad} />
            </motion.div>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Назад</button>
          {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
            const pageNum = totalPages <= 7 ? i : Math.max(0, Math.min(page - 3, totalPages - 7)) + i;
            return (
              <button key={pageNum} className={pageNum === page ? 'active' : ''} onClick={() => setPage(pageNum)}>
                {pageNum + 1}
              </button>
            );
          })}
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Далее →</button>
        </div>
      )}
    </motion.div>
  );
}
