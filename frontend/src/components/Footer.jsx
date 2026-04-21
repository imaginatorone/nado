import { Link } from 'react-router-dom';
import { useTheme } from '../context/ThemeContext';
import { IconSun, IconMoon } from './Icons';

export default function Footer() {
  const year = new Date().getFullYear();
  const { theme, toggleTheme } = useTheme();

  return (
    <footer className="footer">
      <div className="container">
        <div className="footer-content">
          <div className="footer-left">
            <div className="footer-copyright">
              © {year} Nado.ru
            </div>
          </div>

          <div className="footer-center">
            <div className="footer-slogan">Только то, что надо — и ничего лишнего</div>
            <div className="footer-nav">
              <Link to="/">Главная</Link>
              <Link to="/search">Поиск</Link>
              <Link to="/ads/new">Подать объявление</Link>
              <Link to="/chats">Сообщения</Link>
            </div>
          </div>

          <div className="footer-right">
            <div className="footer-links">
              <a href="https://vk.com" target="_blank" rel="noopener noreferrer">VK</a>
              <a href="https://t.me" target="_blank" rel="noopener noreferrer">Telegram</a>
              <a href="mailto:support@nado.ru">Поддержка</a>
            </div>
          </div>
        </div>

        <div className="footer-bottom">
          <button
            className="footer-theme-toggle"
            onClick={toggleTheme}
            aria-label="Переключить тему"
          >
            <span className={`theme-icon ${theme}`}>
              <span className="theme-sun"><IconSun size={16} /></span>
              <span className="theme-moon"><IconMoon size={16} /></span>
            </span>
            <span className="theme-label">
              {theme === 'dark' ? 'Светлая сторона' : 'Тёмная сторона'}
            </span>
          </button>
        </div>
      </div>
    </footer>
  );
}
