import { Link } from 'react-router-dom';
import { imagesAPI } from '../api/api';

function formatPrice(price) {
  if (!price) return 'Цена не указана';
  return new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 0 }).format(price);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' });
}

export default function AdCard({ ad }) {
  const hasImage = ad.images && ad.images.length > 0;

  return (
    <Link to={`/ads/${ad.id}`} className="ad-card" id={`ad-card-${ad.id}`}>
      <div className="ad-card-image">
        {hasImage ? (
          <img src={imagesAPI.getUrl(ad.images[0].id)} alt={ad.title} loading="lazy" />
        ) : (
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>
        )}
      </div>
      <div className="ad-card-body">
        <span className="ad-card-category">{ad.categoryName}</span>
        <div className="ad-card-price">{formatPrice(ad.price)}</div>
        <div className="ad-card-title">{ad.title}</div>
        <div className="ad-card-meta">
          {ad.region && <span className="ad-card-region">{ad.region}</span>}
          <span>{formatDate(ad.createdAt)}</span>
        </div>
      </div>
    </Link>
  );
}
