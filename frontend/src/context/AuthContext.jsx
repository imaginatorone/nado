import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { authAPI, usersAPI } from '../api/api';
import { createKeycloak, getKeycloak } from '../auth/keycloak';
import api from '../api/api';

const AuthContext = createContext(null);

// режим auth определяется из бэкенда (/auth-config)
// keycloak — целевой, legacy — переходный

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [authMode, setAuthMode] = useState(null);
  const initRef = useRef(false);

  useEffect(() => {
    if (initRef.current) return;
    initRef.current = true;
    initAuth();
  }, []);

  async function initAuth() {
    try {
      const { data: config } = await api.get('/auth-config');
      setAuthMode(config.mode);

      if (config.mode === 'keycloak') {
        await initKeycloak(config);
      } else {
        initLegacy();
      }
    } catch {
      // fallback — если бэкенд недоступен, пробуем legacy
      initLegacy();
    }
  }

  async function initKeycloak(config) {
    const kc = createKeycloak(config);

    try {
      const authenticated = await kc.init({
        onLoad: 'check-sso',
        checkLoginIframe: false,
        pkceMethod: 'S256',
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
      });

      if (authenticated) {
        await loadKeycloakUser(kc);
      }

      // auto-refresh токена за 30 секунд до истечения
      setInterval(() => {
        kc.updateToken(30).catch(() => {
          setUser(null);
        });
      }, 10000);

    } catch (err) {
      console.error('keycloak init error:', err);
    } finally {
      setLoading(false);
    }
  }

  async function loadKeycloakUser(kc) {
    try {
      // загружаем профиль из Nado backend (AuthFacade сделает auto-provision)
      const { data } = await usersAPI.getMe();
      const userData = {
        id: data.id,
        name: data.name,
        email: data.email,
        role: data.role,
        emailVerified: data.emailVerified,
        avatarUrl: data.avatarUrl,
      };
      setUser(userData);
    } catch (err) {
      console.error('ошибка загрузки профиля:', err);
    }
  }

  function initLegacy() {
    const token = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');
    if (token && savedUser) {
      try {
        setUser(JSON.parse(savedUser));
      } catch {
        localStorage.removeItem('user');
      }
    }
    setLoading(false);
  }

  // --- actions ---

  const login = useCallback(async (email, password) => {
    if (authMode === 'keycloak') {
      const kc = getKeycloak();
      if (kc) {
        kc.login({ redirectUri: window.location.origin + '/' });
      }
      return;
    }

    // legacy login
    const res = await authAPI.login({ email, password });
    const data = res.data;
    localStorage.setItem('token', data.token);
    const userData = { id: data.userId, name: data.name, email: data.email, role: data.role };
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  }, [authMode]);

  const register = useCallback(async (formData) => {
    if (authMode === 'keycloak') {
      const kc = getKeycloak();
      if (kc) {
        kc.register({ redirectUri: window.location.origin + '/' });
      }
      return;
    }

    // legacy register
    const res = await authAPI.register(formData);
    const data = res.data;
    localStorage.setItem('token', data.token);
    const userData = { id: data.userId, name: data.name, email: data.email, role: data.role };
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  }, [authMode]);

  const logout = useCallback(() => {
    if (authMode === 'keycloak') {
      const kc = getKeycloak();
      if (kc) {
        kc.logout({ redirectUri: window.location.origin + '/' });
      }
    }

    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  }, [authMode]);

  const isAuthenticated = !!user;
  const isAdmin = user?.role === 'ADMIN';
  const isModerator = user?.role === 'MODERATOR' || isAdmin;

  return (
    <AuthContext.Provider value={{
      user, loading, login, register, logout,
      isAuthenticated, isAdmin, isModerator,
      authMode,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be inside AuthProvider');
  return context;
}
