/*
 * BiglyBT Extreme Mod - Spoofing HTTPS Connection
 * 
 * Wraps the real HttpsURLConnection and intercepts header operations
 * to inject spoofed headers. Also provides SSL bypass capability.
 */
package ghostfucker.http;

import ghostfucker.spoof.PerfectSpoof;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * HTTPS connection wrapper that injects spoofed headers and provides SSL bypass.
 * 
 * This class wraps the real HttpsURLConnection and intercepts header operations
 * to apply spoofing based on the current client profile.
 */
public class SpoofingHttpsURLConnection extends HttpsURLConnection {
    
    /** The real HTTPS connection being wrapped */
    protected HttpsURLConnection delegate;
    
    /** Whether headers have been spoofed for this connection */
    private boolean headersSpoofed = false;
    
    /** Whether SSL bypass is enabled */
    private static volatile boolean sslBypassEnabled = true;
    
    /** Shared permissive SSL context */
    private static volatile SSLContext permissiveSSLContext;
    
    /** Shared permissive hostname verifier */
    private static final HostnameVerifier PERMISSIVE_HOSTNAME_VERIFIER = (hostname, session) -> true;
    
    /** Trust manager that accepts all certificates */
    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[] {
        new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // Accept all
            }
            
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // Accept all
            }
            
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }
    };
    
    static {
        initializePermissiveSSLContext();
    }
    
    private static void initializePermissiveSSLContext() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, TRUST_ALL_CERTS, new SecureRandom());
            permissiveSSLContext = ctx;
        } catch (Exception e) {
            System.err.println("Failed to initialize permissive SSL context: " + e.getMessage());
        }
    }
    
    /**
     * Enable or disable SSL certificate verification bypass.
     */
    public static void setSSLBypassEnabled(boolean enabled) {
        sslBypassEnabled = enabled;
    }
    
    public static boolean isSSLBypassEnabled() {
        return sslBypassEnabled;
    }
    
    public SpoofingHttpsURLConnection(URL url, Proxy proxy) throws IOException {
        super(url);
        
        // Create the real connection using reflection to access internal class
        this.delegate = createRealConnection(url, proxy);
        
        // Apply SSL bypass if enabled
        if (sslBypassEnabled && permissiveSSLContext != null) {
            delegate.setSSLSocketFactory(permissiveSSLContext.getSocketFactory());
            delegate.setHostnameVerifier(PERMISSIVE_HOSTNAME_VERIFIER);
        }
    }
    
    /**
     * Creates the real HttpsURLConnection using the default handler.
     */
    private HttpsURLConnection createRealConnection(URL url, Proxy proxy) throws IOException {
        try {
            // Use reflection to instantiate the real sun.net.www.protocol.https.Handler
            Class<?> handlerClass = Class.forName("sun.net.www.protocol.https.Handler");
            java.net.URLStreamHandler handler = (java.net.URLStreamHandler) handlerClass.getDeclaredConstructor().newInstance();
            
            // Create URL with our handler to bypass SPI lookup
            URL realUrl = new URL(url, url.toString(), handler);
            
            if (proxy != null) {
                return (HttpsURLConnection) realUrl.openConnection(proxy);
            } else {
                return (HttpsURLConnection) realUrl.openConnection();
            }
        } catch (ReflectiveOperationException e) {
            throw new IOException("Failed to create real HTTPS connection", e);
        }
    }
    
    /**
     * Determines the request type based on URL path.
     */
    private byte detectRequestType() {
        String path = url.getPath();
        if (path != null && path.contains("scrape")) {
            return 1; // SCRAPE
        }
        return 0; // ANNOUNCE (default)
    }
    
    /**
     * Injects spoofed headers before the connection is made.
     */
    private void injectSpoofedHeaders() {
        if (headersSpoofed) {
            return;
        }
        headersSpoofed = true;
        
        PerfectSpoof spoof = PerfectSpoof.getInstance();
        if (spoof == null || !spoof.isEnabled()) {
            return;
        }
        
        // Get the spoofed headers for this request type
        byte requestType = detectRequestType();
        String[][] headers = spoof.getHttpHeaders(requestType);
        
        if (headers == null) {
            return;
        }
        
        // Build host header value
        String host = url.getHost();
        int port = url.getPort();
        String portStr = "";
        
        if (port != -1 && port != 443) {
            portStr = ":" + port;
        } else if (spoof.shouldDisplayDefaultPort()) {
            portStr = ":443";
        }
        
        // Apply each header
        for (String[] header : headers) {
            if (header == null || header.length < 2) {
                continue;
            }
            
            String name = header[0];
            String value = header[1];
            
            if (name == null || value == null) {
                continue;
            }
            
            // Replace placeholders
            value = value.replace("{host}", host);
            value = value.replace(":{port}", portStr);
            
            // Set the header on the real connection
            try {
                delegate.setRequestProperty(name, value);
            } catch (Exception e) {
                // Ignore errors setting headers
            }
        }
    }
    
    // ========== HttpsURLConnection-specific methods ==========
    
    @Override
    public String getCipherSuite() {
        return delegate.getCipherSuite();
    }
    
    @Override
    public Certificate[] getLocalCertificates() {
        return delegate.getLocalCertificates();
    }
    
    @Override
    public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
        return delegate.getServerCertificates();
    }
    
    @Override
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return delegate.getPeerPrincipal();
    }
    
    @Override
    public Principal getLocalPrincipal() {
        return delegate.getLocalPrincipal();
    }
    
    @Override
    public void setHostnameVerifier(HostnameVerifier v) {
        delegate.setHostnameVerifier(v);
    }
    
    @Override
    public HostnameVerifier getHostnameVerifier() {
        return delegate.getHostnameVerifier();
    }
    
    @Override
    public void setSSLSocketFactory(SSLSocketFactory sf) {
        delegate.setSSLSocketFactory(sf);
    }
    
    @Override
    public SSLSocketFactory getSSLSocketFactory() {
        return delegate.getSSLSocketFactory();
    }
    
    // ========== Override connection methods to inject headers ==========
    
    @Override
    public void connect() throws IOException {
        injectSpoofedHeaders();
        delegate.connect();
        connected = true;
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        injectSpoofedHeaders();
        return delegate.getInputStream();
    }
    
    @Override
    public OutputStream getOutputStream() throws IOException {
        injectSpoofedHeaders();
        return delegate.getOutputStream();
    }
    
    @Override
    public int getResponseCode() throws IOException {
        injectSpoofedHeaders();
        return delegate.getResponseCode();
    }
    
    @Override
    public String getResponseMessage() throws IOException {
        injectSpoofedHeaders();
        return delegate.getResponseMessage();
    }
    
    // ========== Delegate all other methods to the real connection ==========
    
    @Override
    public void disconnect() {
        delegate.disconnect();
    }
    
    @Override
    public boolean usingProxy() {
        return delegate.usingProxy();
    }
    
    @Override
    public void setRequestMethod(String method) throws ProtocolException {
        delegate.setRequestMethod(method);
    }
    
    @Override
    public String getRequestMethod() {
        return delegate.getRequestMethod();
    }
    
    @Override
    public void setRequestProperty(String key, String value) {
        delegate.setRequestProperty(key, value);
    }
    
    @Override
    public void addRequestProperty(String key, String value) {
        delegate.addRequestProperty(key, value);
    }
    
    @Override
    public String getRequestProperty(String key) {
        return delegate.getRequestProperty(key);
    }
    
    @Override
    public Map<String, List<String>> getRequestProperties() {
        return delegate.getRequestProperties();
    }
    
    @Override
    public void setFixedLengthStreamingMode(int contentLength) {
        delegate.setFixedLengthStreamingMode(contentLength);
    }
    
    @Override
    public void setFixedLengthStreamingMode(long contentLength) {
        delegate.setFixedLengthStreamingMode(contentLength);
    }
    
    @Override
    public void setChunkedStreamingMode(int chunklen) {
        delegate.setChunkedStreamingMode(chunklen);
    }
    
    @Override
    public String getHeaderField(int n) {
        return delegate.getHeaderField(n);
    }
    
    @Override
    public String getHeaderFieldKey(int n) {
        return delegate.getHeaderFieldKey(n);
    }
    
    @Override
    public String getHeaderField(String name) {
        return delegate.getHeaderField(name);
    }
    
    @Override
    public Map<String, List<String>> getHeaderFields() {
        return delegate.getHeaderFields();
    }
    
    @Override
    public int getHeaderFieldInt(String name, int Default) {
        return delegate.getHeaderFieldInt(name, Default);
    }
    
    @Override
    public long getHeaderFieldLong(String name, long Default) {
        return delegate.getHeaderFieldLong(name, Default);
    }
    
    @Override
    public long getHeaderFieldDate(String name, long Default) {
        return delegate.getHeaderFieldDate(name, Default);
    }
    
    @Override
    public InputStream getErrorStream() {
        return delegate.getErrorStream();
    }
    
    @Override
    public void setInstanceFollowRedirects(boolean followRedirects) {
        delegate.setInstanceFollowRedirects(followRedirects);
    }
    
    @Override
    public boolean getInstanceFollowRedirects() {
        return delegate.getInstanceFollowRedirects();
    }
    
    @Override
    public void setConnectTimeout(int timeout) {
        delegate.setConnectTimeout(timeout);
    }
    
    @Override
    public int getConnectTimeout() {
        return delegate.getConnectTimeout();
    }
    
    @Override
    public void setReadTimeout(int timeout) {
        delegate.setReadTimeout(timeout);
    }
    
    @Override
    public int getReadTimeout() {
        return delegate.getReadTimeout();
    }
    
    @Override
    public URL getURL() {
        return delegate.getURL();
    }
    
    @Override
    public int getContentLength() {
        return delegate.getContentLength();
    }
    
    @Override
    public long getContentLengthLong() {
        return delegate.getContentLengthLong();
    }
    
    @Override
    public String getContentType() {
        return delegate.getContentType();
    }
    
    @Override
    public String getContentEncoding() {
        return delegate.getContentEncoding();
    }
    
    @Override
    public long getExpiration() {
        return delegate.getExpiration();
    }
    
    @Override
    public long getDate() {
        return delegate.getDate();
    }
    
    @Override
    public long getLastModified() {
        return delegate.getLastModified();
    }
    
    @Override
    public Object getContent() throws IOException {
        injectSpoofedHeaders();
        return delegate.getContent();
    }
    
    @Override
    public Object getContent(Class<?>[] classes) throws IOException {
        injectSpoofedHeaders();
        return delegate.getContent(classes);
    }
    
    @Override
    public void setDoInput(boolean doinput) {
        delegate.setDoInput(doinput);
    }
    
    @Override
    public boolean getDoInput() {
        return delegate.getDoInput();
    }
    
    @Override
    public void setDoOutput(boolean dooutput) {
        delegate.setDoOutput(dooutput);
    }
    
    @Override
    public boolean getDoOutput() {
        return delegate.getDoOutput();
    }
    
    @Override
    public void setAllowUserInteraction(boolean allowuserinteraction) {
        delegate.setAllowUserInteraction(allowuserinteraction);
    }
    
    @Override
    public boolean getAllowUserInteraction() {
        return delegate.getAllowUserInteraction();
    }
    
    @Override
    public void setUseCaches(boolean usecaches) {
        delegate.setUseCaches(usecaches);
    }
    
    @Override
    public boolean getUseCaches() {
        return delegate.getUseCaches();
    }
    
    @Override
    public void setIfModifiedSince(long ifmodifiedsince) {
        delegate.setIfModifiedSince(ifmodifiedsince);
    }
    
    @Override
    public long getIfModifiedSince() {
        return delegate.getIfModifiedSince();
    }
    
    @Override
    public boolean getDefaultUseCaches() {
        return delegate.getDefaultUseCaches();
    }
    
    @Override
    public void setDefaultUseCaches(boolean defaultusecaches) {
        delegate.setDefaultUseCaches(defaultusecaches);
    }
    
    @Override
    public String toString() {
        return "SpoofingHttpsURLConnection[" + delegate.toString() + "]";
    }
}
