/*
 * BiglyBT Extreme Mod - HTTPS Handler
 * 
 * Custom URLStreamHandler for HTTPS protocol that returns
 * SpoofingHttpsURLConnection instances with header spoofing
 * and SSL bypass capabilities.
 */
package ghostfucker.http;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * HTTPS protocol handler that creates spoofing-capable connections.
 * 
 * Replaces sun.net.www.protocol.https.Handler
 */
public class SpoofingHttpsHandler extends URLStreamHandler {
    
    protected String proxy;
    protected int proxyPort;
    
    public SpoofingHttpsHandler() {
        this.proxy = null;
        this.proxyPort = -1;
    }
    
    public SpoofingHttpsHandler(String proxy, int port) {
        this.proxy = proxy;
        this.proxyPort = port;
    }
    
    @Override
    protected int getDefaultPort() {
        return 443;
    }
    
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return openConnection(url, null);
    }
    
    @Override
    protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        return new SpoofingHttpsURLConnection(url, proxy);
    }
}
