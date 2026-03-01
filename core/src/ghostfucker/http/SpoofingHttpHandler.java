/*
 * BiglyBT Extreme Mod - HTTP Handler
 * 
 * Custom URLStreamHandler for HTTP protocol that returns
 * SpoofingHttpURLConnection instances with header spoofing capabilities.
 */
package ghostfucker.http;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * HTTP protocol handler that creates spoofing-capable connections.
 * 
 * Replaces sun.net.www.protocol.http.Handler
 */
public class SpoofingHttpHandler extends URLStreamHandler {
    
    protected String proxy;
    protected int proxyPort;
    
    public SpoofingHttpHandler() {
        this.proxy = null;
        this.proxyPort = -1;
    }
    
    public SpoofingHttpHandler(String proxy, int port) {
        this.proxy = proxy;
        this.proxyPort = port;
    }
    
    @Override
    protected int getDefaultPort() {
        return 80;
    }
    
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return openConnection(url, null);
    }
    
    @Override
    protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        return new SpoofingHttpURLConnection(url, proxy);
    }
}
