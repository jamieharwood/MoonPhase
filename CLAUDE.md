# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Compile only
./gradlew compileJava

# Build executable JAR
./gradlew bootJar

# Run tests (note: "no tests discovered" is a pre-existing issue, not a regression)
./gradlew test

# Run locally (headless mode — no web server)
./gradlew bootRun

# Run with dashboard enabled
SPRING_MAIN_WEB-APPLICATION-TYPE=servlet ./gradlew bootRun
```

### Docker

```bash
# Production (nginx + MongoDB + app)
docker compose up -d

# Development (faster update interval, DEBUG logging, host network)
docker compose -f docker-compose.dev.yml up --build

# Rebuild and redeploy
docker compose up -d --build moonphase
```

The Docker remote is named `main`, not `origin`: `git push main <branch>`.

## Architecture Overview

This is a Spring Boot app that calculates astronomical metrics twice daily and pushes them to an [Awtrix](https://blueforcer.github.io/awtrix3/) LED display device.

### Execution Flow

1. `MoonPhaseApplication` starts Spring context with `@EnableScheduling`
2. `Main` (`@Component`) initializes via `@PostConstruct`: checks Awtrix connectivity, runs first update
3. `@Scheduled` cron (default `0 1 0,12 * * *`) calls `update()` every 12 hours
4. `update()` acquires `updateLock`, calls `doUpdate()` which orchestrates all module calculations
5. Results are packaged into `AstronomicalSnapshot` (immutable Java record with builder)
6. Snapshot is pushed to Awtrix via `APIPost` (17 named apps, 3 retries with 2s delay)
7. If web is enabled, `AstronomicalDataService.publishSnapshot()` stores in MongoDB and broadcasts via SSE

### Web Dashboard (optional)

Enabled by setting `spring.main.web-application-type=servlet` (or env var `SPRING_MAIN_WEB-APPLICATION-TYPE=servlet`). All web beans use `@ConditionalOnWebApplication` so the classpath is safe either way.

- `GET /` — serves `index.html` (dark-theme PWA dashboard)
- `GET /api/data` — returns latest `AstronomicalSnapshot` as JSON
- `GET /api/events` — SSE stream; clients receive push updates after each cron run
- `POST /api/refresh` — triggers an immediate update (runs in a new thread)
- `GET /api/history?metric=X&limit=N` — historical graph data from MongoDB
- `POST /api/history/populate?days=N` — backfills historical data
- `DELETE /api/history` — clears MongoDB history collection

### MongoDB / History

Opt-in via `app.history.enabled=true`. `HistoryService` is `@ConditionalOnProperty` — it only loads when enabled. In production Docker, MongoDB runs on the internal Docker network (host port not exposed). `MongoConfig` manually constructs the client from `spring.data.mongodb.uri`.

### Claude AI Verification

`ClaudeMoonPhaseVerifier` calls the Claude API to independently verify the moon phase calculation. Its result is **logged only** — it never overrides the computed value. Configured via `CLAUDE_API_KEY`, `CLAUDE_MODEL` (default: `claude-sonnet-4-6`), `CLAUDE_API_URL`.

### Module Classes (`MoonPhaseModule/`)

Pure-calculation stateless classes, each responsible for one metric family. They take Julian Day Numbers (from `DateUtils`) and return computed values in standard units (AU for distances, km/s for speed, degrees for angles). `LeoDataFetcher` is the only one that makes external HTTP calls (to CelesTrak for satellite data).

## Key Configuration

| Property | Default | Notes |
|---|---|---|
| `spring.main.web-application-type` | `servlet` | Set to `none` for headless |
| `app.cron` | `0 1 0,12 * * *` | Run schedule (UTC) |
| `AWTRIXHOSTNAME` | `http://moonclock.bluelarma.com` | Awtrix device URL |
| `app.latitude` | `51.4769` | Used for daylight calculations |
| `app.history.enabled` | `false` | Enables MongoDB persistence |
| `CLAUDE_API_KEY` | _(none)_ | Enables Claude moon phase verification |

## Code Conventions

- No Lombok, no MapStruct — plain Java records for DTOs
- Logger: `LoggerFactory.getLogger(ClassName.class)`
- AU distance format: `String.format("%.6f", val)`
- `AstronomicalSnapshot` uses a builder pattern — always construct via `AstronomicalSnapshot.builder()...build()`
- `@ConditionalOnWebApplication` guards all web beans; `@ConditionalOnProperty` guards `HistoryService`
