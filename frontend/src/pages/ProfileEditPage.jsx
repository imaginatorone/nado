import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { usersAPI } from '../api/api';

export default function ProfileEditPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const fileRef = useRef(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [form, setForm] = useState({ name: '', phone: '', region: '' });
  const [avatarUrl, setAvatarUrl] = useState(null);

  useEffect(() => {
    usersAPI.getMe()
      .then(res => {
        const p = res.data;
        setForm({ name: p.name || '', phone: p.phone || '', region: p.region || '' });
        setAvatarUrl(p.avatarUrl);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const handleChange = (field) => (e) => {
    setForm(prev => ({ ...prev, [field]: e.target.value }));
  };

  const handleAvatarClick = () => fileRef.current?.click();

  const handleAvatarUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      showToast('Максимальный размер файла — 5 МБ', 'error');
      return;
    }
    setUploading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await usersAPI.uploadAvatar(formData);
      setAvatarUrl(res.data.avatarUrl);
      showToast('Фото профиля обновлено', 'success');
    } catch {
      showToast('Не удалось загрузить фото', 'error');
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.name.trim()) {
      showToast('Имя не может быть пустым', 'error');
      return;
    }
    setSaving(true);
    try {
      await usersAPI.updateMe(form);
      showToast('Профиль обновлён', 'success');
      navigate('/profile');
    } catch {
      showToast('Ошибка при сохранении', 'error');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="loading"><div className="spinner"></div></div>;

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="profile-edit-page"
    >
      <div className="profile-edit-card">
        <div className="profile-edit-header">
          <div className="profile-avatar-upload" onClick={handleAvatarClick}>
            {avatarUrl ? (
              <img src={avatarUrl} alt="Аватар" className="profile-avatar-img" />
            ) : (
              <span className="profile-avatar-letter">{form.name?.[0]?.toUpperCase() || '?'}</span>
            )}
            <div className="profile-avatar-overlay">
              {uploading ? '...' : '+'}
            </div>
            <input
              ref={fileRef}
              type="file"
              accept="image/*"
              onChange={handleAvatarUpload}
              hidden
            />
          </div>
          <h1>Редактирование профиля</h1>
        </div>

        <form onSubmit={handleSubmit} className="profile-edit-form">
          <div className="form-group">
            <label htmlFor="edit-name">
              <span className="form-label-icon">👤</span> Имя
            </label>
            <input
              id="edit-name"
              type="text"
              className="form-control"
              value={form.name}
              onChange={handleChange('name')}
              placeholder="Ваше имя"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="edit-phone">
              Телефон
            </label>
            <input
              id="edit-phone"
              type="tel"
              className="form-control"
              value={form.phone}
              onChange={handleChange('phone')}
              placeholder="+7 (999) 123-45-67"
            />
          </div>

          <div className="form-group">
            <label htmlFor="edit-region">
              Регион
            </label>
            <input
              id="edit-region"
              type="text"
              className="form-control"
              value={form.region}
              onChange={handleChange('region')}
              placeholder="Например: Москва"
            />
          </div>

          <div className="profile-edit-actions">
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Сохранение...' : 'Сохранить'}
            </button>
            <button type="button" className="btn btn-secondary" onClick={() => navigate('/profile')}>
              Отмена
            </button>
          </div>
        </form>
      </div>
    </motion.div>
  );
}
