
import java.net.*;
import java.io.IOException;

public class QuickCheckFix {
    public static void main(String[] args) {
        String urlString = "http://awtrix_2e6f60.local/api/custom?name=test";
        try {
            URI uri = resolveUri(urlString);
            System.out.println("Resolved URI: " + uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static URI resolveUri(String urlString) throws IOException {
        URI uri;
        try {
            uri = URI.create(urlString);
            // check if host is parsed. RFC 2396 parsers return null host for underscores
            if (uri.getHost() != null) {
                return uri;
            }
        } catch (IllegalArgumentException ignored) {
        }

        System.out.println("URI.create failed or returned null host, trying fallback...");

        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            System.out.println("URL class parsed host: " + host);

            InetAddress address = InetAddress.getByName(host);
            String ip = address.getHostAddress();
            System.out.println("Resolved IP: " + ip);

            // Reconstruct URI with IP.
            // Simple replaceFirst might be dangerous if host appears in path, but usually safe for hostname.
            // Better to reconstruct using URI constructor if possible, but URL replacement is easier.
            // Let's use URL to reconstruct.
            URL newUrl = new URL(url.getProtocol(), ip, url.getPort(), url.getFile());
            return newUrl.toURI();

        } catch (Exception e) {
            throw new IOException("Failed to resolve invalid URI: " + urlString, e);
        }
    }
}

