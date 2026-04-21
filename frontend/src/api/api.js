import axios from 'axios';
import { getKeycloak } from '../auth/keycloak';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// подстановка токена: keycloak token или legacy localStorage
api.interceptors.request.use(async (config) => {
  const kc = getKeycloak();

  if (kc?.authenticated) {
    // обновляем токен если до истечения < 15 сек
    try { await kc.updateToken(15); } catch { /* ignore */ }
    config.headers.Authorization = `Bearer ${kc.token}`;
  } else {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }

  return config;
});

// при 401 — очистка состояния
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      const kc = getKeycloak();
      if (kc?.authenticated) {
        // keycloak mode — перенаправляем на логин
        kc.login({ redirectUri: window.location.origin + '/' });
      } else {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(err);
  }
);

export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
};

export const adsAPI = {
  getAll: (page = 0, size = 20) => api.get(`/ads?page=${page}&size=${size}`),
  getById: (id) => api.get(`/ads/${id}`),
  search: (params) => api.get('/ads/search', { params }),
  create: (data) => api.post('/ads', data),
  update: (id, data) => api.put(`/ads/${id}`, data),
  delete: (id) => api.delete(`/ads/${id}`),
  getMy: (page = 0, size = 20) => api.get(`/ads/my?page=${page}&size=${size}`),
  getMyCabinet: (page = 0, size = 20, status) => {
    const params = new URLSearchParams({ page, size });
    if (status) params.set('status', status);
    return api.get(`/ads/my/cabinet?${params}`);
  },
  getSellerPhone: (adId) => api.get(`/ads/${adId}/phone`),
};

export const categoriesAPI = {
  getAll: () => api.get('/categories'),
};

export const commentsAPI = {
  getByAdId: (adId, page = 0, size = 20) =>
    api.get(`/ads/${adId}/comments?page=${page}&size=${size}`),
  create: (adId, data) => api.post(`/ads/${adId}/comments`, data),
};

export const imagesAPI = {
  upload: (adId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`/ads/${adId}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  delete: (id) => api.delete(`/images/${id}`),
  getUrl: (id) => `/api/images/${id}`,
};

export const usersAPI = {
  getMe: () => api.get('/users/me'),
  getById: (id) => api.get(`/users/${id}`),
  updateMe: (data) => api.put('/users/me', data),
  uploadAvatar: (formData) => api.post('/users/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
};

export const chatAPI = {
  getMyChats: () => api.get('/chats'),
  startChat: (adId) => api.post('/chats/start', { adId }),
  getMessages: (roomId) => api.get(`/chats/${roomId}/messages`),
  sendMessage: (roomId, content, file) => {
    const formData = new FormData();
    if (content) formData.append('content', content);
    if (file) formData.append('file', file);
    return api.post(`/chats/${roomId}/messages`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  markAsRead: (roomId) => api.put(`/chats/${roomId}/read`),
  getUnreadCount: () => api.get('/chats/unread'),
  getFileUrl: (messageId) => `/api/chats/files/${messageId}`,
};

export const ratingsAPI = {
  getSellerRatings: (sellerId) => api.get(`/ratings/seller/${sellerId}`),
  getSellerProfile: (sellerId) => api.get(`/ratings/seller/${sellerId}/profile`),
  createRating: (sellerId, data) => api.post(`/ratings/seller/${sellerId}`, data),
  getBySeller: (sellerId) => api.get(`/ratings/seller/${sellerId}`),
};

export const favoritesAPI = {
  toggle: (adId) => api.post(`/favorites/${adId}`),
  check: (adId) => api.get(`/favorites/${adId}/check`),
  getMyFavorites: (page = 0, size = 20) => api.get(`/favorites?page=${page}&size=${size}`),
};

export const auctionsAPI = {
  create: (data) => api.post('/auctions', data),
  getByAdId: (adId) => api.get(`/auctions/ad/${adId}`),
  getActive: () => api.get('/auctions/active'),
  placeBid: (auctionId, data) => api.post(`/auctions/${auctionId}/bid`, data),
  cancel: (auctionId) => api.post(`/auctions/${auctionId}/cancel`),
  extend: (auctionId, endsAt) => api.post(`/auctions/${auctionId}/extend`, { endsAt }),
};

export const adminAPI = {
  getStats: () => api.get('/admin/stats'),
  getAllAds: (page = 0, size = 20) => api.get(`/admin/ads?page=${page}&size=${size}`),
  getAllUsers: () => api.get('/admin/users'),
  deleteAd: (id) => api.delete(`/admin/ads/${id}`),
  toggleUser: (id) => api.put(`/admin/users/${id}/toggle-active`),
};

export const wantToBuyAPI = {
  create: (data) => api.post('/want-to-buy', data),
  getMy: () => api.get('/want-to-buy'),
  deactivate: (id) => api.put(`/want-to-buy/${id}/deactivate`),
  delete: (id) => api.delete(`/want-to-buy/${id}`),
  getMatches: (id) => api.get(`/want-to-buy/${id}/matches`),
  markSeen: (id) => api.post(`/want-to-buy/${id}/matches/seen`),
  getUnseenCount: () => api.get('/want-to-buy/unseen-count'),
};

export const trustAPI = {
  getRating: (userId) => api.get(`/trust/${userId}`),
};

export const moderationAPI = {
  getPending: (page = 0, size = 20) => api.get(`/moderation/pending?page=${page}&size=${size}`),
  approve: (adId) => api.post(`/moderation/${adId}/approve`),
  reject: (adId, reason) => api.post(`/moderation/${adId}/reject`, { reason }),
  block: (adId, reason) => api.post(`/moderation/${adId}/block`, { reason }),
  getCount: () => api.get('/moderation/count'),
};

export const phoneVerificationAPI = {
  requestCode: (phone) => api.post('/phone-verification/request', { phone }),
  verifyCode: (phone, code) => api.post('/phone-verification/verify', { phone, code }),
};

export const notificationsAPI = {
  getList: (page = 0, size = 20) => api.get(`/notifications?page=${page}&size=${size}`),
  getUnreadCount: () => api.get('/notifications/unread-count'),
  markAsRead: (id) => api.post(`/notifications/${id}/read`),
  markAllAsRead: () => api.post('/notifications/read-all'),
};

export default api;
