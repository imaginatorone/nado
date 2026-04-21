import { motion } from 'framer-motion';

function WarningIcon() {
  return (
    <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
      <circle cx="24" cy="24" r="22" stroke="var(--text-muted)" strokeWidth="2" strokeDasharray="4 3" opacity="0.3"/>
      <path d="M24 14v12M24 30v2" stroke="var(--text-muted)" strokeWidth="3" strokeLinecap="round"/>
    </svg>
  );
}

export default function ErrorState({ message, onRetry }) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="error-state"
    >
      <div className="error-state-icon"><WarningIcon /></div>
      <h3>{message || 'Не удалось загрузить данные'}</h3>
      <p>Проверьте подключение к интернету и попробуйте снова</p>
      {onRetry && (
        <button className="btn btn-primary" onClick={onRetry}>
          <svg width="14" height="14" viewBox="0 0 18 18" fill="none" style={{marginRight: 6, verticalAlign: 'middle'}}>
            <path d="M15 3v4h-4M3 15v-4h4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M4.5 7a5 5 0 018.2-1.8L15 7M13.5 11a5 5 0 01-8.2 1.8L3 11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          Попробовать снова
        </button>
      )}
    </motion.div>
  );
}
