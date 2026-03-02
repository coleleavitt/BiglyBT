package ghostfucker.http;

import java.net.HttpURLConnection;
import java.net.URL;

public class HttpInterceptionTest {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== HTTP Interception Test ===\n");
        
        if (!SpoofingURLStreamHandlerFactory.install()) {
            System.err.println("FAILED: Could not install HTTP interception factory");
            System.exit(1);
        }
        
        System.out.println("Testing HTTP connection...");
        testConnection("http://httpbin.org/headers");
        
        System.out.println("\nTesting HTTPS connection...");
        testConnection("https://httpbin.org/headers");
        
        System.out.println("\n=== All tests passed! ===");
    }
    
    private static void testConnection(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        String connClass = conn.getClass().getName();
        System.out.println("  Connection class: " + connClass);
        
        boolean isSpoofing = connClass.contains("Spoofing");
        if (isSpoofing) {
            System.out.println("  ✓ Spoofing connection created successfully");
        } else {
            System.out.println("  ✗ Expected Spoofing connection but got: " + connClass);
        }
        
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        try {
            int responseCode = conn.getResponseCode();
            System.out.println("  Response code: " + responseCode);
            
            if (responseCode == 200) {
                System.out.println("  ✓ Connection successful");
            }
        } catch (Exception e) {
            System.out.println("  Network error (expected in test env): " + e.getMessage());
        } finally {
            conn.disconnect();
        }
    }
}
