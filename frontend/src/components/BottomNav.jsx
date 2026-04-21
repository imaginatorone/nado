import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useState, useEffect } from 'react';
import { notificationsAPI } from '../api/api';

export default function BottomNav({ unreadCount = 0 }) {
  const { pathname } = useLocation();
  const { isAuthenticated, user } = useAuth();
  const [notifCount, setNotifCount] = useState(0);

  const isActive = (path) => pathname === path || pathname.startsWith(path + '/');

  // polling уведомлений для mobile bell
  useEffect(() => {
    if (!isAuthenticated) return;
    const load = () => notificationsAPI.getUnreadCount()
      .then(r => setNotifCount(r.data?.count || 0)).catch(() => {});
    load();
    const iv = setInterval(load, 20000);
    return () => clearInterval(iv);
  }, [isAuthenticated]);

  const initial = user?.name ? user.name.charAt(0).toUpperCase() : '?';

  return (
    <nav className="bottom-nav">
      <div className="bottom-nav-items">
        <Link to="/" className={`bottom-nav-item ${isActive('/') && pathname === '/' ? 'active' : ''}`}>
          <svg viewBox="0 0 24 24"><path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-4 0a1 1 0 01-1-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 01-1 1h-2z"/></svg>
          <span>Главная</span>
        </Link>

        <Link to="/search" className={`bottom-nav-item ${isActive('/search') ? 'active' : ''}`}>
          <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
          <span>Поиск</span>
        </Link>

        <Link to="/ads/new" className="bottom-nav-item">
          <div className="bottom-nav-fab">+</div>
        </Link>

        {isAuthenticated ? (
          <>
            <Link to="/notifications" className={`bottom-nav-item ${isActive('/notifications') ? 'active' : ''}`}>
              <div className="bottom-nav-bell">🔔</div>
              {notifCount > 0 && <span className="bottom-nav-badge">{notifCount}</span>}
              <span>Уведомления</span>
            </Link>

            <Link to="/profile" className={`bottom-nav-item ${isActive('/profile') ? 'active' : ''}`}>
              <div className="bottom-nav-avatar">{initial}</div>
              <span className="bottom-nav-name">{user?.name?.split(' ')[0] || 'Профиль'}</span>
            </Link>
          </>
        ) : (
          <>
            <Link to="/favorites" className={`bottom-nav-item ${isActive('/favorites') ? 'active' : ''}`}>
              <svg viewBox="0 0 24 24"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>
              <span>Избранное</span>
            </Link>

            <Link to="/login" className="bottom-nav-item">
              <svg viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <span>Войти</span>
            </Link>
          </>
        )}
      </div>
    </nav>
  );
}
