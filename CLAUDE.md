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

# Build and run with Docker (includes MongoDB + nginx)
docker compose up -d

# Build Docker image locally
docker build -t jamieharwood/moonphase:latest .
```

## Architecture

**MoonPhaseAI** is a Spring Boot 3 / Java 21 application that computes astronomical data on a cron schedule, pushes it to an [Awtrix](https://awtrixdocs.blueforcer.de/) LED matrix device, and serves it through a web dashboard with SSE-based live updates.

### Core data flow

1. `Main.java` — Spring `@Component` with `@Scheduled` and `@PostConstruct`. Owns the update loop and `updateLock` (prevents concurrent Awtrix pushes from racing with manual dashboard refreshes). Delegates snapshot building to private compute methods that are intentionally duplicated in `CalculationOrchestrator` — **see note below**.

2. `CalculationOrchestrator` — a separately-extracted `@Component` (in `calculation/`) that mirrors the compute logic from `Main` but is designed to be unit-testable (accepts an injected `AwtrixPusher` mock). Used by `CalculationOrchestratorTest`. Both `Main` and `CalculationOrchestrator` produce `AstronomicalSnapshot` instances using the builder pattern.

3. `AstronomicalSnapshot` — an immutable record (builder pattern) holding all computed values for a single point in time. Serialized to JSON for both SSE broadcast and the REST API.

4. `AstronomicalDataService` — holds the latest snapshot in memory, manages a `CopyOnWriteArrayList<SseEmitter>` of live subscribers, and optionally persists to `HistoryService`. Only instantiated when running as a servlet (`@ConditionalOnWebApplication`).

5. `DashboardController` — REST + SSE endpoints: `GET /api/data`, `GET /api/events` (SSE), `POST /api/refresh` (triggers manual recalc on a new thread), `GET /api/history`, `POST /api/history/populate`, `DELETE /api/history`.

6. `AwtrixPusher` (`integration/awtrix/`) — dedicated component for all Awtrix HTTP communication. Handles retries (3 attempts, 2s delay), success/failure counters, and startup connectivity check.

7. `HistoryService` — persists snapshots to MongoDB; only active when `app.history.enabled=true`. The `METRIC_EXTRACTORS` map is the single source of truth for which metrics are graphable — add new metrics there only.

### Calculation modules (`MoonPhaseModule/`)

Each module is a pure-static utility class with two method styles:
- `*Now()` — reads current `ZonedDateTime` internally (used by `Main`/`CalculationOrchestrator.computeCurrent()`)
- `*(ZonedDateTime)` or `*(LocalDate)` — accepts a specific time (used by `computeForDate()` for historical backfill)

Key modules: `MoonPhase`, `SunDistance`, `PlanetDistance` (interface), `Planets` (enum), `VoyagerDistance`, `NewHorizonsDistance`, `LeoDataFetcher` (live CelesTrak HTTP), `LightTravelTime`, `DayLight`, `EarthSpeed`, `EarthAxialTilt`, `EquinoxCalculator`, `PerihelionAphelion`, `ClaudeMoonPhaseVerifier`.

### Important design constraints

- **`ClaudeMoonPhaseVerifier` is log-only.** It calls the Claude API to cross-check the deterministic moon phase calculation, but the result is **never used to override** the formula-based answer. If Claude disagrees, it logs a warning — do not change this behavior.
- **LEO data is live-only.** `LeoDataFetcher` fetches real-time TLE data from CelesTrak. Historical snapshots (`computeForDate`) always set LEO fields to `0` because past TLE data cannot be reconstructed.
- **MongoDB is opt-in.** When `app.history.enabled=false` (the default), all three MongoDB auto-configurations are excluded and `HistoryService` is not instantiated. Code that touches history must handle `Optional<HistoryService>`.
- **Headless mode.** Set `SPRING_MAIN_WEB-APPLICATION-TYPE=none` to disable Tomcat entirely. `AstronomicalDataService` and `DashboardController` are both `@ConditionalOnWebApplication` for this reason.

### Configuration

All config can be set via environment variables. Key ones:

| Variable | Default | Notes |
|---|---|---|
| `AWTRIXHOSTNAME` | `http://moonclock.local` | Awtrix device base URL |
| `LATITUDE` | `51.4769` | Decimal degrees for daylight calculations |
| `CRON_SCHEDULE` | `0 1 0,12 * * *` | Spring cron (runs at 00:01 and 12:01 UTC) |
| `CLAUDE_API_KEY` | _(none)_ | Required for moon phase verification |
| `CLAUDE_MODEL` | `claude-sonnet-4-6` | Model for verification |
| `SERVER_PORT` | `8080` (local) / `8081` (Docker) | Dashboard port |
| `APP_HISTORY_ENABLED` | `false` | Set `true` + provide MongoDB URI |
| `SPRING_DATA_MONGODB_URI` | _(none)_ | e.g. `mongodb://localhost:27017/moonphase` |

### Frontend

Static files in `src/main/resources/static/`. The dashboard (`index.html` + `app.js`) is a PWA with a service worker. It connects to `/api/events` (SSE) for real-time updates and uses Chart.js for the historical graph. The `↻ Refresh` button calls `POST /api/refresh`.
