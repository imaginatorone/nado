// service worker — минимально надёжный shell.
// кешируем только статику; API и auth проходят напрямую,
// чтобы не ломать auth-sensitive сценарии в PWA standalone.
const CACHE_NAME = 'nado-v3';
const STATIC_ASSETS = ['/', '/index.html', '/manifest.json', '/favicon.png'];

// пути, которые SW никогда не перехватывает
const BYPASS_PATTERNS = [
  '/api/',
  '/auth/',
  '/realms/',         // keycloak realm endpoints
  '/silent-check-sso',
  '/oauth2/',
  '/login',
  '/logout',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(STATIC_ASSETS))
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    )
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // не перехватываем: не-GET, кросс-доменные, auth/api
  if (request.method !== 'GET') return;
  if (url.origin !== self.location.origin) return;
  if (BYPASS_PATTERNS.some(p => url.pathname.startsWith(p))) return;

  // статика: cache-first, network-fallback
  event.respondWith(
    caches.match(request).then((cached) => {
      if (cached) return cached;
      return fetch(request).then((response) => {
        // кешируем только статические ресурсы (js/css/fonts/images)
        if (response.ok && response.type === 'basic' && isStaticAsset(url.pathname)) {
          const clone = response.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(request, clone));
        }
        return response;
      });
    }).catch(() => {
      // SPA-фоллбек: deep link reload → отдаём index.html
      if (request.mode === 'navigate') {
        return caches.match('/index.html');
      }
    })
  );
});

function isStaticAsset(pathname) {
  return /\.(js|css|woff2?|png|jpg|svg|ico|webp)$/.test(pathname);
}
