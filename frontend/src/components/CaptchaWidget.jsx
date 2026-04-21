import { useState, useEffect, useCallback } from 'react';

export default function CaptchaWidget({ onVerify, onCodeChange }) {
  const [captchaId, setCaptchaId] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(true);

  const loadCaptcha = useCallback(async () => {
    setLoading(true);
    setCode('');
    onVerify?.('', '');
    onCodeChange?.('');
    try {
      const res = await fetch('/api/captcha');
      if (!res.ok) throw new Error();
      const data = await res.json();
      setCaptchaId(data.id);
      setImageUrl(data.image);
    } catch {
      setImageUrl('');
    } finally {
      setLoading(false);
    }
  }, [onVerify, onCodeChange]);

  useEffect(() => { loadCaptcha(); }, []);

  const handleChange = (e) => {
    const val = e.target.value.toUpperCase();
    setCode(val);
    onVerify?.(captchaId, val);
    onCodeChange?.(val);
  };

  return (
    <div className="captcha-block">
      <label className="captcha-label">
        <svg width="16" height="20" viewBox="0 0 16 20" fill="none" style={{verticalAlign: 'middle', marginRight: 6}}>
          <path d="M8 1L2 4v4c0 4.4 2.6 8.2 6 9.2 3.4-1 6-4.8 6-9.2V4L8 1z" stroke="currentColor" strokeWidth="1.2" fill="none"/>
          <path d="M5.5 8l2 2 3.5-4" stroke="var(--primary)" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
        Введите текст с картинки
      </label>
      <div className="captcha-row">
        <div className="captcha-image-wrap">
          {loading ? (
            <div className="captcha-loading">
              <div className="spinner" style={{width: 20, height: 20}}></div>
            </div>
          ) : imageUrl ? (
            <img src={imageUrl} alt="Капча" className="captcha-image" draggable={false} />
          ) : (
            <span className="captcha-error-text">Ошибка</span>
          )}
        </div>
        <button type="button" className="captcha-refresh" onClick={loadCaptcha} title="Обновить капчу">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <path d="M15 3v4h-4M3 15v-4h4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M4.5 7a5 5 0 018.2-1.8L15 7M13.5 11a5 5 0 01-8.2 1.8L3 11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
        </button>
        <input
          type="text"
          className="form-control captcha-input"
          placeholder="Текст с картинки"
          value={code}
          onChange={handleChange}
          maxLength={6}
          autoComplete="off"
        />
      </div>
    </div>
  );
}
