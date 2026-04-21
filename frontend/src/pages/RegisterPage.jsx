import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import CaptchaWidget from '../components/CaptchaWidget';

export default function RegisterPage() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [captchaId, setCaptchaId] = useState('');
  const [captchaCode, setCaptchaCode] = useState('');
  const [errors, setErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [touched, setTouched] = useState(false);
  const { register, isAuthenticated, authMode, loading } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) navigate('/', { replace: true });
  }, [isAuthenticated, navigate]);

  // в keycloak mode — redirect на KC registration
  useEffect(() => {
    if (!loading && authMode === 'keycloak' && !isAuthenticated) {
      register();
    }
  }, [loading, authMode, isAuthenticated, register]);

  if (isAuthenticated || authMode === 'keycloak') return null;

  const clearError = (field) => {
    setErrors(prev => {
      if (!prev[field]) return prev;
      const next = { ...prev };
      delete next[field];
      return next;
    });
  };

  const validate = () => {
    const errs = {};
    if (!name.trim()) errs.name = 'Имя обязательно';
    if (!email.trim()) errs.email = 'Email обязателен';
    else if (!/\S+@\S+\.\S+/.test(email)) errs.email = 'Некорректный формат email';
    if (!password) errs.password = 'Пароль обязателен';
    else if (password.length < 6) errs.password = 'Пароль должен содержать минимум 6 символов';
    if (!confirmPassword) errs.confirmPassword = 'Подтверждение пароля обязательно';
    else if (password !== confirmPassword) errs.confirmPassword = 'Пароли не совпадают';
    if (!captchaCode.trim()) errs.captcha = 'Введите текст с картинки';
    return errs;
  };

  const isFormReady = name.trim() && email.trim() && password && confirmPassword && captchaCode.trim();

  const formatPhone = (value) => {
    const digits = value.replace(/\D/g, '');
    if (digits.length === 0) return '';
    let formatted = '+' + digits[0];
    if (digits.length > 1) formatted += ' (' + digits.substring(1, 4);
    if (digits.length > 4) formatted += ') ' + digits.substring(4, 7);
    if (digits.length > 7) formatted += '-' + digits.substring(7, 9);
    if (digits.length > 9) formatted += '-' + digits.substring(9, 11);
    return formatted;
  };

  const handlePhoneChange = (e) => setPhone(formatPhone(e.target.value));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGlobalError('');
    setTouched(true);

    const errs = validate();
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;

    setSubmitting(true);
    try {
      await register({
        name, email,
        phone: phone.replace(/\D/g, '') ? phone : '',
        password, confirmPassword,
        captchaId, captchaCode,
      });
      navigate('/');
    } catch (err) {
      const data = err.response?.data;
      if (data?.details) {
        setErrors(data.details);
      } else {
        setGlobalError(data?.error || 'Ошибка регистрации');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleDisabledClick = () => {
    if (!isFormReady) {
      setTouched(true);
      setErrors(validate());
    }
  };

  if (isAuthenticated) return null;

  return (
    <div className="form-card">
      <h1>Регистрация</h1>
      <p className="form-subtitle">Создайте аккаунт на платформе Nado</p>

      {globalError && <div className="alert alert-error">{globalError}</div>}

      <form onSubmit={handleSubmit} noValidate>
        <div className="form-group">
          <label htmlFor="reg-name">Ваше имя <span className="required">*</span></label>
          <input id="reg-name" type="text"
            className={`form-control ${touched && errors.name ? 'error' : ''}`}
            placeholder="Иван Иванов" value={name}
            onChange={e => { setName(e.target.value); clearError('name'); }}
          />
          {touched && errors.name && <div className="form-error">{errors.name}</div>}
        </div>

        <div className="form-group">
          <label htmlFor="reg-email">Электронная почта <span className="required">*</span></label>
          <input id="reg-email" type="email"
            className={`form-control ${touched && errors.email ? 'error' : ''}`}
            placeholder="ivan@example.com" value={email}
            onChange={e => { setEmail(e.target.value); clearError('email'); }}
          />
          {touched && errors.email && <div className="form-error">{errors.email}</div>}
        </div>

        <div className="form-group">
          <label htmlFor="reg-phone">Номер телефона</label>
          <input id="reg-phone" type="tel" className="form-control"
            placeholder="+7 (___) ___-__-__" value={phone}
            onChange={handlePhoneChange} maxLength={18}
          />
        </div>

        <div className="form-group">
          <label htmlFor="reg-password">Пароль <span className="required">*</span></label>
          <input id="reg-password" type="password"
            className={`form-control ${touched && errors.password ? 'error' : ''}`}
            placeholder="Минимум 6 символов" value={password}
            onChange={e => { setPassword(e.target.value); clearError('password'); }}
          />
          {touched && errors.password && <div className="form-error">{errors.password}</div>}
        </div>

        <div className="form-group">
          <label htmlFor="reg-confirm">Подтверждение пароля <span className="required">*</span></label>
          <input id="reg-confirm" type="password"
            className={`form-control ${touched && errors.confirmPassword ? 'error' : ''}`}
            placeholder="Повторите пароль" value={confirmPassword}
            onChange={e => { setConfirmPassword(e.target.value); clearError('confirmPassword'); }}
          />
          {touched && errors.confirmPassword && <div className="form-error">{errors.confirmPassword}</div>}
        </div>

        <CaptchaWidget
          onVerify={(id, code) => { setCaptchaId(id); setCaptchaCode(code); }}
          onCodeChange={(c) => { setCaptchaCode(c); clearError('captcha'); }}
        />
        {touched && errors.captcha && <div className="form-error" style={{textAlign:'center', marginTop: 4}}>{errors.captcha}</div>}

        <div style={{ position: 'relative', marginTop: 16 }}>
          {!isFormReady && (
            <div className="btn-overlay" onClick={handleDisabledClick} />
          )}
          <button type="submit"
            className={`btn btn-primary btn-lg btn-full ${!isFormReady ? 'btn-dimmed' : ''}`}
            disabled={submitting}>
            {submitting ? 'Регистрация...' : 'Зарегистрироваться'}
          </button>
        </div>
      </form>

      <div className="form-footer">
        Уже с нами? <Link to="/login">Войдите!</Link>
      </div>
    </div>
  );
}
