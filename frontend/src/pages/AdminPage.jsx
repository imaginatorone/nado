import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { adminAPI } from '../api/api';
import { useToast } from '../context/ToastContext';

export default function AdminPage() {
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [ads, setAds] = useState([]);
  const [tab, setTab] = useState('stats');
  const [loading, setLoading] = useState(true);
  const toast = useToast();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [statsRes, usersRes, adsRes] = await Promise.all([
        adminAPI.getStats(),
        adminAPI.getAllUsers(),
        adminAPI.getAllAds(0, 50),
      ]);
      setStats(statsRes.data);
      setUsers(usersRes.data);
      setAds(adsRes.data.content || []);
    } catch {
      toast.error('Ошибка загрузки данных');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleUser = async (id) => {
    try {
      await adminAPI.toggleUser(id);
      setUsers(users.map(u => u.id === id ? { ...u, active: !u.active } : u));
      toast.success('Статус пользователя обновлён');
    } catch { toast.error('Ошибка'); }
  };

  const handleDeleteAd = async (id) => {
    try {
      await adminAPI.deleteAd(id);
      setAds(ads.filter(a => a.id !== id));
      toast.success('Объявление удалено');
    } catch { toast.error('Ошибка'); }
  };

  if (loading) return <div className="loading"><div className="spinner"></div></div>;

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <div className="container">
        <h1 style={{ marginBottom: 24 }}>Панель администратора</h1>

        {stats && (
          <div className="admin-grid">
            <div className="admin-stat-card">
              <div className="admin-stat-value">{stats.totalUsers}</div>
              <div className="admin-stat-label">Пользователей</div>
            </div>
            <div className="admin-stat-card">
              <div className="admin-stat-value">{stats.totalAds}</div>
              <div className="admin-stat-label">Объявлений</div>
            </div>
          </div>
        )}

        <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
          <button className={`btn ${tab === 'stats' ? 'btn-primary' : 'btn-outline'}`} onClick={() => setTab('stats')}>Пользователи</button>
          <button className={`btn ${tab === 'ads' ? 'btn-primary' : 'btn-outline'}`} onClick={() => setTab('ads')}>Объявления</button>
        </div>

        {tab === 'stats' && (
          <table className="admin-table">
            <thead>
              <tr><th>ID</th><th>Имя</th><th>Email</th><th>Роль</th><th>Статус</th><th></th></tr>
            </thead>
            <tbody>
              {users.map(u => (
                <tr key={u.id}>
                  <td>{u.id}</td>
                  <td>{u.name}</td>
                  <td>{u.email}</td>
                  <td>{u.role === 'ADMIN' ? 'Админ' : u.role}</td>
                  <td>{u.active ? 'Активен' : 'Заблокирован'}</td>
                  <td>
                    {u.role !== 'ADMIN' && (
                      <button className="btn btn-sm btn-outline" onClick={() => handleToggleUser(u.id)}>
                        {u.active ? 'Заблокировать' : 'Разблокировать'}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {tab === 'ads' && (
          <table className="admin-table">
            <thead>
              <tr><th>ID</th><th>Название</th><th>Цена</th><th>Автор</th><th>Статус</th><th></th></tr>
            </thead>
            <tbody>
              {ads.map(a => (
                <tr key={a.id}>
                  <td>{a.id}</td>
                  <td>{a.title}</td>
                  <td>{a.price ? `${a.price} ₽` : '—'}</td>
                  <td>{a.userName}</td>
                  <td>{a.status}</td>
                  <td>
                    {a.status !== 'DELETED' && (
                      <button className="btn btn-sm btn-danger" onClick={() => handleDeleteAd(a.id)}>Удалить</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </motion.div>
  );
}
