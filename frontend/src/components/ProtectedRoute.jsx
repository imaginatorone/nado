import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ children }) {
  const { isAuthenticated, loading, authMode, login } = useAuth();
  const location = useLocation();

  if (loading) {
    return <div className="loading"><div className="spinner"></div></div>;
  }

  if (!isAuthenticated) {
    if (authMode === 'keycloak') {
      // keycloak: redirect на KC login с возвратом на текущий URL
      login();
      return <div className="loading"><div className="spinner"></div></div>;
    }
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
}
