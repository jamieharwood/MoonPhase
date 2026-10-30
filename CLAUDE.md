# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew bootJar

# Run locally (web dashboard on http://localhost:8080)
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "org.iHarwood.calculation.CalculationOrchestratorTest"

# Run a single test method
./gradlew test --tests "org.iHarwood.calculation.CalculationOrchestratorTest.computeForDate_leoFieldsAreZero"

# Build and run with Docker (app on 8081 + MongoDB + nginx HTTPS; see also docker-compose.dev.yml / docker-compose.deploy.yml)
docker compose up -d

# Build Docker image locally
docker build -t jamieharwood/moonphase:latest .
```

Tests in the default package (`src/test/java/*.java`) are mixed: `DayLightTest`, `EarthAxialTiltTest`, `EarthSpeedTest`, `EquinoxCalculatorTest` are JUnit; the moon-phase ones (`MoonPhaseDebugTest`, `PhaseBoundaryTest`, `JulianDayTest`, etc.) are `main()`-method diagnostic scripts that `./gradlew test` does not run.

## Architecture

**MoonPhaseAI** is a Spring Boot 4.1 / Java 25 application that computes astronomical data on a cron schedule, pushes it to an [Awtrix](https://awtrixdocs.blueforcer.de/) LED matrix device, and serves it through a web dashboard with SSE-based live updates.

### Core data flow

1. `Main.java` — implements `ApplicationRunner` (first update runs once the full context is ready) and holds the `@Scheduled(cron = "${app.cron}")` update. A `ReentrantLock` with `tryLock()` makes overlapping triggers (cron vs. manual refresh) skip rather than queue. It contains no compute logic — it calls the orchestrator in two phases: `computeCurrentResult()` → publish snapshot to the dashboard/SSE → `deliverSideEffects()`. This ordering keeps the web UI responsive when the Awtrix device is slow or offline.

2. `CalculationOrchestrator` (`calculation/`) — the single place where `AstronomicalSnapshot`s are built. `computeCurrentResult()` builds the live snapshot plus the list of pending Awtrix pushes (`LiveComputeResult`) without side effects; `deliverSideEffects()` pushes to Awtrix and fires the async Claude verification; `computeCurrent()` does both (used by tests); `computeForDate()` builds historical snapshots for backfill (no Awtrix, no live fetches). Unit-tested in `CalculationOrchestratorTest` with a mocked `AwtrixPusher`.

3. `AstronomicalSnapshot` — an immutable record (builder pattern) holding all computed values for a single point in time. Serialized to JSON for both SSE broadcast and the REST API.

4. `AstronomicalDataService` — holds the latest snapshot in memory, manages a `CopyOnWriteArrayList<SseEmitter>` of live subscribers, and optionally persists to `HistoryService`. Only instantiated when running as a servlet (`@ConditionalOnWebApplication`).

5. `DashboardController` — REST + SSE endpoints: `GET /api/data`, `GET /api/events` (SSE), `GET /api/health`, `POST /api/refresh` (runs `Main.update()` on the shared executor; returns 202, or 429 if a refresh is already running), `GET /api/history`, `GET /api/history/export`, `POST /api/history/populate`, `DELETE /api/history`. `health/SnapshotHealthIndicator` reports `STARTING` to Actuator until the first snapshot exists.

6. `AwtrixPusher` (`integration/awtrix/`) — dedicated component for all Awtrix HTTP communication. `pushAll()` sends apps in small parallel batches (to avoid overwhelming the ESP32) with a per-cycle circuit breaker that skips remaining pushes once the device is unreachable, plus retries (3 attempts, 2s delay), success/failure counters, and startup connectivity check. Delegates each push to `APIPost`, which holds the `IconType` enum, serialises the payload to JSON via Jackson, and executes the HTTP POST.

7. `HistoryService` — persists snapshots to MongoDB; only active when `app.history.enabled=true`. The `METRIC_EXTRACTORS` map is the single source of truth for which metrics are graphable — add new metrics there only.

### Calculation modules (`MoonPhaseModule/`)

Each module is a pure-static utility class with two method styles:
- `*Now()` — reads current `ZonedDateTime` internally (used by the live compute path)
- `*(ZonedDateTime)` or `*(LocalDate)` — accepts a specific time (used by `computeForDate()` for historical backfill)

Key modules: `MoonPhase`, `MoonDistance`, `SunDistance`, `SunriseSunset`, `PlanetDistance` (interface), `Planets` (enum), `VoyagerDistance`, `NewHorizonsDistance`, `JamesWebbDistance`, `LeoDataFetcher` (live TLE data from `tle.ivanstanojevic.me`, people-in-space from corquaid's ISS API), `AuroraKpFetcher` (live Kp index from NOAA), `LightTravelTime`, `DayLight`, `EarthSpeed`, `EarthAxialTilt`, `EquinoxCalculator`, `PerihelionAphelion`, `DateUtils`, `ClaudeMoonPhaseVerifier`.

### Important design constraints

- **`ClaudeMoonPhaseVerifier` is log-only.** It calls the Claude API to cross-check the deterministic moon phase calculation, but the result is **never used to override** the formula-based answer. If Claude disagrees, it logs a warning — do not change this behavior. It runs async via `CompletableFuture` and must never block the update cycle. It reads `CLAUDE_API_KEY`/`CLAUDE_MODEL`/`CLAUDE_API_URL` directly via `System.getenv`, not Spring properties.
- **LEO data is live-only.** `LeoDataFetcher` fetches real-time data (TLE API + people-in-space JSON). Historical snapshots (`computeForDate`) always set LEO fields to `0` because past TLE data cannot be reconstructed.
- **MongoDB is gated by `app.history.enabled`.** Boot 4 Mongo auto-configurations are always excluded (`MongoAutoConfiguration`, `DataMongoAutoConfiguration`, `DataMongoRepositoriesAutoConfiguration`); `MongoConfig` and `HistoryService` are `@ConditionalOnProperty(app.history.enabled=true)` and create the Mongo beans manually. Defaults to `false`; `docker-compose.yml` enables it alongside its bundled MongoDB. Code that touches history must handle `Optional<HistoryService>`.
- **Headless mode.** Set `SPRING_MAIN_WEB-APPLICATION-TYPE=none` to disable Tomcat entirely. `AstronomicalDataService` and `DashboardController` are both `@ConditionalOnWebApplication` for this reason.

### Configuration

All config can be set via environment variables. Key ones:

| Variable | Default | Notes |
|---|---|---|
| `AWTRIXHOSTNAME` | `http://moonclock.local` | Awtrix device base URL |
| `LATITUDE` / `LONGITUDE` | `51.4769` / `0.0` | Decimal degrees for daylight/sunrise calculations |
| `CRON_SCHEDULE` | `0 1 0,12 * * *` | Spring cron (runs at 00:01 and 12:01 UTC) |
| `CLAUDE_API_KEY` | _(none)_ | Required for moon phase verification |
| `CLAUDE_MODEL` | `claude-sonnet-4-6` | Model for verification (`CLAUDE_API_URL` overrides the endpoint) |
| `SERVER_PORT` | `8080` (local) / `8081` (Docker) | Dashboard port |
| `APP_HISTORY_ENABLED` | `false` | Set `true` + provide MongoDB URI |
| `APP_HISTORY_RETENTION_DAYS` | `0` | Days of history to keep (0 = unlimited); purges nightly at 03:00 UTC |
| `SPRING_DATA_MONGODB_URI` | _(none)_ | e.g. `mongodb://localhost:27017/moonphase` |

### Frontend

Static files in `src/main/resources/static/`. The dashboard (`index.html` + `app.js`) is a PWA with a service worker. It connects to `/api/events` (SSE) for real-time updates and uses Chart.js for the historical graph. The `↻ Refresh` button calls `POST /api/refresh`.

### Spring Boot 4.1 notes
- Starter: `spring-boot-starter-webmvc` (preferred over deprecated `spring-boot-starter-web`)
- Virtual threads: `spring.threads.virtual.enabled=true` plus a shared virtual-thread `ExecutorService` bean (`applicationTaskExecutor`) in `config/AppConfig` — use it for background work instead of creating threads/executors
- Typed config: `AppProperties` / `AwtrixProperties` (`@ConfigurationProperties`)
- Jackson 3: inject Boot `JsonMapper` in Spring components; static utilities use `JsonMapper.shared()`
- Observability: Actuator at `/actuator/health` (liveness/readiness); domain health remains at `/api/health`
- Graceful shutdown: `server.shutdown=graceful`
