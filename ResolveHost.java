
import java.net.InetAddress;

public class ResolveHost {
    public static void main(String[] args) {
        String host = "awtrix_2e6f60.local";
        try {
            InetAddress addr = InetAddress.getByName(host);
            System.out.println("Resolved: " + addr.getHostAddress());
        } catch (Exception e) {
            System.out.println("Failed to resolve: " + e);
        }
    }
}

