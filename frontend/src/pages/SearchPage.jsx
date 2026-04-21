import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { adsAPI } from '../api/api';
import AdCard from '../components/AdCard';

const REGIONS = [
  'Москва', 'Санкт-Петербург', 'Севастополь', 'Новосибирск', 'Екатеринбург', 'Казань',
  'Нижний Новгород', 'Челябинск', 'Самара', 'Омск', 'Ростов-на-Дону',
  'Уфа', 'Красноярск', 'Воронеж', 'Пермь', 'Волгоград',
];

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [ads, setAds] = useState([]);
  const [otherAds, setOtherAds] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [localQuery, setLocalQuery] = useState(searchParams.get('query') || '');
  const [titleOnly, setTitleOnly] = useState(searchParams.get('titleOnly') === 'true');
  const [withPhoto, setWithPhoto] = useState(searchParams.get('withPhoto') === 'true');
  const [region, setRegion] = useState(searchParams.get('region') || localStorage.getItem('nado_region') || '');

  const query = searchParams.get('query') || '';
  const categoryId = searchParams.get('categoryId') || '';
  const priceFrom = searchParams.get('priceFrom') || '';
  const priceTo = searchParams.get('priceTo') || '';
  const urlRegion = searchParams.get('region') || '';

  const hasFilters = query || categoryId || priceFrom || priceTo;

  useEffect(() => {
    setLoading(true);
    const baseParams = { page, size: 12 };
    if (query) baseParams.query = query;
    if (categoryId) baseParams.categoryId = categoryId;
    if (titleOnly) baseParams.titleOnly = true;
    if (withPhoto) baseParams.withPhoto = true;
    if (priceFrom) baseParams.priceFrom = priceFrom;
    if (priceTo) baseParams.priceTo = priceTo;

    if (region) {
      // с регионом: два запроса — в регионе + остальные
      const regionParams = { ...baseParams, region };
      const otherParams = { ...baseParams, size: 6 };

      Promise.all([
        adsAPI.search(regionParams),
        adsAPI.search(otherParams),
      ]).then(([regionRes, allRes]) => {
        const regionIds = new Set(regionRes.data.content.map(a => a.id));
        setAds(regionRes.data.content);
        setTotalPages(regionRes.data.totalPages);
        setTotalElements(regionRes.data.totalElements);
        setOtherAds(allRes.data.content.filter(a => !regionIds.has(a.id)).slice(0, 6));
      }).catch(() => {}).finally(() => setLoading(false));
    } else {
      adsAPI.search(baseParams)
        .then(res => {
          setAds(res.data.content);
          setTotalPages(res.data.totalPages);
          setTotalElements(res.data.totalElements);
          setOtherAds([]);
        })
        .catch(() => {})
        .finally(() => setLoading(false));
    }
  }, [query, categoryId, titleOnly, withPhoto, priceFrom, priceTo, page, region, urlRegion]);

  const handleSearch = (e) => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (localQuery.trim()) params.set('query', localQuery.trim());
    if (titleOnly) params.set('titleOnly', 'true');
    if (withPhoto) params.set('withPhoto', 'true');
    if (region) params.set('region', region);
    if (categoryId) params.set('categoryId', categoryId);
    if (priceFrom) params.set('priceFrom', priceFrom);
    if (priceTo) params.set('priceTo', priceTo);
    setPage(0);
    setSearchParams(params);
  };

  const handleRegionChange = (e) => {
    setRegion(e.target.value);
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3 }}
    >
      {/* Поисковая строка — всегда видна, включая мобильную */}
      <form className="search-page-bar" onSubmit={handleSearch}>
        <div className="search-page-input-wrap">
          <svg viewBox="0 0 24 24" className="search-page-icon"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
          <input
            type="text"
            className="form-control"
            placeholder="Поиск объявлений..."
            value={localQuery}
            onChange={e => setLocalQuery(e.target.value)}
            autoFocus
          />
          <button type="submit" className="btn btn-primary btn-sm">Найти</button>
        </div>
        <div className="search-page-filters">
          <label className="checkbox-label">
            <input type="checkbox" checked={titleOnly} onChange={e => setTitleOnly(e.target.checked)} />
            <span>Только в названиях</span>
          </label>
          <label className="checkbox-label">
            <input type="checkbox" checked={withPhoto} onChange={e => setWithPhoto(e.target.checked)} />
            <span>Только с фото</span>
          </label>
          <select
            className="form-control"
            value={region}
            onChange={handleRegionChange}
            style={{ maxWidth: '180px', padding: '6px 10px', fontSize: '0.85rem' }}
          >
            <option value="">Все регионы</option>
            {REGIONS.map(r => <option key={r} value={r}>{r}</option>)}
          </select>
        </div>
      </form>

      <div className="page-header">
        <h1>
          {region
            ? (query ? `«${query}» в ${region}` : `Объявления в ${region}`)
            : (query ? `Результаты по «${query}»` : (hasFilters ? 'Результаты поиска' : 'Свежие объявления'))
          }
          <span style={{ fontSize: '1rem', color: 'var(--text-muted)', fontWeight: 400, marginLeft: '12px' }}>
            {totalElements} объявлений
          </span>
        </h1>
      </div>

      {loading ? (
        <div className="loading"><div className="spinner"></div></div>
      ) : ads.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon"><svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" strokeWidth="1.5"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg></div>
          <h3>{region ? `В регионе «${region}» пока ничего нет` : 'Не нашли? Бывает!'}</h3>
          <p>{region ? 'Попробуйте другой регион или расширьте поиск' : 'Попробуйте другой запрос'}</p>
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

      {/* Объявления из других регионов */}
      {region && otherAds.length > 0 && (
        <>
          <div className="page-header" style={{ marginTop: '32px' }}>
            <h2 style={{ fontSize: '1.15rem', color: 'var(--text-secondary)' }}>
              Из других регионов
            </h2>
          </div>
          <div className="ads-grid">
            {otherAds.map((ad, i) => (
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
        </>
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
