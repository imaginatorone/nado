import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { adsAPI, categoriesAPI } from '../api/api';

export default function EditAdPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [status, setStatus] = useState('ACTIVE');
  const [categories, setCategories] = useState([]);
  const [errors, setErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      adsAPI.getById(id),
      categoriesAPI.getAll()
    ]).then(([adRes, catRes]) => {
      const ad = adRes.data;
      setTitle(ad.title);
      setDescription(ad.description);
      setPrice(ad.price || '');
      setCategoryId(ad.categoryId);
      setStatus(ad.status);
      setCategories(catRes.data);
    }).catch(() => {
      setGlobalError('Объявление не найдено');
    }).finally(() => setLoading(false));
  }, [id]);

  const flatCategories = [];
  categories.forEach(cat => {
    flatCategories.push({ id: cat.id, name: cat.name, level: 0 });
    if (cat.children) {
      cat.children.forEach(child => {
        flatCategories.push({ id: child.id, name: child.name, level: 1 });
      });
    }
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGlobalError('');
    const errs = {};
    if (!title.trim()) errs.title = 'Заголовок обязателен';
    if (!description.trim()) errs.description = 'Описание обязательно';
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;

    setSubmitting(true);
    try {
      await adsAPI.update(id, {
        title,
        description,
        price: price ? parseFloat(price) : null,
        categoryId: categoryId ? parseInt(categoryId) : null,
        status
      });
      navigate(`/ads/${id}`);
    } catch (err) {
      setGlobalError(err.response?.data?.error || 'Ошибка обновления');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="loading"><div className="spinner"></div></div>;

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      <div className="form-card" style={{ maxWidth: '640px' }}>
        <h1>Редактировать объявление</h1>

        {globalError && <div className="alert alert-error">{globalError}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Заголовок <span className="required">*</span></label>
            <input
              type="text"
              className={`form-control ${errors.title ? 'error' : ''}`}
              value={title}
              onChange={e => setTitle(e.target.value)}
              maxLength={255}
            />
            {errors.title && <div className="form-error">{errors.title}</div>}
          </div>

          <div className="form-group">
            <label>Категория</label>
            <select
              className="form-control"
              value={categoryId}
              onChange={e => setCategoryId(e.target.value)}
            >
              {flatCategories.map(cat => (
                <option key={cat.id} value={cat.id}>
                  {cat.level === 1 ? '  — ' : ''}{cat.name}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label>Описание <span className="required">*</span></label>
            <textarea
              className={`form-control ${errors.description ? 'error' : ''}`}
              value={description}
              onChange={e => setDescription(e.target.value)}
              rows={5}
            />
            {errors.description && <div className="form-error">{errors.description}</div>}
          </div>

          <div className="form-group">
            <label>Цена (₽)</label>
            <input
              type="number"
              className="form-control"
              value={price}
              onChange={e => setPrice(e.target.value)}
              min="0"
              step="0.01"
            />
          </div>

          <div className="form-group">
            <label>Статус</label>
            <select
              className="form-control"
              value={status}
              onChange={e => setStatus(e.target.value)}
            >
              <option value="ACTIVE">Активно</option>
              <option value="CLOSED">Закрыто</option>
            </select>
          </div>

          <button type="submit" className="btn btn-accent btn-lg" style={{ width: '100%' }} disabled={submitting}>
            {submitting ? 'Сохранение...' : 'Сохранить изменения'}
          </button>
        </form>
      </div>
    </motion.div>
  );
}
