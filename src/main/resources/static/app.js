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
    void card.offsetWidth;
    card.classList.add('updated');
  }

  // ── Number formatters ─────────────────────────────────────────────────────
  var KM_PER_AU     = 149597870.7;
  var MILES_PER_AU  = 92955807.273;
  var MILES_PER_KM  = 0.621371;

  function fmtAu(val) {
    return (typeof val === 'number') ? val.toFixed(6) + ' AU' : '—';
  }

  function fmtKm(val) {
    return (typeof val === 'number') ? val.toLocaleString(undefined, { maximumFractionDigits: 0 }) + ' km' : '—';
  }

  function fmtMi(val) {
    return (typeof val === 'number') ? val.toLocaleString(undefined, { maximumFractionDigits: 0 }) + ' mi' : '—';
  }

  /** Format a distance stored in AU, respecting the current unit preference. */
  function fmtDistFromAu(au) {
    if (typeof au !== 'number') return '—';
    if (currentUnit === 'km') return fmtKm(au * KM_PER_AU);
    if (currentUnit === 'mi') return fmtMi(au * MILES_PER_AU);
    return au.toFixed(6) + ' AU';
  }

  /** Format a distance stored in km, respecting the current unit preference. */
  function fmtDistFromKm(km) {
    if (typeof km !== 'number' || km <= 0) return '—';
    if (currentUnit === 'au') return (km / KM_PER_AU).toFixed(6) + ' AU';
    if (currentUnit === 'mi') return fmtMi(km * MILES_PER_KM);
    return fmtKm(km);
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

  function fmtKp(val) {
    if (typeof val !== 'number' || val < 0) return '—';
    var label = val < 2 ? 'Quiet' : val < 4 ? 'Unsettled' : val < 5 ? 'Active' : 'Storm ⚡';
    return val.toFixed(1) + ' (' + label + ')';
  }

  // ── Toast notification ────────────────────────────────────────────────────
  var toastTimer = null;
  function showToast(message) {
    var toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.classList.add('visible');
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      toast.classList.remove('visible');
      toastTimer = null;
    }, 3000);
  }

  // ── Dark / Light theme toggle ─────────────────────────────────────────────
  var THEME_KEY = 'moonphase-theme';
  var themeBtn = document.getElementById('theme-toggle-btn');

  function applyTheme(theme) {
    if (theme === 'light') {
      document.body.classList.add('light-theme');
      if (themeBtn) themeBtn.textContent = '\uD83C\uDF19'; // 🌙
    } else {
      document.body.classList.remove('light-theme');
      if (themeBtn) themeBtn.textContent = '\u2600\uFE0F'; // ☀️
    }
  }

  applyTheme(localStorage.getItem(THEME_KEY) || 'dark');

  if (themeBtn) {
    themeBtn.addEventListener('click', function () {
      var next = document.body.classList.contains('light-theme') ? 'dark' : 'light';
      localStorage.setItem(THEME_KEY, next);
      applyTheme(next);
    });
  }

  // ── Distance unit selector ────────────────────────────────────────────────
  var UNIT_KEY = 'moonphase-unit';
  var currentUnit = localStorage.getItem(UNIT_KEY) || 'au';
  var lastSnapshot = null;

  function applyUnit(unit) {
    currentUnit = unit;
    localStorage.setItem(UNIT_KEY, unit);
    document.querySelectorAll('.unit-btn').forEach(function (btn) {
      btn.classList.toggle('active', btn.getAttribute('data-unit') === unit);
    });
    if (lastSnapshot) applySnapshot(lastSnapshot);
  }

  // Set initial active state
  applyUnit(currentUnit);

  document.querySelectorAll('.unit-btn').forEach(function (btn) {
    btn.addEventListener('click', function () {
      applyUnit(btn.getAttribute('data-unit'));
    });
  });

  // ── Apply snapshot to DOM ─────────────────────────────────────────────────
  function applySnapshot(data) {
    lastSnapshot = data;

    // Moon
    setText('moon-phase-name',  data.moonPhaseName || '—');
    setText('moon-illumination', data.moonIlluminationPercent != null ? data.moonIlluminationPercent + '%' : '—');
    setText('moon-age',         data.moonAgeDays != null ? data.moonAgeDays + ' days' : '—');
    setText('moon-full-in',     data.daysUntilFullMoon != null ? data.daysUntilFullMoon + ' days' : '—');
    setText('moon-distance',    fmtDistFromKm(data.moonDistanceKm));

    var asciiEl = document.getElementById('moon-ascii');
    if (asciiEl && Array.isArray(data.moonAsciiArt)) {
      asciiEl.textContent = data.moonAsciiArt.join('\n');
    }

    // Solar System Distances
    setText('dist-mercury',  fmtDistFromAu(data.mercuryDistanceAu));
    setText('dist-venus',    fmtDistFromAu(data.venusDistanceAu));
    setText('dist-sun',      fmtDistFromAu(data.sunDistanceAu));
    setText('dist-mars',     fmtDistFromAu(data.marsDistanceAu));
    setText('dist-jupiter',  fmtDistFromAu(data.jupiterDistanceAu));
    setText('dist-saturn',   fmtDistFromAu(data.saturnDistanceAu));
    setText('dist-uranus',   fmtDistFromAu(data.uranusDistanceAu));
    setText('dist-neptune',  fmtDistFromAu(data.neptuneDistanceAu));
    setText('dist-pluto',    fmtDistFromAu(data.plutoDistanceAu));

    // Probes
    setText('probe-hubble', data.hubbleAltitudeKm > 0 ? fmtDistFromKm(data.hubbleAltitudeKm) + ' alt' : '—');
    setText('probe-jwst',   data.jamesWebbDistanceKm > 0 ? fmtDistFromKm(data.jamesWebbDistanceKm) : '—');
    setText('probe-v1', fmtDistFromAu(data.voyager1DistanceAu));
    setText('probe-v2', fmtDistFromAu(data.voyager2DistanceAu));
    setText('probe-nh', fmtDistFromAu(data.newHorizonsDistanceAu));

    // Earth
    setText('earth-speed-s',  fmtKmPerSec(data.earthSpeedKmPerSec));
    setText('earth-speed-h',  fmtKmPerHour(data.earthSpeedKmPerHour));
    setText('earth-daylight', fmtHours(data.daylightHours));
    setText('earth-sunrise',  data.sunriseTime  || '—');
    setText('earth-sunset',   data.sunsetTime   || '—');
    setText('earth-tilt',     typeof data.earthAxialTiltDegrees === 'number' ? data.earthAxialTiltDegrees.toFixed(3) + '°' : '—');
    setText('earth-aurora',   fmtKp(data.auroraKpIndex));

    // Light travel times
    setText('light-sun',     data.lightTimeSunToEarth    || '—');
    setText('light-mercury', data.lightTimeSunToMercury  || '—');
    setText('light-venus',   data.lightTimeSunToVenus    || '—');
    setText('light-mars',    data.lightTimeSunToMars     || '—');
    setText('light-jupiter', data.lightTimeSunToJupiter  || '—');
    setText('light-saturn',  data.lightTimeSunToSaturn   || '—');
    setText('light-uranus',  data.lightTimeSunToUranus   || '—');
    setText('light-neptune', data.lightTimeSunToNeptune  || '—');
    setText('light-pluto',   data.lightTimeSunToPluto    || '—');

    // LEO
    setText('leo-iss',      data.issAltitudeKm      > 0 ? fmtDistFromKm(data.issAltitudeKm)      : '—');
    setText('leo-iss-crew', data.issCrew            > 0 ? data.issCrew + ' crew' : '—');
    setText('leo-tiangong', data.tiangongAltitudeKm  > 0 ? fmtDistFromKm(data.tiangongAltitudeKm)  : '—');
    setText('leo-hubble',   data.hubbleAltitudeKm    > 0 ? fmtDistFromKm(data.hubbleAltitudeKm)    : '—');
    setText('leo-starlink', data.starlinkSatelliteCount > 0 ? data.starlinkSatelliteCount.toLocaleString() : '—');
    setText('leo-kuiper',   data.kuiperSatelliteCount   > 0 ? data.kuiperSatelliteCount.toLocaleString()   : '—');
    setText('leo-total',    data.totalSatellitesInOrbit > 0 ? data.totalSatellitesInOrbit.toLocaleString() : '—');

    // People in space
    var peopleTotal = data.totalPeopleInSpace;
    setText('leo-people-total', peopleTotal > 0 ? peopleTotal.toString() : '—');
    var craftTbody = document.getElementById('craft-tbody');
    if (craftTbody) {
      var occupancy = data.craftOccupancy;
      if (occupancy && typeof occupancy === 'object' && Object.keys(occupancy).length > 0) {
        craftTbody.innerHTML = Object.entries(occupancy).map(function (entry) {
          var craft = entry[0];
          var count = entry[1];
          return '<tr><td class="craft-name">' + craft + '</td>'
               + '<td class="val">' + count + '</td></tr>';
        }).join('');
      } else {
        craftTbody.innerHTML = '';
      }
    }

    // Events — sorted chronologically by days remaining
    var events = [
      { label: 'Summer Solstice', days: data.daysUntilSummerSolstice },
      { label: 'Winter Solstice', days: data.daysUntilWinterSolstice },
      { label: 'Perihelion',      days: data.daysUntilPerihelion },
      { label: 'Aphelion',        days: data.daysUntilAphelion }
    ];
    events.sort(function (a, b) { return a.days - b.days; });
    var eventsTbody = document.getElementById('events-tbody');
    if (eventsTbody) {
      eventsTbody.innerHTML = events.map(function (e) {
        return '<tr><td>' + e.label + '</td><td class="val">' + fmtDays(e.days) + '</td></tr>';
      }).join('');
    }

    // Last updated
    setText('last-updated', data.lastUpdated ? 'Updated ' + data.lastUpdated + ' UTC' : '');

    // Flash all cards
    ['card-moon', 'card-solar', 'card-probes', 'card-earth', 'card-events', 'card-leo', 'card-people']
      .forEach(flashCard);

  }

  // ── Hamburger menu ────────────────────────────────────────────────────────
  var menuBtn = document.getElementById('menu-btn');
  var menuDropdown = document.getElementById('menu-dropdown');

  if (menuBtn && menuDropdown) {
    menuBtn.addEventListener('click', function (e) {
      e.stopPropagation();
      var open = menuDropdown.hidden;
      menuDropdown.hidden = !open;
      menuBtn.setAttribute('aria-expanded', String(open));
    });

    // Close when any item inside is clicked
    menuDropdown.addEventListener('click', function () {
      menuDropdown.hidden = true;
      menuBtn.setAttribute('aria-expanded', 'false');
    });

    // Close when clicking anywhere outside the menu
    document.addEventListener('click', function () {
      if (!menuDropdown.hidden) {
        menuDropdown.hidden = true;
        menuBtn.setAttribute('aria-expanded', 'false');
      }
    });
  }

  // ── Refresh button ────────────────────────────────────────────────────────
  var refreshBtn = document.getElementById('refresh-btn');
  var refreshPending = false;

  function setRefreshPending(pending) {
    refreshPending = pending;
    if (!refreshBtn) return;
    refreshBtn.disabled = pending;
    if (pending) {
      refreshBtn.classList.add('spinning');
    } else {
      refreshBtn.classList.remove('spinning');
    }
  }

  if (refreshBtn) {
    refreshBtn.addEventListener('click', function () {
      if (refreshPending) return;
      setRefreshPending(true);
      fetch('/api/refresh', { method: 'POST' })
        .then(function (res) {
          if (res.status === 429) {
            showToast('⏳ Refresh already in progress…');
            setRefreshPending(false);
          } else if (!res.ok) {
            console.warn('[Refresh] Server returned', res.status);
            setRefreshPending(false);
          }
          // on 202 keep spinner spinning until SSE update arrives
        })
        .catch(function (err) {
          console.warn('[Refresh] Request failed:', err.message);
          setRefreshPending(false);
        });
    });
  }

  // ── Reset Data button ─────────────────────────────────────────────────────
  var resetBtn = document.getElementById('reset-btn');
  if (resetBtn) {
    resetBtn.addEventListener('click', function () {
      if (!confirm('Delete all historical snapshots? This cannot be undone.')) return;
      resetBtn.disabled = true;
      fetch('/api/history', { method: 'DELETE' })
        .then(function (r) {
          if (r.ok) { window.location.reload(); }
          else { alert('Reset failed (status ' + r.status + ')'); resetBtn.disabled = false; }
        })
        .catch(function () { alert('Reset failed — server unreachable.'); resetBtn.disabled = false; });
    });
  }

  // ── Populate Past button ──────────────────────────────────────────────────
  var populateBtn = document.getElementById('populate-btn');
  if (populateBtn) {
    populateBtn.addEventListener('click', function () {
      var input = prompt('Populate how many past days? (1\u2013365)', '30');
      if (input === null) return;
      var days = parseInt(input, 10);
      if (isNaN(days) || days < 1 || days > 365) {
        alert('Please enter a number between 1 and 365.');
        return;
      }
      populateBtn.disabled = true;
      populateBtn.textContent = '\u23f3 Populating\u2026';
      fetch('/api/history/populate?days=' + days, { method: 'POST' })
        .then(function (r) {
          if (r.ok) { window.location.reload(); }
          else { alert('Populate failed (status ' + r.status + ')'); populateBtn.disabled = false; populateBtn.textContent = '\ud83d\udcc5 Populate Past'; }
        })
        .catch(function () { alert('Populate failed \u2014 server unreachable.'); populateBtn.disabled = false; populateBtn.textContent = '\ud83d\udcc5 Populate Past'; });
    });
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
        setRefreshPending(false);
        // Refresh chart with the latest persisted data point
        if (metricSelect) loadHistory(metricSelect.value);
        showToast('✓ Data updated');
        // Browser notifications for notable events
        maybeNotify(data);
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

  // ── History chart ─────────────────────────────────────────────────────────
  var historyChart = null;

  function initChart() {
    var ctx = document.getElementById('history-chart');
    if (!ctx) return;
    historyChart = new Chart(ctx, {
      type: 'line',
      data: { labels: [], datasets: [{ label: '', data: [],
        borderColor: '#00d4ff',
        backgroundColor: 'rgba(0, 212, 255, 0.08)',
        fill: true, tension: 0.3, pointRadius: 2,
        pointBackgroundColor: '#00d4ff'
      }] },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 400 },
        plugins: { legend: { display: false } },
        scales: {
          x: {
            ticks: { color: '#7aa8cc', maxTicksLimit: 10, maxRotation: 30 },
            grid: { color: 'rgba(13, 42, 74, 0.8)' }
          },
          y: {
            ticks: { color: '#7aa8cc' },
            grid: { color: 'rgba(13, 42, 74, 0.8)' }
          }
        }
      }
    });
  }

  function loadHistory(metric) {
    var msg = document.getElementById('history-msg');
    var rangeSelect = document.getElementById('range-select');
    var limit = rangeSelect ? parseInt(rangeSelect.value, 10) : 60;
    fetch('/api/history?metric=' + encodeURIComponent(metric) + '&limit=' + limit)
      .then(function (res) {
        if (res.status === 503) {
          if (msg) msg.textContent = 'Historical data unavailable — MongoDB not configured.';
          return null;
        }
        if (!res.ok) throw new Error('HTTP ' + res.status);
        return res.json();
      })
      .then(function (data) {
        if (!data) return;
        if (!historyChart) return;
        if (data.length === 0) {
          if (msg) msg.textContent = 'No data yet — history will appear after the next scheduled update.';
          historyChart.data.labels = [];
          historyChart.data.datasets[0].data = [];
          historyChart.update();
          return;
        }
        if (msg) msg.textContent = data.length + ' data point' + (data.length !== 1 ? 's' : '');
        historyChart.data.labels = data.map(function (d) { return d.timestamp; });
        historyChart.data.datasets[0].data = data.map(function (d) { return d.value; });
        historyChart.update();
      })
      .catch(function (err) {
        if (msg) msg.textContent = 'Could not load history: ' + err.message;
      });
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
  initChart();
  loadHistory('daylightHours');

  var metricSelect = document.getElementById('metric-select');
  if (metricSelect) {
    metricSelect.addEventListener('change', function () {
      loadHistory(this.value);
    });
  }

  // ── Date-range picker ─────────────────────────────────────────────────────
  var rangeSelect = document.getElementById('range-select');
  if (rangeSelect) {
    rangeSelect.addEventListener('change', function () {
      if (metricSelect) loadHistory(metricSelect.value);
    });
  }

  // ── Browser notifications ─────────────────────────────────────────────────
  var notifyBtn = document.getElementById('notify-btn');
  var notificationsEnabled = false;

  // Show the bell button if the Notifications API is supported
  if ('Notification' in window && notifyBtn) {
    notifyBtn.hidden = false;
    if (Notification.permission === 'granted') {
      notificationsEnabled = true;
      notifyBtn.title = 'Notifications enabled';
      notifyBtn.style.opacity = '1';
    }
    notifyBtn.addEventListener('click', function () {
      Notification.requestPermission().then(function (perm) {
        notificationsEnabled = perm === 'granted';
        notifyBtn.title = notificationsEnabled ? 'Notifications enabled' : 'Notifications blocked';
        notifyBtn.style.opacity = notificationsEnabled ? '1' : '0.4';
        if (notificationsEnabled) showToast('🔔 Notifications enabled');
      });
    });
  }

  /**
   * Fires a browser notification when a geomagnetic storm (Kp ≥ 5) is detected
   * or when a full moon is tonight (0 days) or tomorrow (1 day).
   * Notifications are throttled: the same alert is not repeated within 6 hours.
   */
  var lastNotified = {};
  function maybeNotify(data) {
    if (!notificationsEnabled || Notification.permission !== 'granted') return;
    var now = Date.now();
    var SIX_HOURS = 6 * 60 * 60 * 1000;

    if (typeof data.auroraKpIndex === 'number' && data.auroraKpIndex >= 5) {
      var kpKey = 'kp-storm';
      if (!lastNotified[kpKey] || (now - lastNotified[kpKey]) > SIX_HOURS) {
        lastNotified[kpKey] = now;
        new Notification('🌌 Aurora Alert — Geomagnetic Storm!', {
          body: 'Kp index is ' + data.auroraKpIndex.toFixed(1) + ' — aurora may be visible at mid-latitudes.',
          icon: '/icon-192.png',
          tag: kpKey
        });
      }
    }

    if (data.daysUntilFullMoon === 0) {
      var fullKey = 'full-moon-tonight';
      if (!lastNotified[fullKey] || (now - lastNotified[fullKey]) > SIX_HOURS) {
        lastNotified[fullKey] = now;
        new Notification('🌕 Full Moon Tonight!', {
          body: 'Tonight\'s moon is full — ' + (data.moonIlluminationPercent || '?') + '% illuminated.',
          icon: '/icon-192.png',
          tag: fullKey
        });
      }
    } else if (data.daysUntilFullMoon === 1) {
      var tomorrowKey = 'full-moon-tomorrow';
      if (!lastNotified[tomorrowKey] || (now - lastNotified[tomorrowKey]) > SIX_HOURS) {
        lastNotified[tomorrowKey] = now;
        new Notification('🌔 Full Moon Tomorrow', {
          body: 'The full moon is tomorrow — ' + (data.moonIlluminationPercent || '?') + '% illuminated tonight.',
          icon: '/icon-192.png',
          tag: tomorrowKey
        });
      }
    }
  }

  // ── CSV Export ────────────────────────────────────────────────────────────
  var exportBtn = document.getElementById('export-csv-btn');
  if (exportBtn) {
    exportBtn.addEventListener('click', function () {
      var metric = metricSelect ? metricSelect.value : 'daylightHours';
      var limit = rangeSelect ? rangeSelect.value : '500';
      var url = '/api/history/export?metric=' + encodeURIComponent(metric) + '&limit=' + limit;
      var a = document.createElement('a');
      a.href = url;
      a.download = metric + '.csv';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    });
  }

}());
