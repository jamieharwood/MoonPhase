
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

public class QuickCheckURI {
    public static void main(String[] args) {
        String urlStr = "http://awtrix_2e6f60.local/api/custom?name=marsDistanceAu";

        System.out.println("Testing URI: " + urlStr);

        try {
            URI uri = URI.create(urlStr);
            System.out.println("URI.create() successful: " + uri);
            System.out.println("Host: " + uri.getHost());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .build();
            System.out.println("HttpRequest builder successful");

        } catch (Exception e) {
            System.out.println("Error: " + e);
            e.printStackTrace();
        }
    }
}

