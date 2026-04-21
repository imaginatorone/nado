import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { adsAPI, categoriesAPI, imagesAPI } from '../api/api';

export default function CreateAdPage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [region, setRegion] = useState(() => localStorage.getItem('nado_region') || '');
  const [categories, setCategories] = useState([]);
  const [files, setFiles] = useState([]);
  const [errors, setErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    categoriesAPI.getAll().then(res => setCategories(res.data)).catch(() => {});
  }, []);

  const flatCategories = [];
  categories.forEach(cat => {
    flatCategories.push({ id: cat.id, name: cat.name, level: 0 });
    if (cat.children) {
      cat.children.forEach(child => {
        flatCategories.push({ id: child.id, name: child.name, level: 1 });
      });
    }
  });

  const handleFileChange = (e) => {
    const newFiles = Array.from(e.target.files);
    setFiles(prev => [...prev, ...newFiles].slice(0, 10));
  };

  const removeFile = (idx) => {
    setFiles(prev => prev.filter((_, i) => i !== idx));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGlobalError('');
    const errs = {};
    if (!title.trim()) errs.title = 'Заголовок обязателен';
    if (!description.trim()) errs.description = 'Описание обязательно';
    if (!categoryId) errs.categoryId = 'Выберите категорию';
    if (files.length === 0) errs.files = 'Добавьте хотя бы одно фото';
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;

    setSubmitting(true);
    try {
      const res = await adsAPI.create({
        title,
        description,
        price: price ? parseFloat(price) : null,
        categoryId: parseInt(categoryId),
        region: region || null
      });

      const adId = res.data.id;

      // загрузка фотографий
      for (const file of files) {
        try {
          await imagesAPI.upload(adId, file);
        } catch {
          // пропускаем ошибку, грузим остальные
        }
      }

      navigate(`/ads/${adId}`);
    } catch (err) {
      setGlobalError(err.response?.data?.error || 'Ошибка создания объявления');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      <div className="form-card" style={{ maxWidth: '640px' }}>
        <h1>Подать объявление</h1>

        {globalError && <div className="alert alert-error">{globalError}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Заголовок <span className="required">*</span></label>
            <input
              type="text"
              className={`form-control ${errors.title ? 'error' : ''}`}
              placeholder="Например: Продам велосипед"
              value={title}
              onChange={e => setTitle(e.target.value)}
              maxLength={255}
            />
            {errors.title && <div className="form-error">{errors.title}</div>}
          </div>

          <div className="form-group">
            <label>Категория <span className="required">*</span></label>
            <select
              className={`form-control ${errors.categoryId ? 'error' : ''}`}
              value={categoryId}
              onChange={e => setCategoryId(e.target.value)}
            >
              <option value="">Выберите категорию</option>
              {flatCategories.map(cat => (
                <option key={cat.id} value={cat.id}>
                  {cat.level === 1 ? '  — ' : ''}{cat.name}
                </option>
              ))}
            </select>
            {errors.categoryId && <div className="form-error">{errors.categoryId}</div>}
          </div>

          <div className="form-group">
            <label>Описание <span className="required">*</span></label>
            <textarea
              className={`form-control ${errors.description ? 'error' : ''}`}
              placeholder="Подробно опишите товар или услугу"
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
              placeholder="0"
              value={price}
              onChange={e => setPrice(e.target.value)}
              min="0"
              step="0.01"
            />
          </div>

          <div className="form-group">
            <label>Регион</label>
            <select className="form-control" value={region} onChange={e => setRegion(e.target.value)}>
              <option value="">Не указан</option>
              {['Москва','Санкт-Петербург','Севастополь','Новосибирск','Екатеринбург','Казань','Нижний Новгород','Челябинск','Самара','Омск','Ростов-на-Дону','Уфа','Красноярск','Воронеж','Пермь','Волгоград'].map(r => (
                <option key={r} value={r}>{r}</option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label>Фотографии (до 10) <span className="required">*</span></label>
            <div
              className={`image-upload-zone ${errors.files ? 'error' : ''}`}
              onClick={() => document.getElementById('file-input').click()}
            >
              Нажмите для загрузки фото
            </div>
            <input
              id="file-input"
              type="file"
              accept="image/*"
              multiple
              style={{ display: 'none' }}
              onChange={handleFileChange}
            />
            {errors.files && <div className="form-error">{errors.files}</div>}

            {files.length > 0 && (
              <div className="image-preview-grid">
                {files.map((file, i) => (
                  <div key={i} className="image-preview-item">
                    <img src={URL.createObjectURL(file)} alt="" />
                    <button type="button" className="remove-btn" onClick={() => removeFile(i)}>✕</button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <button type="submit" className="btn btn-accent btn-lg" style={{ width: '100%' }} disabled={submitting}>
            {submitting ? 'Публикация...' : 'Опубликовать'}
          </button>
        </form>
      </div>
    </motion.div>
  );
}
