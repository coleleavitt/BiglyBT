package ghostfucker.misc;

import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.ui.swt.mainwindow.SWTThread;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.eclipse.swt.widgets.Display;

public class SBILogin implements Runnable {
	public static boolean loginValid = true;
	public static boolean deactivateLogin = true;
	public static String loginResult = "";
	private Runnable sbiBlock;
	private static volatile boolean busy = false;
	private static final String SBI_URL = "www.sb-innovation.de/login.php";
	private static final String DO_ARG = "do=vuzeCheck&";
	private static final String NAME_ARG = "vb_login_username=";
	private static final String PASSWORD_ARG = "vb_login_md5password=";
	private static final char LOGIN_INVALID = '0';
	private static final char LOGIN_VALID = '1';
	private static final char LOGIN_BANNED = '2';
	private static final char LOGIN_INACTIVE = '3';
	private static final char LOGIN_FLOOD = '<';

	public SBILogin(Runnable sbiBlock) {
		this.sbiBlock = sbiBlock;
	}

	public void run() {
		if (!busy) {
			busy = true;

			try {
				this.runCheck();
			} finally {
				busy = false;
			}

		}
	}

	private void runCheck() {
		loginValid = false;
		String name = COConfigurationManager.getStringParameter("SBILoginName").replaceAll(" ", "");
		byte[] password = COConfigurationManager.getByteParameter("SBILoginPassword");
		if (name.length() != 0 && password.length != 0) {
			try {
				char result = this.performLogin(name, md5ToString(password));
				this.checkResult(result);
			} catch (Exception var4) {
				loginResult = "Cannot connect to Server.";
			}

			this.updateShuSection();
		} else {
			loginResult = "";
		}
	}

	private char performLogin(String name, String password) throws Exception {
		String protocol = null;
		if (COConfigurationManager.getBooleanParameter("SBILoginSSL")) {
			protocol = "https://";
		} else {
			protocol = "http://";
		}

		HttpURLConnection connection = (HttpURLConnection)(new URL(protocol + "www.sb-innovation.de/login.php")).openConnection();
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(5000);
		if (connection instanceof HttpsURLConnection) {
			this.trustCustomCertificates((HttpsURLConnection)connection);
		}

		connection.setInstanceFollowRedirects(false);
		connection.setUseCaches(false);
		connection.setDoOutput(true);
		connection.setRequestProperty("User-Agent", "Vuze Plus Extreme Mod");
		connection.setRequestProperty("Connection", "close");
		String message = "do=vuzeCheck&vb_login_username=" + name + "&vb_login_md5password=" + password;
		OutputStreamWriter writer = null;
		BufferedReader reader = null;

		char result;
		try {
			writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(message);
			writer.flush();
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			result = (char)reader.read();
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}

				if (reader != null) {
					reader.close();
				}
			} catch (IOException var15) {
			}

		}

		return result;
	}

	private void trustCustomCertificates(HttpsURLConnection connection) throws Exception {
		TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		}};
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init((KeyManager[])null, trustAllCerts, new SecureRandom());
		connection.setSSLSocketFactory(sc.getSocketFactory());
		HostnameVerifier hostname = new HostnameVerifier() {
			public boolean verify(String urlHostname, SSLSession session) {
				return true;
			}
		};
		connection.setHostnameVerifier(hostname);
	}

	private void checkResult(char result) {
		switch (result) {
			case '0':
				loginResult = "Username and/or password is incorrect.";
				break;
			case '1':
				loginResult = "Login Successful.";
				loginValid = true;
				break;
			case '2':
				loginResult = "You have been banned.";
				break;
			case '3':
				loginResult = "Your account has not been activated.";
				break;
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case ':':
			case ';':
			default:
				loginResult = "Unknown response. Please report this to SBI.";
				break;
			case '<':
				loginResult = "You have used up your login quota! Please wait 15min. before trying again.";
		}

	}

	private void updateShuSection() {
		if (this.sbiBlock != null) {
			Display display = SWTThread.getInstance().getDisplay();
			if (display != null && !display.isDisposed()) {
				display.asyncExec(this.sbiBlock);
			}
		}

	}

	public static String md5ToString(byte[] md5) {
		String result = "";

		for(int i = 0; i < md5.length; ++i) {
			String hex = Integer.toHexString(md5[i] & 255);
			if (hex.length() == 1) {
				result = result + "0";
			}

			result = result + hex;
		}

		return result;
	}
}
