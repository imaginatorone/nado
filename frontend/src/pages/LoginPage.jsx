import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { login, isAuthenticated, authMode, loading } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  // в keycloak mode — redirect на KC login, форма не показывается
  useEffect(() => {
    if (!loading && authMode === 'keycloak' && !isAuthenticated) {
      login();
    }
  }, [loading, authMode, isAuthenticated, login]);

  if (isAuthenticated || authMode === 'keycloak') return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);

    try {
      await login(email, password);
      navigate('/');
    } catch (err) {
      const msg = err.response?.data?.error || 'Ошибка входа';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="form-card">
      <h1>Вход</h1>
      <p className="form-subtitle">С возвращением! Мы тут без вас скучали 👋</p>

      {error && <div className="alert alert-error">{error}</div>}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="login-email">Электронная почта <span className="required">*</span></label>
          <input
            id="login-email"
            type="email"
            className="form-control"
            placeholder="ivan@example.com"
            value={email}
            onChange={e => setEmail(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="login-password">Пароль <span className="required">*</span></label>
          <input
            id="login-password"
            type="password"
            className="form-control"
            placeholder="Минимум 6 символов"
            value={password}
            onChange={e => setPassword(e.target.value)}
            required
          />
        </div>

        <button type="submit" className="btn btn-primary btn-lg" style={{ width: '100%' }} disabled={submitting}>
          {submitting ? 'Вход...' : 'Войти'}
        </button>
      </form>

      <div className="form-footer">
        Ещё нет аккаунта? <Link to="/register">Присоединяйтесь!</Link>
      </div>
    </div>
  );
}
