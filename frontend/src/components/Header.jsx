import { Link, useNavigate } from 'react-router-dom';
import { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { categoriesAPI, chatAPI, adsAPI, notificationsAPI } from '../api/api';
import { IconChat, IconHeart, IconPackage, IconShield, IconSearch, IconPlus, IconUser } from './Icons';

const REGIONS = [
  'Москва', 'Санкт-Петербург', 'Севастополь', 'Новосибирск', 'Екатеринбург', 'Казань',
  'Нижний Новгород', 'Челябинск', 'Самара', 'Омск', 'Ростов-на-Дону',
  'Уфа', 'Красноярск', 'Воронеж', 'Пермь', 'Волгоград',
];

const TIPS = [
  'Кто ищет — тот находит',
  'Хорошее фото = быстрая продажа',
  'Адекватная цена — мгновенная сделка',
  'Только то, что надо',
  'Мы не Авито. Мы — Nado',
  'Вещи находят новых хозяев',
];

export default function Header() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const headerRef = useRef(null);

  const [categories, setCategories] = useState([]);
  const [catOpen, setCatOpen] = useState(false);
  const [selectedCat, setSelectedCat] = useState(null);
  const catRef = useRef(null);

  const [regionOpen, setRegionOpen] = useState(false);
  const [selectedRegion, setSelectedRegion] = useState(
    () => localStorage.getItem('nado_region') || ''
  );
  const regionRef = useRef(null);

  const [query, setQuery] = useState('');
  const [focused, setFocused] = useState(false);
  const [suggestions, setSuggestions] = useState([]);
  const [titleOnly, setTitleOnly] = useState(false);
  const [withPhoto, setWithPhoto] = useState(false);
  const searchTimeout = useRef(null);

  // меню пользователя
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifCount, setNotifCount] = useState(0);

  // мобильный поиск
  const [mobileSearchOpen, setMobileSearchOpen] = useState(false);

  const tip = useMemo(() => TIPS[Math.abs(Math.floor(Date.now() / 3600000) * 31) % TIPS.length], []);

  useEffect(() => {
    categoriesAPI.getAll().then(r => setCategories(r.data || [])).catch(() => {});
  }, []);

  // динамическая высота шапки для фиксированного отступа
  useEffect(() => {
    const el = headerRef.current;
    if (!el) return;
    const sync = () => document.documentElement.style.setProperty('--header-h', el.offsetHeight + 'px');
    sync();
    const ro = new ResizeObserver(sync);
    ro.observe(el);
    return () => ro.disconnect();
  }, [mobileSearchOpen]);

  // polling непрочитанных сообщений + уведомлений
  useEffect(() => {
    if (!isAuthenticated) return;
    const load = () => {
      chatAPI.getUnreadCount().then(r => setUnreadCount(r.data?.count || 0)).catch(() => {});
      notificationsAPI.getUnreadCount().then(r => setNotifCount(r.data?.count || 0)).catch(() => {});
    };
    load();
    const iv = setInterval(load, 15000);
    return () => clearInterval(iv);
  }, [isAuthenticated]);

  // закрытие выпадающих списков при клике снаружи
  useEffect(() => {
    const handler = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) setMenuOpen(false);
      if (catRef.current && !catRef.current.contains(e.target)) setCatOpen(false);
      if (regionRef.current && !regionRef.current.contains(e.target)) setRegionOpen(false);
    };
    document.addEventListener('click', handler);
    return () => document.removeEventListener('click', handler);
  }, []);

  // живые подсказки
  const doSuggest = useCallback((q) => {
    if (q.length < 2) { setSuggestions([]); return; }
    adsAPI.search({ query: q, page: 0, size: 5 })
      .then(r => setSuggestions((r.data?.content || []).map(a => ({ id: a.id, title: a.title, price: a.price }))))
      .catch(() => setSuggestions([]));
  }, []);

  const handleQueryChange = (e) => {
    const v = e.target.value;
    setQuery(v);
    clearTimeout(searchTimeout.current);
    searchTimeout.current = setTimeout(() => doSuggest(v.trim()), 300);
  };

  const handleSearch = (e) => {
    e.preventDefault();
    setSuggestions([]);
    setFocused(false);
    setMobileSearchOpen(false);
    const p = new URLSearchParams();
    if (query.trim()) p.set('query', query.trim());
    if (selectedCat) p.set('categoryId', selectedCat.id);
    if (selectedRegion) p.set('region', selectedRegion);
    if (titleOnly) p.set('titleOnly', 'true');
    if (withPhoto) p.set('withPhoto', 'true');
    navigate(`/search?${p.toString()}`);
  };

  const handleLogout = () => { setMenuOpen(false); logout(); navigate('/'); };

  const handleRegionSelect = (r) => {
    setSelectedRegion(r);
    setRegionOpen(false);
    if (r) localStorage.setItem('nado_region', r);
    else localStorage.removeItem('nado_region');
  };

  // плоский список категорий
  const flatCats = useMemo(() => {
    const out = [];
    (categories || []).forEach(c => {
      out.push({ ...c, depth: 0 });
      (c.children || []).forEach(ch => out.push({ ...ch, depth: 1 }));
    });
    return out;
  }, [categories]);

  // корневые категории для навигации
  const topCats = useMemo(() => (categories || []).slice(0, 5), [categories]);
  const hasMoreCats = (categories || []).length > 5;

  // подсказка категории из поиска
  const suggestedCat = useMemo(() => {
    if (!query.trim() || query.length < 2) return null;
    const q = query.toLowerCase();
    return flatCats.find(c => c.name.toLowerCase().includes(q)) || null;
  }, [query, flatCats]);

  const showSuggestions = focused && query.length >= 2 && (suggestions.length > 0 || suggestedCat);

  return (
    <header className="header" ref={headerRef}>
      <div className="container">

        {/* строка 1: логотип + навигация + авторизация */}
        <div className="h-row1">
          <Link to="/" className="header-logo">
            <img src="/logo.png" alt="Nado" className="header-logo-icon" style={{ width: 32, height: 'auto' }} />
            <span className="header-logo-text">
              <span className="header-logo-name">Nado</span>
              <span className="header-logo-dot">.</span>
            </span>
          </Link>

          {/* навигация по категориям */}
          <nav className="h-cat-nav">
            {topCats.map(c => (
              <Link
                key={c.id}
                to={`/search?categoryId=${c.id}`}
                className="h-cat-nav-link"
              >
                {c.name}
              </Link>
            ))}
            {hasMoreCats && (
              <button
                className="h-cat-nav-link h-cat-nav-more"
                type="button"
                onClick={(e) => { e.stopPropagation(); setCatOpen(!catOpen); }}
              >
                ещё..
              </button>
            )}
          </nav>

          {/* блок авторизации и действий */}
          <div className="h-actions">
            {isAuthenticated ? (
              <>
                <Link to="/ads/new" className="btn btn-accent btn-sm h-btn-post">
                  <IconPlus size={14} /> <span className="hide-mobile">Подать объявление</span>
                </Link>
                <Link to="/chats" className="h-icon-btn" title="Сообщения">
                  <IconChat size={18} />
                  {unreadCount > 0 && <span className="h-badge">{unreadCount}</span>}
                </Link>
                <Link to="/favorites" className="h-icon-btn hide-mobile-icon" title="Избранное">
                  <IconHeart size={18} />
                </Link>
                <Link to="/notifications" className="h-icon-btn" title="Уведомления">
                  🔔
                  {notifCount > 0 && <span className="h-badge">{notifCount}</span>}
                </Link>
                <div className="h-user-menu" ref={menuRef}>
                  <button className="h-icon-btn" onClick={(e) => { e.stopPropagation(); setMenuOpen(!menuOpen); }}>
                    <IconUser size={18} />
                  </button>
                  {menuOpen && (
                    <div className="h-dropdown">
                      <div className="h-dropdown-header">
                        <strong>{user?.name}</strong>
                        <span>{user?.email}</span>
                      </div>
                      <Link to="/profile" onClick={() => setMenuOpen(false)}><IconUser size={14} /> Профиль</Link>
                      <Link to="/chats" onClick={() => setMenuOpen(false)}><IconChat size={14} /> Сообщения{unreadCount > 0 && ` (${unreadCount})`}</Link>
                      <Link to="/favorites" onClick={() => setMenuOpen(false)}><IconHeart size={14} /> Избранное</Link>
                      <Link to="/my-ads" onClick={() => setMenuOpen(false)}><IconPackage size={14} /> Мои объявления</Link>
                      <Link to="/wanted" onClick={() => setMenuOpen(false)}>🔍 Хочу купить</Link>
                      {(user?.role === 'ADMIN' || user?.role === 'MODERATOR') && <Link to="/moderation" onClick={() => setMenuOpen(false)}><IconShield size={14} /> Модерация</Link>}
                      <button className="h-dropdown-logout" onClick={handleLogout}>Выйти</button>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <>
                <Link to="/login" className="h-auth-link">
                  <span className="hide-mobile">Вход и регистрация</span>
                  <span className="show-mobile">Войти</span>
                </Link>
                <Link to="/ads/new" className="btn btn-accent btn-sm h-btn-post">
                  <IconPlus size={14} /> <span className="hide-mobile">Подать объявление</span>
                </Link>
              </>
            )}

            {/* кнопка поиска (мобильная) */}
            <button
              className="h-mobile-search-toggle show-mobile"
              type="button"
              onClick={() => setMobileSearchOpen(!mobileSearchOpen)}
              aria-label="Поиск"
            >
              <IconSearch size={18} />
            </button>
          </div>
        </div>

        {/* строка 2: панель поиска */}
        <form
          className={`h-search ${mobileSearchOpen ? 'h-search--mobile-open' : ''}`}
          onSubmit={handleSearch}
        >
          <div className="h-search-row">
            {/* выпадающий список категорий */}
            <div className="h-cat" ref={catRef}>
              <button type="button" className="h-cat-btn" onClick={() => setCatOpen(!catOpen)}>
                {selectedCat ? selectedCat.name : 'Категория'}
                <svg width="10" height="6" viewBox="0 0 10 6" fill="currentColor"><path d="M1 1l4 4 4-4"/></svg>
              </button>
              {catOpen && (
                <div className="h-cat-menu">
                  <button type="button" className={`h-cat-opt ${!selectedCat ? 'active' : ''}`} onClick={() => { setSelectedCat(null); setCatOpen(false); }}>Любая категория</button>
                  {flatCats.map(c => (
                    <button type="button" key={c.id}
                      className={`h-cat-opt ${c.depth ? 'sub' : ''} ${selectedCat?.id === c.id ? 'active' : ''}`}
                      onClick={() => { setSelectedCat(c); setCatOpen(false); }}>
                      {c.name}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* поле поиска */}
            <div className="h-input-wrap">
              <input
                type="text"
                className="h-input"
                placeholder="Поиск по объявлениям"
                value={query}
                onChange={handleQueryChange}
                onFocus={() => setFocused(true)}
                onBlur={() => setTimeout(() => setFocused(false), 250)}
              />
            </div>

            {/* выпадающий список регионов */}
            <div className="h-region" ref={regionRef}>
              <button type="button" className="h-region-btn" onClick={() => setRegionOpen(!regionOpen)}>
                {selectedRegion || 'Регион'}
                <svg width="10" height="6" viewBox="0 0 10 6" fill="currentColor"><path d="M1 1l4 4 4-4"/></svg>
              </button>
              {regionOpen && (
                <div className="h-region-menu">
                  <button type="button" className={`h-region-opt ${!selectedRegion ? 'active' : ''}`} onClick={() => handleRegionSelect('')}>Любой регион</button>
                  {REGIONS.map(r => (
                    <button type="button" key={r}
                      className={`h-region-opt ${selectedRegion === r ? 'active' : ''}`}
                      onClick={() => handleRegionSelect(r)}>
                      {r}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* кнопка «Найти» */}
            <button type="submit" className="h-find-btn">
              <IconSearch size={16} />
              <span>Найти</span>
            </button>
          </div>

          {/* строка 3: фильтры */}
          <div className="h-filters">
            <label><input type="checkbox" checked={titleOnly} onChange={e => setTitleOnly(e.target.checked)} /> Только в названиях</label>
            <label><input type="checkbox" checked={withPhoto} onChange={e => setWithPhoto(e.target.checked)} /> Только с фото</label>
            <span className="h-tip">{tip}</span>
          </div>

          {/* живые подсказки */}
          {showSuggestions && (
            <div className="h-suggestions">
              {suggestedCat && (
                <button type="button" className="h-sug h-sug--cat" onClick={() => { setSelectedCat(suggestedCat); setSuggestions([]); setFocused(false); }}>
                  <IconSearch size={14} /> Категория: <strong>{suggestedCat.name}</strong>
                </button>
              )}
              {suggestions.map(s => (
                <Link key={s.id} to={`/ads/${s.id}`} className="h-sug" onClick={() => { setFocused(false); setSuggestions([]); }}>
                  <IconSearch size={14} />
                  <span className="h-sug-title">{s.title}</span>
                  {s.price != null && <span className="h-sug-price">{Number(s.price).toLocaleString('ru-RU')} ₽</span>}
                </Link>
              ))}
              {isAuthenticated && query.length >= 2 && (
                <button type="button" className="h-sug h-sug--want" onClick={() => { navigate(`/want-to-buy?query=${encodeURIComponent(query)}`); setSuggestions([]); setFocused(false); }}>
                  <IconHeart size={14} /> Хочу купить: «{query}»
                </button>
              )}
            </div>
          )}
        </form>
      </div>
    </header>
  );
}
