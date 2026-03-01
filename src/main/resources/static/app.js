(function () {
  'use strict';

  // ── DOM helpers ──────────────────────────────────────────────────────────
  function setText(id, value) {
    var el = document.getElementById(id);
    if (el) el.textContent = value;
  }

  function flashCard(id) {
    var card = document.getElementById(id);
    if (!card) return;
    card.classList.remove('updated');
    // Force reflow so the animation restarts
    void card.offsetWidth;
    card.classList.add('updated');
  }

  // ── Number formatters ─────────────────────────────────────────────────────
  function fmtAu(val) {
    return (typeof val === 'number') ? val.toFixed(6) + ' AU' : '—';
  }

  function fmtKm(val) {
    return (typeof val === 'number') ? val.toLocaleString(undefined, { maximumFractionDigits: 0 }) + ' km' : '—';
  }

  function fmtKmPerSec(val) {
    return (typeof val === 'number') ? val.toFixed(2) + ' km/s' : '—';
  }

  function fmtKmPerHour(val) {
    return (typeof val === 'number') ? val.toLocaleString(undefined, { maximumFractionDigits: 0 }) + ' km/h' : '—';
  }

  function fmtHours(val) {
    return (typeof val === 'number') ? val.toFixed(1) + ' hrs' : '—';
  }

  function fmtDays(val) {
    return (val !== null && val !== undefined) ? val + ' days' : '—';
  }

  // ── Apply snapshot to DOM ─────────────────────────────────────────────────
  function applySnapshot(data) {
    // Moon
    setText('moon-phase-name',  data.moonPhaseName || '—');
    setText('moon-illumination', data.moonIlluminationPercent != null ? data.moonIlluminationPercent + '%' : '—');
    setText('moon-age',         data.moonAgeDays != null ? data.moonAgeDays + ' days' : '—');
    setText('moon-full-in',     data.daysUntilFullMoon != null ? data.daysUntilFullMoon + ' days' : '—');
    setText('moon-distance',    fmtKm(data.moonDistanceKm));

    var asciiEl = document.getElementById('moon-ascii');
    if (asciiEl && Array.isArray(data.moonAsciiArt)) {
      asciiEl.textContent = data.moonAsciiArt.join('\n');
    }

    // Solar System Distances
    setText('dist-sun',     fmtAu(data.sunDistanceAu));
    setText('dist-mars',    fmtAu(data.marsDistanceAu));
    setText('dist-jupiter', fmtAu(data.jupiterDistanceAu));
    setText('dist-saturn',  fmtAu(data.saturnDistanceAu));

    // Probes
    setText('probe-v1', fmtAu(data.voyager1DistanceAu));
    setText('probe-v2', fmtAu(data.voyager2DistanceAu));
    setText('probe-nh', fmtAu(data.newHorizonsDistanceAu));

    // Earth
    setText('earth-speed-s',  fmtKmPerSec(data.earthSpeedKmPerSec));
    setText('earth-speed-h',  fmtKmPerHour(data.earthSpeedKmPerHour));
    setText('earth-daylight', fmtHours(data.daylightHours));

    // Light travel times
    setText('light-sun',     data.lightTimeSunToEarth    || '—');
    setText('light-mars',    data.lightTimeEarthToMars   || '—');
    setText('light-jupiter', data.lightTimeEarthToJupiter|| '—');
    setText('light-saturn',  data.lightTimeEarthToSaturn || '—');
    setText('light-v1',      data.lightTimeEarthToVoyager1 || '—');
    setText('light-v2',      data.lightTimeEarthToVoyager2 || '—');

    // Events
    setText('event-summer',     fmtDays(data.daysUntilSummerSolstice));
    setText('event-winter',     fmtDays(data.daysUntilWinterSolstice));
    setText('event-perihelion', fmtDays(data.daysUntilPerihelion));
    setText('event-aphelion',   fmtDays(data.daysUntilAphelion));

    // Last updated
    setText('last-updated', data.lastUpdated ? 'Updated ' + data.lastUpdated + ' UTC' : '');

    // Flash all cards
    ['card-moon', 'card-solar', 'card-probes', 'card-earth', 'card-light', 'card-events']
      .forEach(flashCard);
  }

  // ── SSE dot helpers ───────────────────────────────────────────────────────
  function setDot(state) {
    var dot = document.getElementById('sse-dot');
    if (!dot) return;
    dot.className = 'sse-dot ' + state;
    var labels = { connected: 'Live — connected', disconnected: 'Disconnected', connecting: 'Connecting…' };
    dot.title = labels[state] || state;
  }

  // ── Load initial data (HTTP GET) ──────────────────────────────────────────
  function loadInitialData() {
    fetch('/api/data')
      .then(function (res) {
        if (res.status === 503) {
          console.log('[Init] No data yet — waiting for first scheduled run.');
          return null;
        }
        if (!res.ok) throw new Error('HTTP ' + res.status);
        return res.json();
      })
      .then(function (data) {
        if (data) {
          applySnapshot(data);
          console.log('[Init] Snapshot loaded.');
        }
      })
      .catch(function (err) {
        console.warn('[Init] Could not load snapshot:', err.message);
      });
  }

  // ── SSE subscription ──────────────────────────────────────────────────────
  var sseSource = null;

  function connectSSE() {
    setDot('connecting');

    sseSource = new EventSource('/api/events');

    sseSource.addEventListener('update', function (e) {
      try {
        var data = JSON.parse(e.data);
        applySnapshot(data);
        console.log('[SSE] Snapshot received.');
      } catch (err) {
        console.warn('[SSE] Failed to parse event data:', err.message);
      }
    });

    sseSource.onopen = function () {
      setDot('connected');
      console.log('[SSE] Connected.');
    };

    sseSource.onerror = function () {
      setDot('disconnected');
      console.warn('[SSE] Connection lost — browser will auto-reconnect.');
      // EventSource reconnects automatically; update dot when it reopens
    };
  }

  // ── Service Worker registration ───────────────────────────────────────────
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/service-worker.js')
      .then(function () { console.log('[SW] Registered.'); })
      .catch(function (err) { console.warn('[SW] Registration failed:', err.message); });
  }

  // ── PWA install prompt ────────────────────────────────────────────────────
  var deferredInstallPrompt = null;
  var installBtn = document.getElementById('install-btn');

  window.addEventListener('beforeinstallprompt', function (e) {
    e.preventDefault();
    deferredInstallPrompt = e;
    if (installBtn) installBtn.hidden = false;
    console.log('[PWA] Install prompt ready.');
  });

  if (installBtn) {
    installBtn.addEventListener('click', function () {
      if (!deferredInstallPrompt) return;
      deferredInstallPrompt.prompt();
      deferredInstallPrompt.userChoice.then(function (result) {
        console.log('[PWA] User choice:', result.outcome);
        deferredInstallPrompt = null;
        installBtn.hidden = true;
      });
    });
  }

  window.addEventListener('appinstalled', function () {
    console.log('[PWA] App installed.');
    if (installBtn) installBtn.hidden = true;
    deferredInstallPrompt = null;
  });

  // ── Init ──────────────────────────────────────────────────────────────────
  loadInitialData();
  connectSSE();

}());
