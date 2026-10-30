package org.iHarwood;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
public final class APIPost {
    public enum IconType {
        DONE("DONE"),
        SMILEY("SMILEY"),
        PAPKA("PAPKA"),
        DAYLENGTH("DAYLENGTH"),
        SUMMER("SUMMER"),
        WINTER("WINTER"),
        MARS("MARS"),
        VOYAGER("VOYAGER"),
        JUPITER("JUPITER"),
        SATURN("SATURN"),
        EARTH("EARTH"),
        MOON("MOON"),
        LIGHT("LIGHT"),
        NEWHORIZONS("NEWHORIZONS"),
        PERIHELION("PERIHELION"),
        AURORA("AURORA");
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
    private static final JsonMapper OBJECT_MAPPER = JsonMapper.shared();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
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
    public int sendPost() throws IOException, InterruptedException {
        String jsonBody = OBJECT_MAPPER.writeValueAsString(
                new APIPostData(this.name, this.text, this.save, this.effect, this.icon));
        URI targetUri = resolveUri(this.url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(targetUri)
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }
    private URI resolveUri(String urlString) throws IOException {
        try {
            URI uri = URI.create(urlString);
            if (uri.getHost() != null) {
                return uri;
            }
        } catch (IllegalArgumentException ignored) {
            // Illegal URI, try to fix it
        }
        // Fallback for hostnames with underscores (which URI class rejects):
        // Parse manually and resolve the host to an IP address.
        try {
            // Split scheme://host[:port]/path][?query]
            int schemeEnd = urlString.indexOf("://");
            if (schemeEnd < 0) {
                throw new IOException("Missing URI scheme: " + urlString);
            }
            String scheme = urlString.substring(0, schemeEnd);
            String rest = urlString.substring(schemeEnd + 3);
            int pathStart = rest.indexOf('/');
            String authority = pathStart >= 0 ? rest.substring(0, pathStart) : rest;
            String pathAndQuery = pathStart >= 0 ? rest.substring(pathStart) : "";
            String host;
            int port = -1;
            int colon = authority.lastIndexOf(':');
            if (colon > 0 && authority.indexOf(']') < colon) {
                host = authority.substring(0, colon);
                port = Integer.parseInt(authority.substring(colon + 1));
            } else {
                host = authority;
            }
            if (host == null || host.isBlank()) {
                throw new IOException("Missing host in URI: " + urlString);
            }
            InetAddress address = InetAddress.getByName(host);
            String ip = address.getHostAddress();
            String portPart = (port == -1) ? "" : ":" + port;
            return URI.create(scheme + "://" + ip + portPart + pathAndQuery);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to resolve invalid URI: " + urlString, e);
        }
    }
    /**
     * Inner class to represent the JSON structure for serialization.
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
