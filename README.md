# MoonPhaseAI

MoonPhaseAI is a Java Spring Boot application that calculates astronomical data including moon phases, planetary distances, and solstice timings. It is designed to run as a scheduled background task and primarily outputs information to the console and pushes updates to an [Awtrix](https://awtrixdocs.blueforcer.de/) device.

## Functionality

The application performs the following calculations and updates twice daily (at 00:01 and 12:01):

### 1. Moon Phase
*   Calculates the current moon phase and the age of the moon (days since new moon).
*   Displays an ASCII art representation of the current moon phase in the logs.
*   Sends the moon phase name and icon to an Awtrix device.

### 2. Planetary Distances
*   **Sun-Earth**: Calculates the current distance in Astronomical Units (AU) and compares it against the minimum and maximum distances for the current year.
*   **Earth-Mars**: Calculates the current distance to Mars in AU and compares it against the annual min/max.
*   **Voyager Probes**: Estimates the current distance of Voyager 1 and Voyager 2 from Earth in AU.

### 3. Solstices & Equinoxes
*   Calculates the dates for the next Summer and Winter Solstices.
*   Sends the countdown (in days) to the Awtrix device for the upcoming solstice using custom icons ("SUMMER", "WINTER").

### 4. Daylight Calculation
*   Calculates the current length of daylight (in hours) for a specific latitude (default fixed to ~51.48° N, Greenwich).
*   Displays a relative bar showing where the current day length falls between the annual minimum and maximum.
*   Sends the current day length to the Awtrix device.

### 5. Awtrix Integration
*   The application pushes data to a custom API endpoint (`/api/custom`) on a local Awtrix server.