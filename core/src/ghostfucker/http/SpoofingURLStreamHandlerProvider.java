/*
 * BiglyBT Extreme Mod - HTTP Interception Layer
 * 
 * Modern Java 9+ approach using URLStreamHandlerProvider SPI.
 * Registers via META-INF/services/java.net.spi.URLStreamHandlerProvider
 * 
 * This replaces the old sun.net.www.protocol.* extension approach
 * which is blocked in modern Java due to module encapsulation.
 */
package ghostfucker.http;

import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;

/**
 * SPI provider for spoofing HTTP/HTTPS connections.
 * 
 * Automatically discovered by Java's ServiceLoader mechanism when registered
 * in META-INF/services/java.net.spi.URLStreamHandlerProvider
 * 
 * This is the modern (Java 9+) replacement for:
 * - URL.setURLStreamHandlerFactory() (can only be called once)
 * - Extending sun.net.www.protocol.* classes (blocked by module system)
 */
public class SpoofingURLStreamHandlerProvider extends URLStreamHandlerProvider {
    
    private static volatile boolean enabled = true;
    
    /**
     * Enable or disable spoofing globally.
     * When disabled, returns null to let default handlers take over.
     */
    public static void setEnabled(boolean enabled) {
        SpoofingURLStreamHandlerProvider.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (!enabled) {
            return null; // Let default handler handle it
        }
        
        if ("http".equalsIgnoreCase(protocol)) {
            return new SpoofingHttpHandler();
        } else if ("https".equalsIgnoreCase(protocol)) {
            return new SpoofingHttpsHandler();
        }
        
        return null; // Not our protocol, let others handle it
    }
}
