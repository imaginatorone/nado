import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { adsAPI } from '../api/api';
import { Link } from 'react-router-dom';

function PhoneIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{verticalAlign: 'middle', marginRight: 6}}>
      <path d="M22 16.92v3a2 2 0 01-2.18 2A19.79 19.79 0 013.09 5.18 2 2 0 015.11 3h3a2 2 0 012 1.72c.13.81.37 1.61.68 2.37a2 2 0 01-.45 2.11L8.09 11.45a16 16 0 006.46 6.46l2.25-2.25a2 2 0 012.11-.45c.76.31 1.56.55 2.37.68a2 2 0 011.72 2.03z"/>
    </svg>
  );
}

export default function PhoneReveal({ adId }) {
  const { isAuthenticated } = useAuth();
  const [phone, setPhone] = useState(null);
  const [loading, setLoading] = useState(false);
  const [revealed, setRevealed] = useState(false);

  const handleReveal = async () => {
    if (!isAuthenticated) return;
    setLoading(true);
    try {
      const res = await adsAPI.getSellerPhone(adId);
      const p = res.data?.phone;
      setPhone(p || 'Не указан');
      setRevealed(true);
    } catch {
      setPhone('Ошибка загрузки');
    } finally {
      setLoading(false);
    }
  };

  if (revealed) {
    return <span className="phone-revealed"><PhoneIcon /> {phone}</span>;
  }

  if (!isAuthenticated) {
    return (
      <span className="phone-login-hint">
        <Link to="/login">Войдите</Link>, чтобы увидеть контакт
      </span>
    );
  }

  return (
    <button className="phone-reveal" onClick={handleReveal} disabled={loading}>
      <PhoneIcon />
      {loading ? 'Загрузка...' : 'Показать телефон'}
    </button>
  );
}
