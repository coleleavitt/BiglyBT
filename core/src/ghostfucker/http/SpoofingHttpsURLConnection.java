package ghostfucker.http;

import ghostfucker.spoof.PerfectSpoof;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

public class SpoofingHttpsURLConnection extends HttpsURLConnection {
    
    private static final Constructor<? extends HttpsURLConnection> HTTPS_CONNECTION_CTOR;
    private static final Object HTTPS_HANDLER;
    private static volatile SSLContext permissiveSSLContext;
    private static volatile boolean sslBypassEnabled = true;
    
    private static final HostnameVerifier PERMISSIVE_HOSTNAME_VERIFIER = (hostname, session) -> true;
    
    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[] {
        new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }
            
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
            
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }
    };
    
    static {
        Constructor<? extends HttpsURLConnection> ctor = null;
        Object handler = null;
        try {
            Class<?> handlerClass = Class.forName("sun.net.www.protocol.https.Handler");
            handler = handlerClass.getDeclaredConstructor().newInstance();
            
            @SuppressWarnings("unchecked")
            Class<? extends HttpsURLConnection> clazz = 
                (Class<? extends HttpsURLConnection>) Class.forName("sun.net.www.protocol.https.HttpsURLConnectionImpl");
            ctor = clazz.getDeclaredConstructor(URL.class, java.net.Proxy.class, handlerClass);
            ctor.setAccessible(true);
        } catch (Exception e) {
            System.err.println("[GhostFucker] Failed to get HttpsURLConnection constructor: " + e.getMessage());
            System.err.println("[GhostFucker] Ensure JVM has: --add-opens java.base/sun.net.www.protocol.https=ALL-UNNAMED");
        }
        HTTPS_CONNECTION_CTOR = ctor;
        HTTPS_HANDLER = handler;
        
        initializePermissiveSSLContext();
    }
    
    private static void initializePermissiveSSLContext() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, TRUST_ALL_CERTS, new SecureRandom());
            permissiveSSLContext = ctx;
        } catch (Exception e) {
            System.err.println("[GhostFucker] Failed to initialize permissive SSL context: " + e.getMessage());
        }
    }
    
    public static void setSSLBypassEnabled(boolean enabled) {
        sslBypassEnabled = enabled;
    }
    
    public static boolean isSSLBypassEnabled() {
        return sslBypassEnabled;
    }
    
    protected HttpsURLConnection delegate;
    private boolean headersSpoofed = false;
    
    public SpoofingHttpsURLConnection(URL url, Proxy proxy) throws IOException {
        super(url);
        this.delegate = createRealConnection(url, proxy);
        
        if (sslBypassEnabled && permissiveSSLContext != null) {
            delegate.setSSLSocketFactory(permissiveSSLContext.getSocketFactory());
            delegate.setHostnameVerifier(PERMISSIVE_HOSTNAME_VERIFIER);
        }
    }
    
    private HttpsURLConnection createRealConnection(URL url, Proxy proxy) throws IOException {
        if (HTTPS_CONNECTION_CTOR == null || HTTPS_HANDLER == null) {
            throw new IOException("HTTPS interception not available - missing JVM --add-opens flags");
        }
        
        try {
            Proxy effectiveProxy = (proxy != null) ? proxy : Proxy.NO_PROXY;
            return HTTPS_CONNECTION_CTOR.newInstance(url, effectiveProxy, HTTPS_HANDLER);
        } catch (Exception e) {
            throw new IOException("Failed to create HTTPS connection: " + e.getMessage(), e);
        }
    }
    
    private byte detectRequestType() {
        String path = url.getPath();
        if (path != null && path.contains("scrape")) {
            return 1;
        }
        return 0;
    }
    
    private void injectSpoofedHeaders() {
        if (headersSpoofed) {
            return;
        }
        headersSpoofed = true;
        
        PerfectSpoof spoof = PerfectSpoof.getInstance();
        if (spoof == null || !spoof.isEnabled()) {
            return;
        }
        
        byte requestType = detectRequestType();
        String[][] headers = spoof.getHttpHeaders(requestType);
        
        if (headers == null) {
            return;
        }
        
        String host = url.getHost();
        int port = url.getPort();
        String portStr = "";
        
        if (port != -1 && port != 443) {
            portStr = ":" + port;
        } else if (spoof.shouldDisplayDefaultPort()) {
            portStr = ":443";
        }
        
        for (String[] header : headers) {
            if (header == null || header.length < 2) {
                continue;
            }
            
            String name = header[0];
            String value = header[1];
            
            if (name == null || value == null) {
                continue;
            }
            
            value = value.replace("{host}", host);
            value = value.replace(":{port}", portStr);
            
            try {
                delegate.setRequestProperty(name, value);
            } catch (Exception ignored) {
            }
        }
    }
    
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
