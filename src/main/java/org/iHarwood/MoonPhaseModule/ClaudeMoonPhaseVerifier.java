package org.iHarwood.MoonPhaseModule;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Verifies the calculated moon phase by calling the Claude API.
 * Uses the CLAUDE_API_KEY environment variable for authentication.
 */
public final class ClaudeMoonPhaseVerifier {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeMoonPhaseVerifier.class);

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String CLAUDE_MODEL = "claude-sonnet-4-6";
    private static final String API_KEY_ENV_VAR = "CLAUDE_API_KEY";

    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * Result record holding Claude's verification response.
     */
    public record VerificationResult(String claudePhase, String calculatedPhase, boolean matches, String details) {
    }

    /**
     * Calls Claude API to verify the moon phase for the given date and compares
     * it against the locally calculated phase name.
     *
     * @param date            the date to check
     * @param calculatedPhase the phase name from the local MoonPhase calculation
     * @return a VerificationResult with Claude's answer and whether it matches
     */
    public static VerificationResult verify(LocalDate date, String calculatedPhase) {
        String apiKey = System.getenv(API_KEY_ENV_VAR);

        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("CLAUDE_API_KEY environment variable not set – skipping Claude verification");
            return new VerificationResult("N/A", calculatedPhase, false,
                    "Skipped: CLAUDE_API_KEY not configured");
        }

        try {
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String prompt = String.format(
                    "What is the moon phase on %s? Reply with ONLY the phase name using one of these exact values: " +
                    "Full Moon, Waning Gibbous, Last Quarter, Waning Crescent, New Moon, Waxing Crescent, First Quarter, Waxing Gibbous. " +
                    "Do not include any other text, explanation, or punctuation.", dateStr);

            // Build the request JSON
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", prompt);

            JsonArray messages = new JsonArray();
            messages.add(userMessage);

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", CLAUDE_MODEL);
            requestBody.addProperty("max_tokens", 50);
            requestBody.add("messages", messages);

            String jsonBody = new Gson().toJson(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLAUDE_API_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.warn("Claude API returned status {}: {}", response.statusCode(), response.body());
                return new VerificationResult("Error", calculatedPhase, false,
                        "Claude API error: HTTP " + response.statusCode());
            }

            // Parse the response
            String claudePhase = extractPhaseFromResponse(response.body());
            boolean matches = normalise(claudePhase).equals(normalise(calculatedPhase));

            return new VerificationResult(claudePhase, calculatedPhase, matches,
                    matches ? "Claude confirms the calculated phase" : "Mismatch detected");

        } catch (Exception e) {
            logger.warn("Claude verification failed: {}", e.getMessage());
            return new VerificationResult("Error", calculatedPhase, false,
                    "Exception: " + e.getMessage());
        }
    }

    /**
     * Extracts the text content from Claude's API response JSON.
     */
    private static String extractPhaseFromResponse(String responseJson) {
        JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
        JsonArray content = root.getAsJsonArray("content");
        if (content != null && !content.isEmpty()) {
            JsonObject first = content.get(0).getAsJsonObject();
            return first.get("text").getAsString().trim();
        }
        return "Unknown";
    }

    /**
     * Normalises a phase name for comparison (lowercase, trimmed).
     */
    private static String normalise(String phase) {
        return phase == null ? "" : phase.trim().toLowerCase();
    }
}

