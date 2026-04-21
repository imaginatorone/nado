import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { adminAPI } from '../api/api';
import ErrorState from '../components/ErrorState';

export default function AdminDashboardPage() {
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const isAdmin = user?.role === 'ADMIN';

  useEffect(() => {
    if (!isAuthenticated || !isAdmin) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, isAdmin, navigate]);

  const load = async () => {
    if (!isAdmin) return;
    setLoading(true);
    setError(false);
    try {
      const res = await adminAPI.getDashboard();
      setData(res.data);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [isAdmin]);

  if (!isAdmin) return null;
  if (error) return <ErrorState onRetry={load} />;
  if (loading || !data) return <div className="loading"><div className="spinner"></div></div>;

  const { users, ads, auctions, wantedTotal, notificationsTotal, pendingModeration, topCategories, dailyAds } = data;

  return (
    <div style={{ maxWidth: 1100, margin: '0 auto', padding: '24px 16px' }}>

      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 28 }}>
        <DashboardIcon />
        <div>
          <h1 style={{ margin: 0, fontSize: '1.3rem' }}>Аналитика платформы</h1>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Данные в реальном времени
          </span>
        </div>
      </div>

      {/* Пользователи */}
      <SectionTitle>Пользователи</SectionTitle>
      <div className="dash-grid dash-grid-5">
        <Card label="Всего" value={users.total} color="var(--primary)" />
        <Card label="Активных" value={users.active} color="#22c55e" />
        <Card label="Тел. верифицирован" value={users.phoneVerified} color="#3b82f6" />
        <Card label="Email верифицирован" value={users.emailVerified} color="#8b5cf6" />
        <Card label="Заблокировано" value={users.banned} color="#ef4444" />
      </div>

      {/* Объявления */}
      <SectionTitle>Объявления</SectionTitle>
      <div className="dash-grid dash-grid-4">
        <Card label="Всего" value={ads.total} color="var(--primary)" />
        <Card label="Опубликовано" value={ads.published} color="#22c55e" />
        <Card label="На модерации" value={ads.pending} color="#f59e0b" accent />
        <Card label="Отклонено" value={ads.rejected} color="#ef4444" />
      </div>
      <div className="dash-grid dash-grid-3" style={{ marginTop: 12 }}>
        <Card label="Продано" value={ads.sold} color="#10b981" />
        <Card label="В архиве" value={ads.archived} color="#6b7280" />
        <Card label="Заблокировано" value={ads.blocked} color="#dc2626" />
      </div>

      {/* Аукционы / Wanted / Уведомления */}
      <SectionTitle>Аукционы и запросы</SectionTitle>
      <div className="dash-grid dash-grid-4">
        <Card label="Аукционов всего" value={auctions.total} color="var(--primary)" />
        <Card label="Активные" value={auctions.active} color="#22c55e" />
        <Card label="Завершённые" value={auctions.finished} color="#3b82f6" />
        <Card label="Без ставок" value={auctions.noBids} color="#f59e0b" />
      </div>
      <div className="dash-grid dash-grid-3" style={{ marginTop: 12 }}>
        <Card label="Запросов «Хочу купить»" value={wantedTotal} color="#8b5cf6" />
        <Card label="Уведомлений отправлено" value={notificationsTotal} color="#06b6d4" />
        <Card label="Ожидают модерации" value={pendingModeration} color="#f59e0b" accent={pendingModeration > 0} />
      </div>

      {/* Популярные категории */}
      {topCategories && topCategories.length > 0 && (
        <>
          <SectionTitle>Популярные категории (по опубликованным)</SectionTitle>
          <div style={{
            background: 'var(--bg-card)', border: '1px solid var(--border)',
            borderRadius: 'var(--radius)', padding: 20, marginBottom: 24
          }}>
            {topCategories.map((cat, i) => (
              <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: i < topCategories.length - 1 ? 10 : 0 }}>
                <div style={{ minWidth: 30, fontWeight: 700, color: 'var(--text-muted)', fontSize: '0.85rem' }}>#{i + 1}</div>
                <div style={{ flex: 1, fontSize: '0.9rem' }}>{cat.name}</div>
                <BarSegment value={cat.count} max={topCategories[0]?.count || 1} />
                <div style={{ minWidth: 40, textAlign: 'right', fontWeight: 700, fontSize: '0.9rem' }}>{cat.count}</div>
              </div>
            ))}
          </div>
        </>
      )}

      {/* Динамика за 30 дней */}
      {dailyAds && dailyAds.length > 2 && (
        <>
          <SectionTitle>Новые объявления за 30 дней</SectionTitle>
          <div style={{
            background: 'var(--bg-card)', border: '1px solid var(--border)',
            borderRadius: 'var(--radius)', padding: '20px 20px 12px', marginBottom: 24
          }}>
            <MiniChart data={dailyAds} />
          </div>
        </>
      )}
    </div>
  );
}

function SectionTitle({ children }) {
  return <h2 style={{ fontSize: '1rem', fontWeight: 700, margin: '20px 0 12px', color: 'var(--text-secondary)' }}>{children}</h2>;
}

function Card({ label, value, color, accent }) {
  return (
    <div style={{
      padding: '16px 18px', background: 'var(--bg-card)',
      border: accent ? `2px solid ${color}` : '1px solid var(--border)',
      borderRadius: 'var(--radius)', textAlign: 'center',
      transition: 'transform 0.15s', cursor: 'default',
    }}>
      <div style={{ fontSize: '1.8rem', fontWeight: 800, color, lineHeight: 1.1 }}>{value}</div>
      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 6 }}>{label}</div>
    </div>
  );
}

function BarSegment({ value, max }) {
  const pct = Math.round((value / max) * 100);
  return (
    <div style={{ flex: 2, height: 8, background: 'var(--bg-input)', borderRadius: 4, overflow: 'hidden' }}>
      <div style={{ width: `${pct}%`, height: '100%', background: 'var(--primary)', borderRadius: 4, transition: 'width 0.3s' }} />
    </div>
  );
}

function MiniChart({ data }) {
  const maxVal = Math.max(...data.map(d => d.count), 1);
  const barW = Math.max(4, Math.floor(600 / data.length) - 2);

  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: 2, height: 100, justifyContent: 'center', flexWrap: 'nowrap', overflowX: 'auto' }}>
      {data.map((d, i) => {
        const h = Math.max(2, (d.count / maxVal) * 90);
        return (
          <div key={i} title={`${d.date}: ${d.count}`} style={{
            width: barW, height: h, borderRadius: 2, flexShrink: 0,
            background: i === data.length - 1 ? 'var(--primary)' : 'var(--primary-light, rgba(99,102,241,0.35))',
            transition: 'height 0.3s',
          }} />
        );
      })}
    </div>
  );
}

function DashboardIcon() {
  return (
    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" strokeWidth="1.5">
      <rect x="3" y="3" width="7" height="7" rx="1.5" />
      <rect x="14" y="3" width="7" height="7" rx="1.5" />
      <rect x="3" y="14" width="7" height="7" rx="1.5" />
      <rect x="14" y="14" width="7" height="7" rx="1.5" />
    </svg>
  );
}
