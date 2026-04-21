import { createContext, useContext, useState, useEffect } from 'react';

const ThemeContext = createContext();

export function useTheme() {
  return useContext(ThemeContext);
}

function getSystemTheme() {
  if (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches) {
    return 'light';
  }
  return 'dark';
}

function getSavedTheme() {
  const saved = localStorage.getItem('nado-theme');
  if (saved === 'light' || saved === 'dark') return saved;
  return null;
}

export function ThemeProvider({ children }) {
  const [themePreference, setThemePreference] = useState(() => getSavedTheme());
  const [systemTheme, setSystemTheme] = useState(getSystemTheme);

  const activeTheme = themePreference || systemTheme;

  // отслеживание системной темы
  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: light)');
    const handler = (e) => setSystemTheme(e.matches ? 'light' : 'dark');
    mediaQuery.addEventListener('change', handler);
    return () => mediaQuery.removeEventListener('change', handler);
  }, []);

  // применение темы к документу
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', activeTheme);
  }, [activeTheme]);

  const setTheme = (theme) => {
    if (theme === 'system') {
      localStorage.removeItem('nado-theme');
      setThemePreference(null);
    } else {
      localStorage.setItem('nado-theme', theme);
      setThemePreference(theme);
    }
  };

  const toggleTheme = () => {
    setTheme(activeTheme === 'dark' ? 'light' : 'dark');
  };

  return (
    <ThemeContext.Provider value={{ theme: activeTheme, themePreference, setTheme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}
