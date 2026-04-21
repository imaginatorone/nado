import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { adsAPI, usersAPI } from '../api/api';
import ErrorState from '../components/ErrorState';
import api from '../api/api';

/**
 * Hidden admin panel. Not linked from any UI element.
 * Access only at /nado-control (no menu links, no breadcrumbs).
 * Server-side role check: only ADMIN can use these endpoints.
 */
export default function AdminPanel() {
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [ads, setAds] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [tab, setTab] = useState('overview');

  const isAdmin = user?.role === 'ADMIN';

  useEffect(() => {
    if (!isAuthenticated || !isAdmin) {
      // тихий редирект без раскрытия страницы
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, isAdmin, navigate]);

  const loadData = useCallback(async () => {
    if (!isAdmin) return;
    setLoading(true);
    setError(false);
    try {
      const [usersRes, adsRes] = await Promise.all([
        api.get('/admin/users'),
        adsAPI.getAll(0, 100),
      ]);
      setUsers(usersRes.data || []);
      const adsData = adsRes.data?.content || [];
      setAds(adsData);
      setStats({
        totalUsers: (usersRes.data || []).length,
        totalAds: adsData.length,
        activeAds: adsData.filter(a => a.status === 'ACTIVE').length,
      });
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, [isAdmin]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleDeleteAd = async (adId) => {
    if (!window.confirm('Удалить объявление #' + adId + '?')) return;
    try {
      await adsAPI.delete(adId);
      loadData();
    } catch { /* silent */ }
  };

  const handleBanUser = async (userId) => {
    if (!window.confirm('Заблокировать пользователя #' + userId + '?')) return;
    try {
      await api.post(`/admin/users/${userId}/ban`);
      loadData();
    } catch { /* silent */ }
  };

  if (!isAdmin) return null;
  if (error) return <ErrorState onRetry={loadData} />;

  return (
    <div style={{ maxWidth: 1000, margin: '0 auto', padding: '24px 16px' }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24,
        padding: '16px 20px', background: 'var(--bg-card)',
        border: '1px solid var(--border)', borderRadius: 'var(--radius)'
      }}>
        <NadoShield />
        <div>
          <h1 style={{ margin: 0, fontSize: '1.2rem' }}>Панель управления</h1>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Только для команды Nado
          </span>
        </div>
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        {['overview', 'users', 'ads'].map(t => (
          <button
            key={t}
            className={`btn ${tab === t ? 'btn-primary' : 'btn-outline'} btn-sm`}
            onClick={() => setTab(t)}
          >
            {t === 'overview' ? 'Обзор' : t === 'users' ? 'Пользователи' : 'Объявления'}
          </button>
        ))}
      </div>

      {loading && <div className="loading"><div className="spinner"></div></div>}

      {/* Overview */}
      {!loading && tab === 'overview' && stats && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
          <StatCard label="Пользователей" value={stats.totalUsers} />
          <StatCard label="Объявлений" value={stats.totalAds} />
          <StatCard label="Активных" value={stats.activeAds} />
        </div>
      )}

      {/* Users */}
      {!loading && tab === 'users' && (
        <div style={{ overflowX: 'auto' }}>
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th><th>Имя</th><th>Email</th><th>Роль</th><th>Действия</th>
              </tr>
            </thead>
            <tbody>
              {users.map(u => (
                <tr key={u.id}>
                  <td>{u.id}</td>
                  <td>{u.name}</td>
                  <td>{u.email}</td>
                  <td>
                    <span style={{
                      fontSize: '0.7rem', padding: '2px 8px', borderRadius: 4,
                      background: u.role === 'ADMIN' ? 'var(--primary)' : 'var(--bg-input)',
                      color: u.role === 'ADMIN' ? 'white' : 'var(--text-secondary)'
                    }}>
                      {u.role}
                    </span>
                  </td>
                  <td>
                    {u.role !== 'ADMIN' && (
                      <button className="btn btn-danger btn-sm" style={{ fontSize: '0.7rem', padding: '2px 10px' }}
                        onClick={() => handleBanUser(u.id)}>
                        Бан
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Ads */}
      {!loading && tab === 'ads' && (
        <div style={{ overflowX: 'auto' }}>
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th><th>Название</th><th>Цена</th><th>Автор</th><th>Статус</th><th>Действия</th>
              </tr>
            </thead>
            <tbody>
              {ads.map(ad => (
                <tr key={ad.id}>
                  <td>{ad.id}</td>
                  <td style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {ad.title}
                  </td>
                  <td>{ad.price ? ad.price.toLocaleString('ru-RU') + ' ₽' : '—'}</td>
                  <td>{ad.userName}</td>
                  <td>{ad.status}</td>
                  <td>
                    <button className="btn btn-danger btn-sm" style={{ fontSize: '0.7rem', padding: '2px 10px' }}
                      onClick={() => handleDeleteAd(ad.id)}>
                      Удалить
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function StatCard({ label, value }) {
  return (
    <div style={{
      padding: 20, background: 'var(--bg-card)', border: '1px solid var(--border)',
      borderRadius: 'var(--radius)', textAlign: 'center'
    }}>
      <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--primary)' }}>{value}</div>
      <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: 4 }}>{label}</div>
    </div>
  );
}

function NadoShield() {
  return (
    <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
      <path d="M16 2L4 8v8c0 7.18 5.12 13.4 12 14.88C22.88 29.4 28 23.18 28 16V8L16 2z"
        fill="var(--primary)" fillOpacity="0.15" stroke="var(--primary)" strokeWidth="1.5"/>
      <text x="16" y="20" textAnchor="middle" fontSize="12" fontWeight="800"
        fill="var(--primary)" fontFamily="Inter, sans-serif">N</text>
    </svg>
  );
}
