/*
 * BiglyBT Extreme Mod - HTTP Interception Layer
 * 
 * URLStreamHandlerFactory approach for Java 9+ compatibility.
 * This replaces the SPI (URLStreamHandlerProvider) approach which had
 * recursion issues due to URL handler caching.
 * 
 * The factory is registered once at application startup via
 * URL.setURLStreamHandlerFactory() and never recurses because:
 * 1. Factory is consulted FIRST (before SPI)
 * 2. It's a single static instance
 * 3. Our handlers use direct instantiation of JDK classes
 * 
 * Requires JVM flags:
 *   --add-opens java.base/java.net=ALL-UNNAMED
 *   --add-opens java.base/sun.net.www.protocol.http=ALL-UNNAMED
 *   --add-opens java.base/sun.net.www.protocol.https=ALL-UNNAMED
 */
package ghostfucker.http;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * URLStreamHandlerFactory for spoofing HTTP/HTTPS connections.
 * 
 * This is the modern (Java 9+) replacement for:
 * - Extending sun.net.www.protocol.* classes (blocked by module system)
 * - URLStreamHandlerProvider SPI (has recursion issues)
 * 
 * Must be registered early in application startup:
 * {@code SpoofingURLStreamHandlerFactory.install();}
 */
public class SpoofingURLStreamHandlerFactory implements URLStreamHandlerFactory {
    
    private static volatile boolean installed = false;
    private static volatile boolean enabled = true;
    
    /** Singleton instance */
    private static final SpoofingURLStreamHandlerFactory INSTANCE = new SpoofingURLStreamHandlerFactory();
    
    private SpoofingURLStreamHandlerFactory() {
        // Private constructor - use install()
    }
    
    /**
     * Install the spoofing factory. Must be called once at application startup,
     * BEFORE any URL objects are created.
     * 
     * @return true if successfully installed, false if already installed or failed
     */
    public static synchronized boolean install() {
        if (installed) {
            return true; // Already installed
        }
        
        try {
            URL.setURLStreamHandlerFactory(INSTANCE);
            installed = true;
            System.out.println("[GhostFucker] HTTP interception layer installed");
            return true;
        } catch (Error e) {
            // Factory already set by something else
            System.err.println("[GhostFucker] Failed to install HTTP interception: " + e.getMessage());
            System.err.println("[GhostFucker] Another URLStreamHandlerFactory is already registered.");
            System.err.println("[GhostFucker] HTTP header spoofing will be disabled.");
            return false;
        }
    }
    
    /**
     * Check if the factory has been installed.
     */
    public static boolean isInstalled() {
        return installed;
    }
    
    /**
     * Enable or disable spoofing globally.
     * When disabled, returns null to let default handlers take over.
     */
    public static void setEnabled(boolean enabled) {
        SpoofingURLStreamHandlerFactory.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (!enabled) {
            return null; // Spoofing disabled, use default handlers
        }
        
        if ("http".equalsIgnoreCase(protocol)) {
            return new SpoofingHttpHandler();
        } else if ("https".equalsIgnoreCase(protocol)) {
            return new SpoofingHttpsHandler();
        }
        
        // Not our protocol - return null to let JDK use default handler
        return null;
    }
}
