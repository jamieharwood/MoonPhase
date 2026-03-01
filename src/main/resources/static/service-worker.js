// MoonPhaseAI Service Worker
// Satisfies Chrome's PWA installability requirement.
// Uses a network-first strategy so the live SSE data is never stale.

var CACHE = 'moonphaseai-v1';
var PRECACHE = [
  '/',
  '/index.html',
  '/style.css',
  '/app.js',
  '/manifest.json',
  '/favicon.ico',
  '/apple-touch-icon.png',
  '/icon-192.png',
  '/icon-512.png',
  '/dockerhub-qr.svg'
];

self.addEventListener('install', function (e) {
  e.waitUntil(
    caches.open(CACHE).then(function (cache) {
      return cache.addAll(PRECACHE);
    })
  );
  self.skipWaiting();
});

self.addEventListener('activate', function (e) {
  e.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(
        keys.filter(function (k) { return k !== CACHE; })
            .map(function (k) { return caches.delete(k); })
      );
    })
  );
  self.clients.claim();
});

// Network-first: always try the network; fall back to cache for static assets.
// API endpoints (/api/*) are never served from cache.
self.addEventListener('fetch', function (e) {
  var url = e.request.url;

  // Never cache SSE stream or JSON API
  if (url.includes('/api/')) {
    e.respondWith(fetch(e.request));
    return;
  }

  e.respondWith(
    fetch(e.request)
      .then(function (response) {
        var copy = response.clone();
        caches.open(CACHE).then(function (cache) { cache.put(e.request, copy); });
        return response;
      })
      .catch(function () {
        return caches.match(e.request);
      })
  );
});
