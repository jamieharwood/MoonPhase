
import java.net.URL;

public class CheckURL {
    public static void main(String[] args) throws Exception {
        String s = "http://awtrix_2e6f60.local/api/custom";
        URL url = new URL(s);
        System.out.println("URL host: " + url.getHost());

        java.net.URI uri = java.net.URI.create(s);
        System.out.println("URI host: " + uri.getHost());
    }
}

