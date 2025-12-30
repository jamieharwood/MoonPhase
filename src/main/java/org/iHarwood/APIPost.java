package org.iHarwood;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class APIPost {
    public enum IconType {
        DONE("DONE"),
        SMILEY("SMILEY"),
        PAPKA("PAPKA"),
        DAYLENGTH("DAYLENGTH"),
        SUMMER("SUMMER"),
        WINTER("WINTER"),
        MARS("MARS");

        private final String value;

        IconType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private String name;
    private String text;
    private final String save;
    private final String effect;
    private final String icon;
    private String url;

    /**
     * Constructs an APIPost object with the specified parameters.
     *
     * @param appName the name of the application
     * @param text    the text to send in the POST request
     * @param url     the URL endpoint for the POST request
     */
    public APIPost(String appName, String text, String url, String save, String effect, String icon) {
        this.name = appName;
        this.text = text;
        this.url = url;
        this.save = save;
        this.effect = effect;
        this.icon = icon;
    }

    /**
     * Sends a POST request with appName and text parameters in JSON format to the specified URL.
     *
     * @return the HTTP response code
     * @throws IOException if an I/O error occurs
     */
    public int sendPost() throws IOException {
        URL endpoint = new URL(this.url);
        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create the JSON request body using Gson
        Gson gson = new Gson();
        String jsonBody = gson.toJson(new APIPostData(this.name, this.text, this.save, this.effect, this.icon));

        // Send the request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        return connection.getResponseCode();
    }

    /**
         * Inner class to represent the JSON structure for Gson serialization.
         */
        private record APIPostData(String name, String text, String save, String effect, String icon) {
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

