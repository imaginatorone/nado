import { Routes, Route, Navigate } from 'react-router-dom';
import Header from './components/Header';
import Footer from './components/Footer';
import BottomNav from './components/BottomNav';
import HomePage from './pages/HomePage';
import AdDetailPage from './pages/AdDetailPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import CreateAdPage from './pages/CreateAdPage';
import EditAdPage from './pages/EditAdPage';
import MyAdsPage from './pages/MyAdsPage';
import SearchPage from './pages/SearchPage';
import ProfilePage from './pages/ProfilePage';
import ProfileEditPage from './pages/ProfileEditPage';
import ChatPage from './pages/ChatPage';
import FavoritesPage from './pages/FavoritesPage';
import AdminPage from './pages/AdminPage';
import AdminPanel from './pages/AdminPanel';
import AdminDashboardPage from './pages/AdminDashboardPage';
import ModerationPage from './pages/ModerationPage';
import WantedPage from './pages/WantedPage';
import NotificationsPage from './pages/NotificationsPage';
import ProtectedRoute from './components/ProtectedRoute';
import { useAuth } from './context/AuthContext';
import { useState, useEffect } from 'react';
import { chatAPI } from './api/api';

function AdminRoute({ children }) {
  const { isAdmin, loading } = useAuth();
  if (loading) return <div className="loading"><div className="spinner"></div></div>;
  if (!isAdmin) return <Navigate to="/" replace />;
  return children;
}

function ModeratorRoute({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="loading"><div className="spinner"></div></div>;
  const role = user?.role;
  if (role !== 'ADMIN' && role !== 'MODERATOR') return <Navigate to="/" replace />;
  return children;
}

function App() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="app">
      <Header />
      <main className="main-content">
        <div className="container">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/ads/:id" element={<AdDetailPage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/ads/new" element={<ProtectedRoute><CreateAdPage /></ProtectedRoute>} />
            <Route path="/ads/:id/edit" element={<ProtectedRoute><EditAdPage /></ProtectedRoute>} />
            <Route path="/my-ads" element={<ProtectedRoute><MyAdsPage /></ProtectedRoute>} />
            <Route path="/profile/:userId" element={<ProfilePage />} />
            <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
            <Route path="/profile/edit" element={<ProtectedRoute><ProfileEditPage /></ProtectedRoute>} />
            <Route path="/chats" element={<ProtectedRoute><ChatPage /></ProtectedRoute>} />
            <Route path="/favorites" element={<ProtectedRoute><FavoritesPage /></ProtectedRoute>} />
            <Route path="/wanted" element={<ProtectedRoute><WantedPage /></ProtectedRoute>} />
            <Route path="/notifications" element={<ProtectedRoute><NotificationsPage /></ProtectedRoute>} />
            <Route path="/moderation" element={<ModeratorRoute><ModerationPage /></ModeratorRoute>} />
            <Route path="/admin" element={<AdminRoute><AdminPage /></AdminRoute>} />
            <Route path="/admin/dashboard" element={<AdminRoute><AdminDashboardPage /></AdminRoute>} />
            <Route path="/nado-control" element={<AdminPanel />} />
          </Routes>
        </div>
      </main>
      <Footer />
      <BottomNav />
    </div>
  );
}

export default App;
