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

  /**
   * Format a Solar System–scale distance stored in AU, respecting the unit preference.
   * AU precision defaults to 3 decimals (planets); deep-space probes use 1.
   */
  function fmtDistFromAu(au, decimals) {
    if (typeof au !== 'number') return '—';
    if (currentUnit === 'km') return fmtKm(au * KM_PER_AU);
    if (currentUnit === 'mi') return fmtMi(au * MILES_PER_AU);
    return au.toFixed(decimals == null ? 3 : decimals) + ' AU';
  }

  /**
   * Format a near-Earth distance stored in km (Moon, LEO altitudes, JWST).
   * These are never shown in AU — 0.000003 AU is meaningless — so the AU
   * setting falls back to km; only the miles setting converts.
   */
  function fmtNearKm(km) {
    if (typeof km !== 'number' || km <= 0) return '—';
    if (currentUnit === 'mi') return fmtMi(km * MILES_PER_KM);
    return fmtKm(km);
  }

  function helio(sunRelative, fallback) {
    return (typeof sunRelative === 'number' && sunRelative > 0) ? sunRelative : fallback;
  }

  function fmtKmPerSec(val) {
    return (typeof val === 'number') ? val.toFixed(2) + ' km/s' : '—';
  }

  function fmtKmPerHour(val) {
    return (typeof val === 'number') ? val.toLocaleString(undefined, { maximumFractionDigits: 0 }) + ' km/h' : '—';
  }

  /**
   * Format orbital speed (stored in km/s), respecting the current unit preference.
   * AU is not a useful unit for speed, so km/s is used for both 'au' and 'km'.
   */
  function fmtSpeedPerSec(kmPerSec) {
    if (typeof kmPerSec !== 'number') return '—';
    if (currentUnit === 'mi') return (kmPerSec * MILES_PER_KM).toFixed(2) + ' mi/s';
    return kmPerSec.toFixed(2) + ' km/s';
  }

  /**
   * Format orbital speed (stored in km/h), respecting the current unit preference.
   */
  function fmtSpeedPerHour(kmPerHour) {
    if (typeof kmPerHour !== 'number') return '—';
    if (currentUnit === 'mi') return (kmPerHour * MILES_PER_KM).toLocaleString(undefined, { maximumFractionDigits: 0 }) + ' mi/h';
    return kmPerHour.toLocaleString(undefined, { maximumFractionDigits: 0 }) + ' km/h';
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

  // ── Look and feel ─────────────────────────────────────────────────────────
  var THEME_KEY = 'moonphase-theme';
  var THEMES = {
    dark:   { label: 'Classic', color: '#030a1a' },
    light:  { label: 'Light',   color: '#f0f4f8' },
    spacex: { label: 'SpaceX',  color: '#000000' },
    nasa:   { label: 'NASA',    color: '#0B3D91' },
    blueorigin:  { label: 'Blue Origin', color: '#00205B' },
    rocketlab:   { label: 'Rocket Lab',  color: '#000000' },
    ula:         { label: 'ULA',         color: '#004B87' },
    arianespace: { label: 'Arianespace', color: '#001E60' },
    relativity:  { label: 'Relativity',  color: '#1A1410' },
    firefly:     { label: 'Firefly',     color: '#070B16' }
  };
  var themeBtn = document.getElementById('theme-toggle-btn');
  var themeMenu = document.getElementById('theme-menu');
  var themeCurrent = document.getElementById('theme-current');

  function cssVar(name, fallback) {
    var value = getComputedStyle(document.body).getPropertyValue(name).trim();
    return value || fallback;
  }

  function updateChartTheme() {
    if (monthlyChart && monthlyCache) renderMonthly(monthlyCache.metric, monthlyCache.data);
    if (!historyChart) return;
    var line = cssVar('--chart-line', '#00d4ff');
    var fill = cssVar('--chart-fill', 'rgba(0, 212, 255, 0.08)');
    var tick = cssVar('--chart-tick', '#7aa8cc');
    var grid = cssVar('--chart-grid', 'rgba(13, 42, 74, 0.8)');
    var dataset = historyChart.data.datasets[0];
    dataset.borderColor = line;
    dataset.pointBackgroundColor = line;
    dataset.backgroundColor = fill;
    historyChart.options.scales.x.ticks.color = tick;
    historyChart.options.scales.y.ticks.color = tick;
    historyChart.options.scales.x.grid.color = grid;
    historyChart.options.scales.y.grid.color = grid;
    historyChart.update();
  }

  function applyTheme(theme) {
    if (!THEMES[theme]) theme = 'dark';
    document.documentElement.setAttribute('data-theme', theme);
    document.body.classList.toggle('light-theme', theme === 'light');
    if (themeCurrent) themeCurrent.textContent = THEMES[theme].label;
    if (themeBtn) themeBtn.title = 'Look and feel: ' + THEMES[theme].label;
    var meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', THEMES[theme].color);
    if (themeMenu) {
      themeMenu.querySelectorAll('.theme-option').forEach(function (option) {
        var selected = option.getAttribute('data-theme') === theme;
        option.setAttribute('aria-checked', selected ? 'true' : 'false');
      });
    }
    updateChartTheme();
  }

  function closeThemeMenu() {
    if (!themeMenu || !themeBtn) return;
    themeMenu.hidden = true;
    themeBtn.setAttribute('aria-expanded', 'false');
  }

  applyTheme(localStorage.getItem(THEME_KEY) || document.documentElement.getAttribute('data-theme') || 'dark');

  if (themeBtn && themeMenu) {
    themeBtn.addEventListener('click', function (e) {
      e.stopPropagation();
      var open = themeMenu.hidden;
      themeMenu.hidden = !open;
      themeBtn.setAttribute('aria-expanded', String(open));
    });

    themeMenu.addEventListener('click', function (e) {
      var option = e.target.closest ? e.target.closest('.theme-option') : null;
      if (!option) return;
      e.stopPropagation();
      var next = option.getAttribute('data-theme');
      localStorage.setItem(THEME_KEY, next);
      applyTheme(next);
      closeThemeMenu();
    });

    document.addEventListener('click', closeThemeMenu);
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') closeThemeMenu();
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
    setText('moon-distance',    fmtNearKm(data.moonDistanceKm));

    var asciiEl = document.getElementById('moon-ascii');
    if (asciiEl && Array.isArray(data.moonAsciiArt)) {
      asciiEl.textContent = data.moonAsciiArt.join('\n');
    }
    syncMoonPhase(data);
    renderMoonCycle(data);

    // Solar System Distances
    setText('dist-mercury',  fmtDistFromAu(data.mercuryDistanceAu));
    setText('dist-venus',    fmtDistFromAu(data.venusDistanceAu));
    setText('dist-sun',      fmtDistFromAu(data.sunDistanceAu));
    // The card is Sun-relative; for Mars/Jupiter/Saturn the plain *DistanceAu fields are
    // Earth-relative (history/Awtrix), so prefer the heliocentric ones when present.
    setText('dist-mars',     fmtDistFromAu(helio(data.marsHelioDistanceAu, data.marsDistanceAu)));
    setText('dist-jupiter',  fmtDistFromAu(helio(data.jupiterHelioDistanceAu, data.jupiterDistanceAu)));
    setText('dist-saturn',   fmtDistFromAu(helio(data.saturnHelioDistanceAu, data.saturnDistanceAu)));
    setText('dist-uranus',   fmtDistFromAu(data.uranusDistanceAu));
    setText('dist-neptune',  fmtDistFromAu(data.neptuneDistanceAu));
    setText('dist-pluto',    fmtDistFromAu(data.plutoDistanceAu));

    // Deep space
    setText('probe-jwst', fmtNearKm(data.jamesWebbDistanceKm));
    setText('probe-v1',   fmtDistFromAu(data.voyager1DistanceAu, 1));
    setText('probe-v2',   fmtDistFromAu(data.voyager2DistanceAu, 1));
    setText('probe-nh',   fmtDistFromAu(data.newHorizonsDistanceAu, 1));

    // Earth
    setText('earth-speed-s',       fmtSpeedPerSec(data.earthSpeedKmPerSec));
    setText('earth-speed-s-label', currentUnit === 'mi' ? 'Orbital Speed (mi/s)' : 'Orbital Speed (km/s)');
    setText('earth-speed-h',       fmtSpeedPerHour(data.earthSpeedKmPerHour));
    setText('earth-speed-h-label', currentUnit === 'mi' ? 'Speed (mi/h)' : 'Speed (km/h)');
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

    // Near Earth — altitudes always in km (or mi), never AU
    setText('leo-iss',      fmtNearKm(data.issAltitudeKm));
    setText('leo-tiangong', fmtNearKm(data.tiangongAltitudeKm));
    setText('leo-hubble',   fmtNearKm(data.hubbleAltitudeKm));
    setText('leo-starlink', data.starlinkSatelliteCount > 0 ? data.starlinkSatelliteCount.toLocaleString() : '—');
    setText('leo-kuiper',   data.kuiperSatelliteCount   > 0 ? data.kuiperSatelliteCount.toLocaleString()   : '—');
    var totalRow = document.getElementById('leo-total-row');
    if (totalRow) totalRow.hidden = !(data.totalSatellitesInOrbit > 0);
    setText('leo-total',    data.totalSatellitesInOrbit > 0 ? data.totalSatellitesInOrbit.toLocaleString() : '—');
    var crewNote = document.getElementById('iss-crew-note');
    if (crewNote) {
      crewNote.hidden = !(data.issCrew > 0);
      crewNote.textContent = data.issCrew > 0 ? '★ aboard the ISS · ' + data.issCrew + ' crew' : '';
    }

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
          var row = document.createElement('tr');
          var name = document.createElement('td');
          var value = document.createElement('td');
          name.textContent = craft;
          value.className = 'val';
          value.textContent = count;
          row.appendChild(name);
          row.appendChild(value);
          return row.outerHTML;
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
    ['card-moon', 'card-solar', 'card-earth', 'card-near', 'card-deep', 'card-events']
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
      var input = prompt('Populate how many past days? (1 or more, back to 1900)', '30');
      if (input === null) return;
      var days = parseInt(input, 10);
      if (isNaN(days) || days < 1) {
        alert('Please enter a whole number of days (1 or more).');
        return;
      }
      populateBtn.disabled = true;
      populateBtn.textContent = '\u23f3 Populating\u2026';
      fetch('/api/history/populate?days=' + days, { method: 'POST' })
        .then(function (r) {
          if (r.ok) { window.location.reload(); }
          else { alert(r.status === 429 ? 'A populate is already running \u2014 try again when it finishes.' : 'Populate failed (status ' + r.status + ')'); populateBtn.disabled = false; populateBtn.textContent = '\ud83d\udcc5 Populate Past'; }
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
    var line = cssVar('--chart-line', '#00d4ff');
    var tick = cssVar('--chart-tick', '#7aa8cc');
    var grid = cssVar('--chart-grid', 'rgba(13, 42, 74, 0.8)');
    historyChart = new Chart(ctx, {
      type: 'line',
      data: { labels: [], datasets: [{ label: '', data: [],
        borderColor: line,
        backgroundColor: cssVar('--chart-fill', 'rgba(0, 212, 255, 0.08)'),
        fill: true, tension: 0.3, pointRadius: 2,
        pointBackgroundColor: line
      }] },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 400 },
        plugins: { legend: { display: false } },
        scales: {
          x: {
            ticks: { color: tick, maxTicksLimit: 10, maxRotation: 30 },
            grid: { color: grid }
          },
          y: {
            ticks: { color: tick },
            grid: { color: grid }
          }
        }
      }
    });
  }

  // ── Graph type: "line" (timeline) or "monthly" (grouped bars per month, one per year) ──
  var monthlyChart = null;
  var monthlyCache = null; // { metric, data } — lets a theme change redraw without refetching
  var historyRequest = 0;  // ignore responses that arrive after the user switched view
  var MONTHLY_MAX_YEARS = 8;
  var MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  var YEAR_COLORS = ['#3987e5', '#d95926', '#199e70', '#c98500', '#d55181', '#0ca30c', '#9085e9', '#e66767'];
  // Mirrors HistoryService.SUMMED_METRICS: months are totals for these, averages otherwise.
  var SUMMED_METRICS = ['daylightHours'];
  var METRIC_UNITS = { daylightHours: ' h', moonIlluminationPercent: '%', moonDistanceKm: ' km', moonAgeDays: ' d' };

  function currentChartType() {
    var sel = document.getElementById('chart-type-select');
    return sel && sel.value === 'monthly' ? 'monthly' : 'line';
  }

  function showChartType(type) {
    var lineWrap = document.getElementById('history-line-wrap');
    var monthlyWrap = document.getElementById('history-monthly-wrap');
    var range = document.getElementById('range-select');
    if (lineWrap) lineWrap.hidden = type !== 'line';
    if (monthlyWrap) monthlyWrap.hidden = type !== 'monthly';
    if (range) {
      range.disabled = type === 'monthly';
      range.title = type === 'monthly' ? 'Monthly by year uses all stored history' : 'Date range';
    }
  }

  function loadHistory(metric) {
    var type = currentChartType();
    showChartType(type);
    historyRequest++;
    if (type === 'monthly') loadMonthly(metric, historyRequest);
    else loadTimeline(metric, historyRequest);
  }

  function isLeapYear(y) { return (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0; }

  function formatValue(v, metric) {
    var digits = Math.abs(v) >= 1000 ? 0 : 1;
    return v.toLocaleString(undefined, { maximumFractionDigits: digits }) + (METRIC_UNITS[metric] || '');
  }

  function loadMonthly(metric, token) {
    var msg = document.getElementById('history-msg');
    fetch('/api/history/monthly?metric=' + encodeURIComponent(metric))
      .then(function (res) {
        if (res.status === 503) {
          if (msg) msg.textContent = 'Historical data unavailable — MongoDB not configured.';
          return null;
        }
        if (!res.ok) throw new Error('HTTP ' + res.status);
        return res.json();
      })
      .then(function (data) {
        if (!data || token !== historyRequest) return;
        monthlyCache = { metric: metric, data: data };
        renderMonthly(metric, data);
      })
      .catch(function (err) {
        if (token === historyRequest && msg) msg.textContent = 'Could not load history: ' + err.message;
      });
  }

  function renderMonthly(metric, data) {
    var msg = document.getElementById('history-msg');
    var ctx = document.getElementById('history-monthly-chart');
    if (!ctx || typeof Chart === 'undefined') return;
    if (monthlyChart) { monthlyChart.destroy(); monthlyChart = null; }
    if (data.length === 0) {
      if (msg) msg.textContent = 'No data yet — use Populate Past to backfill earlier years.';
      return;
    }

    // year → [12 values or null]; only the newest MONTHLY_MAX_YEARS years get bars.
    var byYear = {};
    var complete = [[], [], [], [], [], [], [], [], [], [], [], []];
    var partial = 0;
    data.forEach(function (p) {
      if (!byYear[p.year]) byYear[p.year] = [null, null, null, null, null, null, null, null, null, null, null, null];
      byYear[p.year][p.month - 1] = p.value;
      if (p.days >= p.daysInMonth) complete[p.month - 1].push(p.value); else partial++;
    });
    var allYears = Object.keys(byYear).map(Number).sort(function (a, b) { return a - b; });
    var years = allYears.slice(-MONTHLY_MAX_YEARS);

    // Dashed line: per-month average over every year with a complete month (partial months
    // would drag summed metrics down); falls back to whatever exists for that month.
    var avg = MONTH_NAMES.map(function (_, i) {
      var vals = complete[i].length ? complete[i] : allYears.map(function (y) { return byYear[y][i]; }).filter(function (v) { return v !== null; });
      return vals.length ? vals.reduce(function (a, b) { return a + b; }, 0) / vals.length : null;
    });

    var tick = cssVar('--chart-tick', '#7aa8cc');
    var grid = cssVar('--chart-grid', 'rgba(13, 42, 74, 0.8)');
    var avgColor = cssVar('--text-primary', '#ffffff');

    var datasets = [{
      type: 'line', label: 'Monthly average', data: avg, order: 0,
      borderColor: avgColor, backgroundColor: avgColor, borderWidth: 2, borderDash: [6, 4],
      pointRadius: 4, pointStyle: 'circle', pointBackgroundColor: avgColor, tension: 0.35, spanGaps: true
    }];
    years.forEach(function (y) {
      datasets.push({
        type: 'bar', label: y + (isLeapYear(y) ? ' (leap)' : ''), data: byYear[y], order: 1,
        backgroundColor: YEAR_COLORS[y % YEAR_COLORS.length], // keyed on the year so colours don't shift
        pointStyle: 'rect', borderRadius: { topLeft: 2, topRight: 2 }, borderSkipped: 'bottom',
        categoryPercentage: 0.82, barPercentage: 0.92
      });
    });

    monthlyChart = new Chart(ctx, {
      data: { labels: MONTH_NAMES, datasets: datasets },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 400 },
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: { position: 'top', labels: { color: tick, usePointStyle: true, boxWidth: 10, boxHeight: 10 } },
          tooltip: { callbacks: { label: function (c) {
            return c.parsed.y === null ? null : c.dataset.label + ': ' + formatValue(c.parsed.y, metric);
          } } }
        },
        scales: {
          x: { ticks: { color: tick, autoSkip: false }, grid: { display: false } },
          y: { beginAtZero: true, ticks: { color: tick, callback: function (v) { return formatValue(v, metric); } }, grid: { color: grid } }
        }
      }
    });

    if (msg) {
      var how = SUMMED_METRICS.indexOf(metric) >= 0 ? 'Monthly totals' : 'Monthly averages';
      var span = years.length === allYears.length
        ? years[0] + '–' + years[years.length - 1]
        : 'latest ' + years.length + ' of ' + allYears.length + ' years';
      msg.textContent = how + ', ' + span + (partial ? ' · ' + partial + ' partial month' + (partial !== 1 ? 's' : '') + ' (not in the average)' : '');
    }
  }

  function loadTimeline(metric, token) {
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
        if (!data || token !== historyRequest) return;
        if (!historyChart) return;
        if (data.length === 0) {
          if (msg) msg.textContent = 'No data yet — history will appear after the next scheduled update.';
          historyChart.data.labels = [];
          historyChart.data.datasets[0].data = [];
          historyChart.update();
          return;
        }
        if (msg) msg.textContent = data.length + ' data point' + (data.length !== 1 ? 's' : '');
        // Thousands of points: drop markers and smoothing, skip the animation so it stays responsive.
        var dense = data.length > 500;
        historyChart.data.labels = data.map(function (d) { return d.timestamp; });
        historyChart.data.datasets[0].data = data.map(function (d) { return d.value; });
        historyChart.data.datasets[0].pointRadius = dense ? 0 : 2;
        historyChart.data.datasets[0].tension = dense ? 0 : 0.3;
        historyChart.options.animation = data.length > 2000 ? false : { duration: 400 };
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

  // ── History metric + date-range pickers (selection remembered per browser) ─
  var METRIC_KEY = 'moonphase-history-metric';
  var RANGE_KEY = 'moonphase-history-range';
  var CHART_TYPE_KEY = 'moonphase-history-chart-type';
  var metricSelect = document.getElementById('metric-select');
  var rangeSelect = document.getElementById('range-select');
  var chartTypeSelect = document.getElementById('chart-type-select');

  function restoreSelect(select, key) {
    if (!select) return;
    var saved = null;
    try { saved = localStorage.getItem(key); } catch (e) {}
    if (saved === null) return;
    for (var i = 0; i < select.options.length; i++) {
      if (select.options[i].value === saved) { select.value = saved; return; }
    }
  }

  function rememberSelect(select, key) {
    try { localStorage.setItem(key, select.value); } catch (e) {}
  }

  restoreSelect(metricSelect, METRIC_KEY);
  restoreSelect(rangeSelect, RANGE_KEY);
  restoreSelect(chartTypeSelect, CHART_TYPE_KEY);
  loadHistory(metricSelect ? metricSelect.value : 'daylightHours');

  if (metricSelect) {
    metricSelect.addEventListener('change', function () {
      rememberSelect(metricSelect, METRIC_KEY);
      loadHistory(this.value);
    });
  }

  if (chartTypeSelect) {
    chartTypeSelect.addEventListener('change', function () {
      rememberSelect(chartTypeSelect, CHART_TYPE_KEY);
      loadHistory(metricSelect ? metricSelect.value : 'daylightHours');
    });
  }

  if (rangeSelect) {
    rangeSelect.addEventListener('change', function () {
      rememberSelect(rangeSelect, RANGE_KEY);
      loadHistory(metricSelect ? metricSelect.value : 'daylightHours');
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
      var limit = rangeSelect ? rangeSelect.value : '0';
      var url = '/api/history/export?metric=' + encodeURIComponent(metric) + '&limit=' + limit;
      var a = document.createElement('a');
      a.href = url;
      a.download = metric + '.csv';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    });
  }

  // ── Animated moon graphic ─────────────────────────────────────────────────
  var GRAPHIC_KEY = 'moonphase-graphic';
  var SYNODIC_DAYS = 29.530588853;
  var KNOWN_NEW_MOON_JD = 2451550.1;
  var moonCanvas = document.getElementById('moon-canvas');
  var moonCtx = moonCanvas ? moonCanvas.getContext('2d') : null;
  var phaseOffset = 0;
  var moonPhaseLabel = '';

  function mulberry32(seed) {
    var a = seed >>> 0;
    return function () {
      a = (a + 0x6D2B79F5) >>> 0;
      var t = Math.imul(a ^ (a >>> 15), 1 | a);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  var moonRand = mulberry32(0x5eed);
  var moonCraters = [];
  var moonMaria = [];
  var moonStars = [];
  var featureIndex;
  for (featureIndex = 0; featureIndex < 36; featureIndex++) {
    moonCraters.push({
      lon: moonRand() * Math.PI * 2,
      lat: (moonRand() - 0.5) * Math.PI * 0.82,
      r: 0.018 + moonRand() * 0.055,
      shade: 0.72 + moonRand() * 0.28
    });
  }
  for (featureIndex = 0; featureIndex < 5; featureIndex++) {
    moonMaria.push({
      lon: moonRand() * Math.PI * 2,
      lat: (moonRand() - 0.5) * Math.PI * 0.55,
      r: 0.12 + moonRand() * 0.16
    });
  }
  for (featureIndex = 0; featureIndex < 42; featureIndex++) {
    moonStars.push({
      x: moonRand(),
      y: moonRand(),
      s: 0.4 + moonRand() * 1.4,
      p: moonRand() * Math.PI * 2,
      v: 0.6 + moonRand() * 1.8
    });
  }

  function julianDayNumber(date) {
    var day = date.getUTCDate();
    var month = date.getUTCMonth() + 1;
    var year = date.getUTCFullYear();
    var a = Math.floor((14 - month) / 12);
    var y = year + 4800 - a;
    var m = month + 12 * a - 3;
    return day + Math.floor((153 * m + 2) / 5) + 365 * y + Math.floor(y / 4) - Math.floor(y / 100) + Math.floor(y / 400) - 32045;
  }

  function wrapPhase(fraction) {
    fraction = fraction % 1;
    if (fraction < 0) fraction += 1;
    return fraction;
  }

  function epochPhase(date) {
    var frac = (date.getUTCHours() + date.getUTCMinutes() / 60 + date.getUTCSeconds() / 3600 + date.getUTCMilliseconds() / 3600000) / 24;
    var jd = julianDayNumber(date) - 0.5 + frac;
    return wrapPhase((jd - KNOWN_NEW_MOON_JD) / SYNODIC_DAYS);
  }

  function phaseFromSnapshot(data) {
    if (!data) return null;
    var illum = data.moonIlluminationPercent;
    var age = typeof data.moonAgeDays === 'number' ? data.moonAgeDays : null;
    // Northern hemisphere, matching NASA: waxing lights the right limb, waning the left.
    // Age is days since new moon, so it — not the phase name — decides which limb is lit.
    // Illumination sets the terminator width (a gibbous is more than half lit, a crescent less).
    if (typeof illum === 'number') {
      var c = 1 - 2 * (Math.max(0, Math.min(100, illum)) / 100);
      c = Math.max(-1, Math.min(1, c));
      var fromNew = Math.acos(c) / (2 * Math.PI);
      var waning = age != null ? age > SYNODIC_DAYS / 2 : waningName(data.moonPhaseName);
      return waning ? wrapPhase(1 - fromNew) : fromNew;
    }
    if (age != null) return wrapPhase(age / SYNODIC_DAYS);
    return phaseFromName(data.moonPhaseName);
  }

  function waningName(name) {
    name = String(name || '').toLowerCase();
    return name.indexOf('waning') !== -1 || name.indexOf('last quarter') !== -1 || name.indexOf('third quarter') !== -1;
  }

  function phaseFromName(name) {
    name = String(name || '').toLowerCase();
    if (!name) return null;
    if (name.indexOf('new') !== -1) return 0;
    if (name.indexOf('waxing crescent') !== -1) return 0.125;
    if (name.indexOf('first quarter') !== -1) return 0.25;
    if (name.indexOf('waxing gibbous') !== -1) return 0.375;
    if (name.indexOf('full') !== -1) return 0.5;
    if (name.indexOf('waning gibbous') !== -1) return 0.625;
    if (name.indexOf('last quarter') !== -1 || name.indexOf('third quarter') !== -1) return 0.75;
    if (name.indexOf('waning crescent') !== -1) return 0.875;
    return null;
  }

  function syncMoonPhase(data) {
    if (data && data.moonPhaseName) moonPhaseLabel = data.moonPhaseName;
    var server = phaseFromSnapshot(data);
    if (server != null) phaseOffset = wrapPhase(server - epochPhase(new Date()));
    if (moonCanvas) {
      moonCanvas.setAttribute('aria-label', (moonPhaseLabel || 'Moon') + ' — animated phase, moon base, and Starship landing');
    }
  }

  function currentMoonPhase(date) {
    return wrapPhase(epochPhase(date) + phaseOffset);
  }

  // ── Lunar cycle: progress bar, phase strip, next new / full moon ─────────
  var PHASE_STRIP = [ // [illuminated fraction, waxing?] — New → Full → New
    [0, true], [0.25, true], [0.5, true], [0.75, true],
    [1, true], [0.75, false], [0.5, false], [0.25, false]
  ];

  /** SVG path for the lit part of a disc. Waxing lights the right limb (northern hemisphere). */
  function litPath(cx, cy, r, f, waxing) {
    if (f <= 0.001) return '';
    var top = cx + ' ' + (cy - r), bot = cx + ' ' + (cy + r);
    if (f >= 0.999) return 'M' + top + 'A' + r + ' ' + r + ' 0 1 1 ' + bot + 'A' + r + ' ' + r + ' 0 1 1 ' + top + 'Z';
    var rx = Math.max(Math.abs(r * (2 * f - 1)), 0.01);
    var limb = waxing ? 1 : 0;
    var term = waxing ? (f >= 0.5 ? 1 : 0) : (f >= 0.5 ? 0 : 1);
    return 'M' + top + 'A' + r + ' ' + r + ' 0 0 ' + limb + ' ' + bot + 'A' + rx + ' ' + r + ' 0 0 ' + term + ' ' + top + 'Z';
  }

  var phaseStripIndex = -1;
  function renderPhaseStrip(current) {
    var strip = document.getElementById('phase-strip');
    if (!strip || current === phaseStripIndex) return;
    phaseStripIndex = current;
    strip.innerHTML = PHASE_STRIP.map(function (p, i) {
      var lit = litPath(13, 13, 8, p[0], p[1]);
      return '<svg class="phase-icon' + (i === current ? ' current' : '') + '" viewBox="0 0 26 26">'
        + '<circle class="phase-dark" cx="13" cy="13" r="8"/>'
        + (lit ? '<path class="phase-lit" d="' + lit + '"/>' : '')
        + (i === current ? '<circle class="phase-ring" cx="13" cy="13" r="11.5"/>' : '')
        + '</svg>';
    }).join('');
  }

  /** "24 Dec · 26 days" from an ISO instant (exact, from the server) or a day count (estimate, marked ≈). */
  function fmtMoonDate(when, approxDays) {
    var at = null, approx = false;
    if (typeof when === 'string' && when) {
      at = new Date(when);
      if (isNaN(at.getTime())) at = null;
    }
    if (!at && typeof approxDays === 'number' && approxDays >= 0) {
      at = new Date(Date.now() + approxDays * 86400000);
      approx = true;
    }
    if (!at) return '—';
    var days = Math.round((at.getTime() - Date.now()) / 86400000);
    var date = at.toLocaleDateString(undefined, { day: 'numeric', month: 'short' });
    if (days <= 0) return (approx ? '≈ ' : '') + 'Tonight';
    if (days === 1) return (approx ? '≈ ' : '') + 'Tomorrow · ' + date;
    return (approx ? '≈ ' : '') + date + ' · ' + days + ' days';
  }

  function renderMoonCycle(data) {
    var age = typeof data.moonAgeDays === 'number' ? data.moonAgeDays : null;
    setText('moon-cycle-day', age != null ? 'Day ' + age + ' of ' + SYNODIC_DAYS.toFixed(1) : '—');
    var pct = age != null ? Math.max(0, Math.min(100, age / SYNODIC_DAYS * 100)) : 0;
    var fill = document.getElementById('moon-cycle-fill');
    if (fill) fill.style.width = pct.toFixed(1) + '%';
    var track = document.getElementById('moon-cycle-track');
    if (track) track.setAttribute('aria-valuenow', String(Math.round(pct)));

    // Strip index from the server's phase name (authoritative), else from the synced phase.
    var named = phaseFromName(data.moonPhaseName);
    var phase = currentMoonPhase(new Date());
    renderPhaseStrip(Math.round((named != null ? named : phase) * 8) % 8);

    // Exact times from the server's true-phase ephemeris; estimate only for older servers.
    setText('moon-next-full', fmtMoonDate(data.nextFullMoon, data.daysUntilFullMoon));
    setText('moon-next-new', fmtMoonDate(data.nextNewMoon, (1 - phase) * SYNODIC_DAYS));
  }

  function applyGraphic(mode) {
    if (mode !== 'animated') mode = 'ascii';
    document.documentElement.setAttribute('data-moon-graphic', mode);
    try { localStorage.setItem(GRAPHIC_KEY, mode); } catch (e) {}
    document.querySelectorAll('.graphic-btn').forEach(function (btn) {
      btn.setAttribute('aria-pressed', btn.getAttribute('data-graphic') === mode ? 'true' : 'false');
    });
    if (moonCanvas) moonCanvas.setAttribute('aria-hidden', mode === 'ascii' ? 'true' : 'false');
    var asciiEl = document.getElementById('moon-ascii');
    if (asciiEl) asciiEl.setAttribute('aria-hidden', mode === 'animated' ? 'true' : 'false');
  }

  applyGraphic(document.documentElement.getAttribute('data-moon-graphic') || 'ascii');
  document.querySelectorAll('.graphic-btn').forEach(function (btn) {
    btn.addEventListener('click', function () {
      applyGraphic(btn.getAttribute('data-graphic'));
    });
  });

  // Shadow the unlit limb. phase 0 = new, 0.25 = first quarter (right half lit),
  // 0.5 = full, 0.75 = third quarter (left half lit). Northern hemisphere / NASA.
  function fillPhaseShadow(ctx, cx, cy, R, phase) {
    var cos = Math.cos(phase * Math.PI * 2);
    var rx = Math.max(Math.abs(cos) * R, 0.5);
    ctx.beginPath();
    if (phase <= 0.5) {
      ctx.arc(cx, cy, R, -Math.PI / 2, Math.PI / 2, true);
      ctx.ellipse(cx, cy, rx, R, 0, Math.PI / 2, -Math.PI / 2, cos > 0);
    } else {
      ctx.arc(cx, cy, R, -Math.PI / 2, Math.PI / 2, false);
      ctx.ellipse(cx, cy, rx, R, 0, Math.PI / 2, -Math.PI / 2, -cos > 0);
    }
    ctx.closePath();
    ctx.fill();
  }

  function smoothstep(u) {
    u = Math.max(0, Math.min(1, u));
    return u * u * (3 - 2 * u);
  }
  function landingPose(t, padX, fromX, fromY, groundedY, shipH) {
    var keys = [
      { t: 0, x: fromX, y: fromY, angle: 0.62, thrust: 0.65 },
      { t: 0.18, x: fromX * 0.62 + padX * 0.38, y: fromY * 0.62 + (groundedY - shipH) * 0.38, angle: 0.3, thrust: 0.8 },
      { t: 0.36, x: padX, y: groundedY - shipH * 0.95, angle: 0.05, thrust: 1 },
      { t: 0.54, x: padX, y: groundedY, angle: 0, thrust: 0.9 },
      { t: 0.62, x: padX, y: groundedY, angle: 0, thrust: 0 },
      { t: 0.8, x: padX, y: groundedY, angle: 0, thrust: 0 },
      { t: 0.9, x: padX + shipH * 0.15, y: groundedY - shipH * 1.15, angle: -0.22, thrust: 1 },
      { t: 1, x: fromX, y: fromY, angle: 0.62, thrust: 0.65 }
    ];
    var i = 0;
    while (i < keys.length - 2 && t > keys[i + 1].t) i++;
    var a = keys[i];
    var b = keys[i + 1];
    var u = smoothstep((t - a.t) / (b.t - a.t));
    return {
      x: a.x + (b.x - a.x) * u,
      y: a.y + (b.y - a.y) * u,
      angle: a.angle + (b.angle - a.angle) * u,
      thrust: a.thrust + (b.thrust - a.thrust) * u
    };
  }
  function drawMoonBase(ctx, x, y, scale, now, thrustNearby) {
    if (scale < 6) return;
    ctx.save();
    ctx.translate(x, y);
    ctx.beginPath();
    ctx.ellipse(0, scale * 0.02, scale * 0.62, scale * 0.2, 0, 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(40, 36, 32, 0.28)';
    ctx.fill();
    ctx.beginPath();
    ctx.ellipse(0, 0, scale * 0.46, scale * 0.16, 0, 0, Math.PI * 2);
    ctx.fillStyle = '#3e4450';
    ctx.fill();
    ctx.lineWidth = Math.max(1, scale * 0.035);
    ctx.strokeStyle = thrustNearby > 0.4 ? 'rgba(255, 214, 120, 0.95)' : 'rgba(214, 222, 232, 0.8)';
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(-scale * 0.3, 0);
    ctx.lineTo(scale * 0.3, 0);
    ctx.moveTo(0, -scale * 0.1);
    ctx.lineTo(0, scale * 0.1);
    ctx.strokeStyle = 'rgba(255, 206, 96, 0.85)';
    ctx.lineWidth = Math.max(1, scale * 0.025);
    ctx.stroke();
    ctx.strokeStyle = '#9aa6b2';
    ctx.lineWidth = Math.max(1, scale * 0.04);
    ctx.beginPath();
    ctx.moveTo(-scale * 0.34, -scale * 0.02);
    ctx.lineTo(-scale * 0.62, -scale * 0.08);
    ctx.stroke();
    ctx.fillStyle = '#e7eef5';
    ctx.beginPath();
    ctx.ellipse(-scale * 0.72, -scale * 0.1, scale * 0.24, scale * 0.11, -0.2, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#7f93a6';
    ctx.lineWidth = 1;
    ctx.stroke();
    ctx.fillStyle = '#c5d0dc';
    ctx.fillRect(-scale * 0.9, -scale * 0.12, scale * 0.16, scale * 0.07);
    ctx.beginPath();
    ctx.arc(-scale * 0.28, -scale * 0.08, scale * 0.13, Math.PI, 0);
    ctx.lineTo(-scale * 0.15, -scale * 0.08);
    ctx.fillStyle = 'rgba(186, 224, 255, 0.92)';
    ctx.fill();
    ctx.strokeStyle = '#8eb4d0';
    ctx.stroke();
    ctx.save();
    ctx.translate(scale * 0.42, -scale * 0.18);
    ctx.rotate(-0.45);
    ctx.fillStyle = '#16385f';
    ctx.fillRect(0, -scale * 0.07, scale * 0.4, scale * 0.14);
    ctx.strokeStyle = 'rgba(130, 196, 255, 0.7)';
    ctx.strokeRect(0, -scale * 0.07, scale * 0.4, scale * 0.14);
    ctx.restore();
    ctx.beginPath();
    ctx.moveTo(scale * 0.16, 0);
    ctx.lineTo(scale * 0.2, -scale * 0.42);
    ctx.strokeStyle = '#d5dbe3';
    ctx.lineWidth = Math.max(1, scale * 0.03);
    ctx.stroke();
    var beaconOn = Math.sin(now / 260) > 0;
    ctx.beginPath();
    ctx.arc(scale * 0.2, -scale * 0.42, scale * 0.04, 0, Math.PI * 2);
    ctx.fillStyle = beaconOn ? '#ff3b3b' : '#6e2424';
    ctx.fill();
    if (beaconOn) {
      ctx.beginPath();
      ctx.arc(scale * 0.2, -scale * 0.42, scale * 0.09, 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(255, 60, 60, 0.28)';
      ctx.fill();
    }
    ctx.restore();
  }
  function drawLandingDust(ctx, x, y, scale, amount, now) {
    if (amount < 0.08) return;
    var i;
    ctx.save();
    for (i = 0; i < 12; i++) {
      var ang = (i / 12) * Math.PI * 2 + now / 320;
      var dist = scale * (0.35 + 0.7 * (0.5 + 0.5 * Math.sin(now / 160 + i))) * amount;
      ctx.globalAlpha = 0.18 * amount;
      ctx.fillStyle = '#d9c7aa';
      ctx.beginPath();
      ctx.ellipse(x + Math.cos(ang) * dist, y + Math.sin(ang) * dist * 0.32, scale * 0.08, scale * 0.03, ang, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.restore();
  }
  function drawStarship(ctx, x, y, height, angle, thrust, now) {
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(angle);
    var w = height * 0.28;
    if (thrust > 0.04) {
      var flicker = 0.72 + 0.28 * Math.sin(now / 37);
      var plumeH = height * (0.28 + 0.9 * thrust) * flicker;
      var plume = ctx.createLinearGradient(0, height * 0.46, 0, height * 0.46 + plumeH);
      plume.addColorStop(0, 'rgba(255, 248, 230, 0.95)');
      plume.addColorStop(0.22, 'rgba(255, 168, 48, 0.85)');
      plume.addColorStop(1, 'rgba(255, 70, 16, 0)');
      ctx.fillStyle = plume;
      ctx.beginPath();
      ctx.moveTo(-w * 0.22, height * 0.44);
      ctx.lineTo(w * 0.22, height * 0.44);
      ctx.lineTo(w * 0.72, height * 0.44 + plumeH);
      ctx.lineTo(-w * 0.72, height * 0.44 + plumeH);
      ctx.closePath();
      ctx.fill();
    }
    var steel = ctx.createLinearGradient(-w, 0, w, 0);
    steel.addColorStop(0, '#8b929b');
    steel.addColorStop(0.32, '#f7f9fc');
    steel.addColorStop(0.68, '#c4cbd3');
    steel.addColorStop(1, '#767e88');
    ctx.fillStyle = steel;
    ctx.beginPath();
    ctx.moveTo(-w * 0.5, height * 0.42);
    ctx.lineTo(-w * 0.4, -height * 0.2);
    ctx.quadraticCurveTo(0, -height * 0.58, w * 0.4, -height * 0.2);
    ctx.lineTo(w * 0.5, height * 0.42);
    ctx.closePath();
    ctx.fill();
    ctx.strokeStyle = 'rgba(255,255,255,0.35)';
    ctx.lineWidth = 0.6;
    ctx.stroke();
    ctx.fillStyle = '#4e565f';
    ctx.beginPath();
    ctx.moveTo(-w * 0.4, -height * 0.05);
    ctx.lineTo(-w * 1.05, height * 0.05);
    ctx.lineTo(-w * 0.46, height * 0.14);
    ctx.closePath();
    ctx.fill();
    ctx.beginPath();
    ctx.moveTo(w * 0.4, -height * 0.05);
    ctx.lineTo(w * 1.05, height * 0.05);
    ctx.lineTo(w * 0.46, height * 0.14);
    ctx.closePath();
    ctx.fill();
    ctx.beginPath();
    ctx.moveTo(-w * 0.5, height * 0.2);
    ctx.lineTo(-w * 0.92, height * 0.42);
    ctx.lineTo(-w * 0.48, height * 0.4);
    ctx.closePath();
    ctx.fill();
    ctx.beginPath();
    ctx.moveTo(w * 0.5, height * 0.2);
    ctx.lineTo(w * 0.92, height * 0.42);
    ctx.lineTo(w * 0.48, height * 0.4);
    ctx.closePath();
    ctx.fill();
    ctx.fillStyle = '#1c2633';
    ctx.fillRect(-w * 0.16, -height * 0.01, w * 0.32, height * 0.03);
    ctx.restore();
  }
  function drawAnimatedMoon(now) {
    if (!moonCtx || !moonCanvas) return;
    var rect = moonCanvas.getBoundingClientRect();
    var cssW = Math.max(1, Math.round(rect.width));
    var cssH = Math.max(1, Math.round(rect.height));
    if (cssW < 2 || cssH < 2) return;
    var dpr = Math.min(window.devicePixelRatio || 1, 2);
    var pw = Math.round(cssW * dpr);
    var ph = Math.round(cssH * dpr);
    if (moonCanvas.width !== pw || moonCanvas.height !== ph) {
      moonCanvas.width = pw;
      moonCanvas.height = ph;
    }
    var ctx = moonCtx;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, cssW, cssH);

    var theme = document.documentElement.getAttribute('data-theme') || 'dark';
    var lightTheme = theme === 'light' || theme === 'nasa' || theme === 'ula';
    var starColor = lightTheme ? 'rgba(40, 60, 90, 0.55)' : 'rgba(255, 255, 255, 0.9)';
    var starIndex;
    for (starIndex = 0; starIndex < moonStars.length; starIndex++) {
      var starDot = moonStars[starIndex];
      var twinkle = 0.25 + 0.75 * Math.abs(Math.sin(now / 700 * starDot.v + starDot.p));
      ctx.globalAlpha = twinkle;
      ctx.fillStyle = starColor;
      ctx.beginPath();
      ctx.arc(((starDot.x + now / 90000) % 1) * cssW, starDot.y * cssH, starDot.s, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.globalAlpha = 1;

    var cx = cssW / 2;
    var cy = cssH / 2 + Math.sin(now / 2400) * 2;
    var radius = Math.min(cssW, cssH) * 0.36;
    var phase = currentMoonPhase(new Date());
    var pulse = 0.65 + 0.35 * Math.sin(now / 1100);
    var glow = cssVar('--moon-glow', 'rgba(240, 230, 140, 0.45)');
    if (!glow || glow === 'transparent') glow = lightTheme ? 'rgba(90, 110, 140, 0.28)' : 'rgba(240, 230, 140, 0.4)';

    var halo = ctx.createRadialGradient(cx, cy, radius * 0.7, cx, cy, radius * 1.7);
    halo.addColorStop(0, glow);
    halo.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.globalAlpha = 0.55 + 0.45 * pulse;
    ctx.fillStyle = halo;
    ctx.beginPath();
    ctx.arc(cx, cy, radius * 1.7, 0, Math.PI * 2);
    ctx.fill();
    ctx.globalAlpha = 1;

    ctx.save();
    ctx.beginPath();
    ctx.arc(cx, cy, radius, 0, Math.PI * 2);
    ctx.clip();

    var lightX = Math.sin(phase * Math.PI * 2) * radius * 0.42;
    var disc = ctx.createRadialGradient(cx + lightX * 0.3, cy - radius * 0.28, radius * 0.08, cx, cy, radius);
    disc.addColorStop(0, '#fffdf8');
    disc.addColorStop(0.5, '#e7dcc0');
    disc.addColorStop(1, '#b6a684');
    ctx.fillStyle = disc;
    ctx.fillRect(cx - radius, cy - radius, radius * 2, radius * 2);

    var rot = Math.sin(now / 4200) * 0.28;
    var tilt = Math.sin(now / 6100) * 0.08;
    function project(lon, lat) {
      var x = Math.cos(lat) * Math.sin(lon + rot);
      var y = Math.sin(lat + tilt);
      var z = Math.cos(lat) * Math.cos(lon + rot);
      return { x: cx + x * radius, y: cy - y * radius, z: z };
    }

    var feature;
    ctx.fillStyle = 'rgba(120, 110, 96, 0.28)';
    for (feature = 0; feature < moonMaria.length; feature++) {
      var mare = moonMaria[feature];
      var marePoint = project(mare.lon, mare.lat);
      if (marePoint.z <= 0.05) continue;
      ctx.beginPath();
      ctx.ellipse(marePoint.x, marePoint.y, mare.r * radius * marePoint.z, mare.r * radius * 0.72 * marePoint.z, rot, 0, Math.PI * 2);
      ctx.fill();
    }
    for (feature = 0; feature < moonCraters.length; feature++) {
      var crater = moonCraters[feature];
      var craterPoint = project(crater.lon, crater.lat);
      if (craterPoint.z <= 0.08) continue;
      var craterRadius = crater.r * radius * craterPoint.z;
      ctx.beginPath();
      ctx.arc(craterPoint.x, craterPoint.y, craterRadius, 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(90, 82, 70, ' + (0.35 * crater.shade) + ')';
      ctx.fill();
      ctx.beginPath();
      ctx.arc(craterPoint.x - craterRadius * 0.2, craterPoint.y - craterRadius * 0.2, craterRadius * 0.55, 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(255, 250, 236, 0.18)';
      ctx.fill();
    }

    var sheen = now / 2600;
    ctx.beginPath();
    ctx.arc(cx + Math.cos(sheen) * radius * 0.34, cy + Math.sin(sheen * 0.8) * radius * 0.18, radius * 0.09, 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(255,255,255,0.22)';
    ctx.fill();

    ctx.fillStyle = 'rgba(7, 10, 20, 0.84)';
    fillPhaseShadow(ctx, cx, cy, radius, phase);

    var limb = ctx.createRadialGradient(cx, cy, radius * 0.72, cx, cy, radius);
    limb.addColorStop(0, 'rgba(0,0,0,0)');
    limb.addColorStop(1, 'rgba(0,0,0,0.38)');
    ctx.fillStyle = limb;
    ctx.fillRect(cx - radius, cy - radius, radius * 2, radius * 2);

    var base = project(0.22, -0.38);
    var baseScale = radius * 0.34 * Math.max(base.z, 0.35);
    var shipH = Math.max(26, radius * 0.46);
    var groundedY = base.y - shipH * 0.4;
    var fromX = Math.min(cssW - 18, cx + radius * 0.95);
    var fromY = Math.max(14, cy - radius * 0.95);
    var pose = landingPose((now % 14000) / 14000, base.x, fromX, fromY, groundedY, shipH);
    var closeness = 1 - Math.min(1, Math.hypot(pose.x - base.x, pose.y - groundedY) / (shipH * 1.4));
    drawMoonBase(ctx, base.x, base.y, baseScale, now, pose.thrust * closeness);
    ctx.restore();

    drawLandingDust(ctx, base.x, base.y, baseScale, pose.thrust * closeness, now);
    drawStarship(ctx, pose.x, pose.y, shipH, pose.angle, pose.thrust, now);

    ctx.beginPath();
    ctx.arc(cx, cy, radius, 0, Math.PI * 2);
    ctx.strokeStyle = 'rgba(255, 248, 230, 0.35)';
    ctx.lineWidth = 1.2;
    ctx.stroke();
  }

  var moonRaf = 0;
  var moonTimer = 0;
  function moonTick() { drawAnimatedMoon(performance.now()); }
  function startMoonLoop() {
    if (moonRaf) return;
    (function frame() {
      moonTick();
      moonRaf = requestAnimationFrame(frame);
    })();
  }
  document.addEventListener('visibilitychange', function () {
    if (document.hidden) {
      if (moonRaf) {
        cancelAnimationFrame(moonRaf);
        moonRaf = 0;
      }
      if (!moonTimer) moonTimer = setInterval(moonTick, 33);
    } else if (moonTimer) {
      clearInterval(moonTimer);
      moonTimer = 0;
      startMoonLoop();
    } else {
      startMoonLoop();
    }
  });
  if (document.hidden) moonTimer = setInterval(moonTick, 33);
  else startMoonLoop();
  window.addEventListener('resize', moonTick);

})();




