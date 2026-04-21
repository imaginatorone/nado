import { useState, useEffect, useRef } from 'react';
import { Link, useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { usersAPI, trustAPI, adsAPI, ratingsAPI } from '../api/api';

/* вспомогательные функции */

function formatDate(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('ru-RU', {
    day: 'numeric', month: 'long', year: 'numeric'
  });
}

function formatMemberSince(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const now = new Date();
  const months = (now.getFullYear() - d.getFullYear()) * 12 + (now.getMonth() - d.getMonth());
  if (months < 1) return 'менее месяца';
  if (months < 12) return `${months} мес.`;
  const years = Math.floor(months / 12);
  const rem = months % 12;
  return rem > 0 ? `${years} г. ${rem} мес.` : `${years} г.`;
}

function AnimatedNumber({ value, duration = 1200 }) {
  const [display, setDisplay] = useState(0);
  const ref = useRef(null);
  useEffect(() => {
    const target = Number(value) || 0;
    const start = performance.now();
    const animate = (now) => {
      const progress = Math.min((now - start) / duration, 1);
      const ease = 1 - Math.pow(1 - progress, 3);
      setDisplay(Math.round(ease * target));
      if (progress < 1) ref.current = requestAnimationFrame(animate);
    };
    ref.current = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(ref.current);
  }, [value, duration]);
  return <>{display}</>;
}

function NadoBadge() {
  const [show, setShow] = useState(false);
  return (
    <span className="admin-emblem"
      onMouseEnter={() => setShow(true)} onMouseLeave={() => setShow(false)}
      onClick={() => setShow(!show)}>
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
        <path d="M12 2L4 6v5c0 5.55 3.44 10.74 8 12 4.56-1.26 8-6.45 8-12V6L12 2z"
          fill="var(--primary)" fillOpacity="0.15" stroke="var(--primary)" strokeWidth="1.5"/>
        <text x="12" y="16" textAnchor="middle" fontSize="10" fontWeight="800"
          fill="var(--primary)" fontFamily="Inter, sans-serif">N</text>
      </svg>
      {show && <span className="admin-tooltip">Команда Nado</span>}
    </span>
  );
}

function IconMail() {
  return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><rect x="2" y="4" width="20" height="16" rx="3"/><path d="M22 7l-10 6L2 7"/></svg>;
}
function IconPhone() {
  return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><path d="M22 16.92v3a2 2 0 01-2.18 2A19.79 19.79 0 013.09 5.18 2 2 0 015.11 3h3a2 2 0 012 1.72c.13.81.37 1.61.68 2.37a2 2 0 01-.45 2.11L8.09 11.45a16 16 0 006.46 6.46l2.25-2.25a2 2 0 012.11-.45c.76.31 1.56.55 2.37.68a2 2 0 011.72 2.03z"/></svg>;
}
function IconPin() {
  return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>;
}
function IconCalendar() {
  return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>;
}
function IconEdit() {
  return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>;
}
function IconChatSmall() {
  return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>;
}

/* уровни доверия */
function getTrustLevel(score) {
  if (score >= 90) return { label: 'Высокий уровень доверия', color: '#00D2A0' };
  if (score >= 80) return { label: 'Проверенный пользователь', color: '#4ECDC4' };
  if (score >= 60) return { label: 'Надёжный пользователь', color: '#FFD93D' };
  if (score >= 40) return { label: 'Базовый уровень доверия', color: '#FFA726' };
  return { label: 'Низкий уровень доверия', color: '#FF6B6B' };
}

/* основной компонент */

export default function ProfilePage() {
  const { user } = useAuth();
  const { userId } = useParams();
  const [profile, setProfile] = useState(null);
  const [trust, setTrust] = useState(null);
  const [myAds, setMyAds] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);

  const viewingOwnProfile = !userId || (user && String(user.id) === String(userId));
  const isStaff = profile?.role === 'ADMIN';

  useEffect(() => {
    setLoading(true);
    const fetchProfile = viewingOwnProfile
      ? usersAPI.getMe().then(r => r.data)
      : usersAPI.getById(userId).then(r => r.data);

    fetchProfile
      .then(profileData => {
        setProfile(profileData);
        trustAPI.getRating(profileData.id).then(t => setTrust(t.data)).catch(() => {});
        if (profileData.role !== 'ADMIN') {
          if (viewingOwnProfile) {
            adsAPI.getMy(0, 6).then(r => setMyAds(r.data.content || [])).catch(() => {});
          } else {
            adsAPI.search({ userId: profileData.id, page: 0, size: 6 })
              .then(r => setMyAds(r.data.content || []))
              .catch(() => {});
          }
          ratingsAPI.getBySeller(profileData.id).then(r => setReviews(r.data || [])).catch(() => {});
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [userId, viewingOwnProfile]);

  if (loading) return <div className="loading"><div className="spinner"></div></div>;
  if (!profile) return <div className="empty-state"><h3>Профиль не найден</h3></div>;

  /* профиль сотрудника */
  if (isStaff) {
    return (
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }} className="profile-page">
        <div className="profile-card">
          <div className="profile-card-header">
            <div className="profile-avatar-large staff-avatar">
              {profile.avatarUrl ? (
                <img src={profile.avatarUrl} alt={profile.name} className="profile-avatar-img" />
              ) : (
                <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
                  <path d="M20 4L8 10v8c0 8.84 5.12 16.74 12 18.68C26.88 34.74 32 26.84 32 18V10L20 4z"
                    fill="var(--primary)" fillOpacity="0.2" stroke="var(--primary)" strokeWidth="2"/>
                  <text x="20" y="24" textAnchor="middle" fontSize="16" fontWeight="800"
                    fill="var(--primary)" fontFamily="Inter, sans-serif">N</text>
                </svg>
              )}
            </div>
            <div className="profile-header-info">
              <h1>{profile.name} <NadoBadge /></h1>
              <span className="profile-member-since">На платформе с {formatDate(profile.createdAt)}</span>
              <span className="staff-role-text">Служба поддержки</span>
            </div>
          </div>
          <div className="staff-description">
            <p>Мы помогаем решать вопросы на платформе Nado — от споров между покупателем и продавцом до
              технических проблем. Напишите нам, и мы ответим в рабочее время.</p>
          </div>
          {viewingOwnProfile && (
            <div className="profile-actions">
              <Link to="/profile/edit" className="btn btn-outline"><IconEdit /> Редактировать</Link>
            </div>
          )}
        </div>
      </motion.div>
    );
  }

  /* профиль обычного пользователя */
  const trustLevel = trust ? getTrustLevel(trust.totalScore) : null;

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }} className="profile-page">
      <div className="profile-card">
        <div className="profile-card-header">
          <div className="profile-avatar-large">
            {profile.avatarUrl ? (
              <img src={profile.avatarUrl} alt={profile.name} className="profile-avatar-img" />
            ) : (
              profile.name?.[0]?.toUpperCase() || '?'
            )}
          </div>
          <div className="profile-header-info">
            <h1>{profile.name}</h1>
            <span className="profile-member-since">На платформе: {formatMemberSince(profile.createdAt)}</span>
          </div>
          {trust && (
            <div className="trust-badge" title={trustLevel.label}>
              <div className="trust-badge-ring" style={{
                '--trust-color': trustLevel.color,
                '--trust-pct': `${trust.totalScore}%`,
              }}>
                <span className="trust-badge-score"><AnimatedNumber value={trust.totalScore} /></span>
              </div>
              <span className="trust-badge-label">{trustLevel.label}</span>
            </div>
          )}
        </div>

        <div className="profile-stats">
          <div className="profile-stat">
            <span className="profile-stat-value"><AnimatedNumber value={profile.adsCount || 0} /></span>
            <span className="profile-stat-label">Объявлений</span>
          </div>
          <div className="profile-stat">
            <span className="profile-stat-value"><AnimatedNumber value={profile.completedDeals || 0} /></span>
            <span className="profile-stat-label">Сделок</span>
          </div>
          <div className="profile-stat">
            <span className="profile-stat-value"><AnimatedNumber value={profile.reviewsCount || 0} /></span>
            <span className="profile-stat-label">Отзывов</span>
          </div>
        </div>

        {/* контактная информация (только владелец) */}
        {viewingOwnProfile && (
          <div className="profile-info-grid">
            <div className="profile-info-item">
              <span className="profile-info-icon"><IconMail /></span>
              <div>
                <span className="profile-info-label">Email</span>
                <span className="profile-info-value">{profile.email}</span>
              </div>
            </div>
            <div className="profile-info-item">
              <span className="profile-info-icon"><IconPhone /></span>
              <div>
                <span className="profile-info-label">Телефон</span>
                <span className="profile-info-value">{profile.phone || 'Не указан'}</span>
              </div>
            </div>
            <div className="profile-info-item">
              <span className="profile-info-icon"><IconPin /></span>
              <div>
                <span className="profile-info-label">Регион</span>
                <span className="profile-info-value">{profile.region || 'Не указан'}</span>
              </div>
            </div>
            <div className="profile-info-item">
              <span className="profile-info-icon"><IconCalendar /></span>
              <div>
                <span className="profile-info-label">Регистрация</span>
                <span className="profile-info-value">{formatDate(profile.createdAt)}</span>
              </div>
            </div>
          </div>
        )}

        {/* публичная информация */}
        {!viewingOwnProfile && (
          <div className="profile-info-grid">
            <div className="profile-info-item">
              <span className="profile-info-icon"><IconPin /></span>
              <div>
                <span className="profile-info-label">Регион</span>
                <span className="profile-info-value">{profile.region || 'Не указан'}</span>
              </div>
            </div>
            <div className="profile-info-item">
              <span className="profile-info-icon"><IconCalendar /></span>
              <div>
                <span className="profile-info-label">На платформе</span>
                <span className="profile-info-value">{formatMemberSince(profile.createdAt)}</span>
              </div>
            </div>
          </div>
        )}

        {viewingOwnProfile && (
          <div className="profile-actions">
            <Link to="/profile/edit" className="btn btn-primary"><IconEdit /> Редактировать</Link>
            <Link to="/my-ads" className="btn btn-secondary">Мои объявления</Link>
          </div>
        )}

        {!viewingOwnProfile && (
          <div className="profile-actions">
            <Link to={`/chats?userId=${profile.id}`} className="btn btn-primary"><IconChatSmall /> Написать</Link>
          </div>
        )}
      </div>

      {myAds.length > 0 && (
        <div className="profile-section">
          <h2>{viewingOwnProfile ? 'Мои объявления' : 'Объявления продавца'}</h2>
          <div className="profile-ads-grid">
            {myAds.map(ad => (
              <Link key={ad.id} to={`/ads/${ad.id}`} className="profile-ad-preview">
                {ad.imageUrl ? (
                  <img src={`/api${ad.imageUrl}`} alt={ad.title} />
                ) : (
                  <div className="profile-ad-placeholder">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" strokeWidth="1.5">
                      <rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a4 4 0 00-8 0v2"/>
                    </svg>
                  </div>
                )}
                <div className="profile-ad-info">
                  <span className="profile-ad-title">{ad.title}</span>
                  {ad.price && <span className="profile-ad-price">{Number(ad.price).toLocaleString('ru-RU')} ₽</span>}
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}

      {reviews.length > 0 && (
        <div className="profile-section">
          <h2>Отзывы</h2>
          <div className="profile-reviews">
            {reviews.map((r, i) => (
              <div key={i} className="profile-review-card">
                <div className="profile-review-header">
                  <span className="profile-review-stars">{'★'.repeat(r.score)}{'☆'.repeat(5 - r.score)}</span>
                  <span className="profile-review-date">{formatDate(r.createdAt)}</span>
                </div>
                {r.comment && <p className="profile-review-text">{r.comment}</p>}
                <span className="profile-review-author">— {r.reviewerName || 'Пользователь'}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </motion.div>
  );
}
