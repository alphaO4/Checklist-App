
/**
 * Service Worker for Feuerwehr Checklist App
 * Provides basic offline functionality
 */

const CACHE_NAME = 'feuerwehr-checklist-v1756609343366';
const urlsToCache = [
  '/',
  '/index.html',
  '/config.js',
  '/web-api-adapter.js?v=1756609343366',
  '/js/renderer.js',
  '/styles/main.css',
  '/components/',
  '/assets/icon-192.png'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(urlsToCache))
  );
});

self.addEventListener('fetch', event => {
  event.respondWith(
    caches.match(event.request)
      .then(response => {
        // Return cached version or fetch from network
        return response || fetch(event.request);
      })
  );
});
