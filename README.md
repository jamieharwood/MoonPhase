# MoonPhaseAI

MoonPhaseAI is a Java Spring Boot application that calculates astronomical data including moon phases, planetary distances, and solstice timings. It is designed to run as a scheduled background task and primarily outputs information to the console and pushes updates to an [Awtrix](https://awtrixdocs.blueforcer.de/) device.

## Functionality

The application performs the following calculations and updates on a configurable schedule (default: 00:01 and 12:01 daily):

### 1. Moon Phase
*   Calculates the current moon phase, age of the moon (days since new moon), and illumination percentage.
*   Displays an ASCII art representation of the current moon phase in the logs.
*   Verifies the calculated phase against the Claude AI API and corrects it if there is a mismatch.
*   Sends the moon phase name, illumination %, and days until next full moon to the Awtrix device.

### 2. Planetary Distances
*   **Sun-Earth**: Calculates the current distance in Astronomical Units (AU) and compares it against the minimum and maximum distances for the current year.
*   **Earth-Mars**: Calculates the current distance to Mars in AU and compares it against the annual min/max.
*   **Voyager Probes**: Estimates the current distance of Voyager 1 and Voyager 2 from Earth in AU and pushes both to the Awtrix device.

### 3. Solstices & Equinoxes
*   Calculates the dates for the next Summer and Winter Solstices.
*   Sends the countdown (in days) to the Awtrix device for the upcoming solstice using custom icons ("SUMMER", "WINTER").

### 4. Daylight Calculation
*   Calculates the current length of daylight (in hours) for a configurable latitude (default: 51.4769° N, Greenwich).
*   Displays a relative bar showing where the current day length falls between the annual minimum and maximum.
*   Sends the current day length to the Awtrix device.

### 5. Awtrix Integration
*   Checks connectivity to the Awtrix device at startup and warns if it is unreachable.
*   Pushes data to the `/api/custom` endpoint on the configured Awtrix server.

## Configuration

All configuration is via environment variables. The following variables are supported:

| Variable | Default | Description |
|---|---|---|
| `AWTRIXHOSTNAME` | `http://moonclock.local` | Base URL of the Awtrix device |
| `LATITUDE` | `51.4769` | Latitude in decimal degrees (positive = North) for daylight calculations |
| `CRON_SCHEDULE` | `0 1 0,12 * * *` | Spring cron expression for the update schedule |
| `CLAUDE_API_KEY` | _(none)_ | Anthropic API key for moon phase verification (optional) |
| `CLAUDE_MODEL` | `claude-sonnet-4-6` | Claude model ID to use for moon phase verification |

## Running with Docker

The recommended way to run MoonPhaseAI is via Docker. Create an `env` file (do not commit this to version control):

```
AWTRIXHOSTNAME=http://your-awtrix-device.local
LATITUDE=53.4808
CLAUDE_API_KEY=your-api-key-here
```

Then start the container:

```bash
docker compose up -d
```

Or pull the latest image directly from Docker Hub:

```bash
docker pull jamieharwood/moonphase:1.0-SNAPSHOT
```

## Building

```bash
./gradlew bootJar
```

Or build the Docker image:

```bash
docker build -t jamieharwood/moonphase:1.0-SNAPSHOT .
```

## Awtrix Apps

The following named apps are pushed to the Awtrix device:

| App Name | Content | Icon |
|---|---|---|
| `moonphase` | Current phase name | Phase icon |
| `moonillumination` | Illumination % | Phase icon |
| `fullmoon` | Days until next full moon | `FullMoon` |
| `marsDistanceAu` | Earth-Mars distance in AU | `MARS` |
| `voyager1` | Voyager 1 distance in AU | `VOYAGER` |
| `voyager2` | Voyager 2 distance in AU | `VOYAGER` |
| `summersolstice` | Days until summer solstice | `SUMMER` |
| `wintersolstice` | Days until winter solstice | `WINTER` |
| `CurrentDayLength` | Current daylight hours | `DAYLENGTH` |
