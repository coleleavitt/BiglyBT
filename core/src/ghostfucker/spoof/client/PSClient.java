package ghostfucker.spoof.client;

import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.core.util.Constants;
import ghostfucker.spoof.ut.ID;
import ghostfucker.utils.PropertyReader;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Core PerfectSpoof engine. Handles peer ID generation, key generation,
 * announce URL parameter injection, and HTTP header spoofing.
 *
 * Supports 4 peer ID modes: custom, ut (uTorrent), hex, anum (alphanumeric).
 * Supports 3 protocol types: BTLP (1), AZMP (2), LTEP (3).
 */
public class PSClient {

	private int protocol;
	private String azmpName;
	private String azmpVersion;
	private Object ltepName;
	private byte[] reservedBytes;
	private int idType;
	private String preFix;
	private int idLength;
	private String idCharSet;
	private Map<String, String> idExceptions;
	private boolean idToLowerCase;
	private String clientId;
	private byte[] peerId;
	private PropertyReader idFile;
	private int keyLength;
	private String globalKey;
	private String keyCharSet;
	private boolean announceInfoHashToLowerCase;
	private Map<String, String> announceInfoHashExceptions;
	private int staticNumwant;
	private String announce;
	private String conCat = "";
	private String ip;
	private int ipMode;
	private long fakeCorrupt = -1;
	private static final String[] OPTIONAL_ANNOUNCE_KEYS = new String[]{
			"supportcrypto", "requirecrypto", "cryptoport", "event"
	};
	private boolean isScrapeDisabled;
	private boolean scrapeInfoHashToLowerCase;
	private Map<String, String> scrapeInfoHashExceptions;
	private String message;

	// Announce/scrape header configuration stored here instead of PSurlConnection.Config
	// to avoid dependency on sun.net.www.protocol package
	private String[][] announceHeader;
	private int announceHostIndex;
	private String announceHttpVersion;
	private boolean announceShowDefaultPort;
	private String[][] scrapeHeader;
	private int scrapeHostIndex;
	private String scrapeHttpVersion;
	private boolean scrapeShowDefaultPort;
	
	// Spoofed User-Agent extracted from header
	private String spoofedUserAgent;

	public void init(Client c, String message) {
		// Protocol setup
		if (c.protocol.type.equalsIgnoreCase("AZMP")) {
			this.protocol = 2;
			this.azmpName = c.protocol.clientName.azmp;
			this.azmpVersion = c.protocol.clientVersion.azmp;
			this.ltepName = c.protocol.clientName.isEncodingDisabled
					? c.protocol.clientName.ltep.getBytes()
					: c.protocol.clientName.ltep;
		} else if (c.protocol.type.equalsIgnoreCase("LTEP")) {
			this.protocol = 3;
			this.ltepName = c.protocol.clientName.isEncodingDisabled
					? c.protocol.clientName.ltep.getBytes()
					: c.protocol.clientName.ltep;
		} else {
			this.protocol = 1;
		}

		this.reservedBytes = decodeReservedBytes(c.protocol.reservedBytes);

		// Check if custom ID is forced via config
		if (COConfigurationManager.getBooleanParameter("PSisCid")) {
			c.peerId.type = "custom";
		}

		this.preFix = c.peerId.preFix;

		// Peer ID generation based on type
		if (c.peerId.type.equalsIgnoreCase("custom")) {
			if (c.peerId.isGlobal) {
				if (COConfigurationManager.getBooleanParameter("PSisCidFromFile")) {
					int id = (int) (Math.random() * (double) c.peerId.propertyReader.getSize());
					this.clientId = this.preFix + c.peerId.propertyReader.getValue(id);
				} else {
					this.clientId = this.preFix + COConfigurationManager.getStringParameter("PScid");
				}
				this.peerId = clientIdToByteArray(this.clientId);
			} else {
				this.idType = 0;
				this.idFile = c.peerId.propertyReader;
			}
			message = message + " (CID)";
		} else if (c.peerId.type.equalsIgnoreCase("ut")) {
			Map<String, String> exceptions = parseExceptions(c.peerId.exceptions, c.peerId.isLowerCase);
			if (c.peerId.isGlobal) {
				this.peerId = ID.create(this.preFix);
				this.clientId = clientIdToString(this.peerId, exceptions, c.peerId.isLowerCase);
			} else {
				this.idType = 1;
				this.idExceptions = exceptions;
				this.idToLowerCase = c.peerId.isLowerCase;
			}
		} else if (c.peerId.type.equalsIgnoreCase("hex")) {
			Map<String, String> exceptions = parseExceptions(c.peerId.exceptions, c.peerId.isLowerCase);
			String charSet = getCharset(c.peerId.charSet, c.peerId.type);
			if (c.peerId.isGlobal) {
				this.clientId = generateHexPeerId(this.preFix, c.peerId.length, charSet, exceptions, c.peerId.isLowerCase);
				this.peerId = clientIdToByteArray(this.clientId);
			} else {
				this.idType = 2;
				this.idLength = c.peerId.length;
				this.idCharSet = charSet;
				this.idExceptions = exceptions;
				this.idToLowerCase = c.peerId.isLowerCase;
			}
		} else {
			// anum type
			String charSet = getCharset(c.peerId.charSet, c.peerId.type);
			if (c.peerId.isGlobal) {
				this.clientId = generateAlphaNumPeerId(this.preFix, c.peerId.length, charSet);
				this.peerId = clientIdToByteArray(this.clientId);
			} else {
				this.idType = 3;
				this.idLength = c.peerId.length;
				this.idCharSet = charSet;
			}
		}

		// Key generation
		String keyCharSet = getCharset(c.key.charSet, c.key.type);
		if (c.key.isGlobal) {
			this.globalKey = generateClientKey(keyCharSet, c.key.length);
		} else {
			this.keyLength = c.key.length;
			this.keyCharSet = keyCharSet;
		}

		// Announce setup
		this.announceInfoHashToLowerCase = c.announce.infoHash.isLowerCase;
		this.announceInfoHashExceptions = parseExceptions(c.announce.infoHash.exceptions, c.announce.infoHash.isLowerCase);
		this.staticNumwant = c.announce.query.staticNumwant;
		this.fakeCorrupt = c.announce.query.fakeCorrupt;

		StringBuilder sb = new StringBuilder();
		for (String s : c.announce.query.param) {
			sb.append(s);
		}

		this.announce = sb.toString();
		this.announce = prepareAnnounceString(this.announce);
		String firstChar = "" + this.announce.charAt(0);
		if (firstChar.equals("?") || firstChar.equals("&")) {
			this.announce = this.announce.substring(1);
			this.conCat = firstChar;
		}

		// IP handling: ipType -1=hide, 0=IPv4, 1=IPv6, 2=Tunnel IPv6
		// ipMode 0=all announces, 1=except stopped event
		int ipIndex = this.announce.indexOf("{ip}");
		if (ipIndex != -1) {
			int ipType = c.announce.query.ipType;
			if (ipType >= 0 && ipType <= 2) {
				String[] ips = getIps();
				this.ip = ips[ipType];
				if (this.ip == null) {
					// Try alternate IPv6 type: 1->2 or 2->1
					if (ipType > 0) {
						int altType = (ipType == 1) ? 2 : 1;
						this.ip = ips[altType];
					}

					// Fallback: try lower IP types until one is found
					if (c.announce.query.ipFallBack == 1) {
						int fallbackType = ipType;
						while (--fallbackType >= 0 && this.ip == null) {
							this.ip = ips[fallbackType];
						}
					}
				}

				if (this.ip != null) {
					// Remove leading slash from InetAddress.toString()
					this.ip = replaceAll(this.ip, "/", "");
					// Remove zone ID suffix (e.g. %eth0 on IPv6 link-local)
					int pIndex = this.ip.indexOf(37);
					if (pIndex != -1) {
						this.ip = this.ip.substring(0, pIndex);
					}

					try {
						this.ip = URLEncoder.encode(this.ip, "ISO-8859-1");
					} catch (UnsupportedEncodingException e) {
						this.ip = URLEncoder.encode(this.ip);
					}

					if (c.announce.infoHash.isLowerCase) {
						this.ip = hexToLowerCase(this.ip);
					}
				}
			}
			// else ipType == -1 or out of range: ip stays null (hidden)

			// Extract parameter key prefix before {ip} (e.g. &ip= or &ipv6=)
			int start = ipIndex;
			for (char tmp = this.announce.charAt(ipIndex); tmp != '&' && tmp != '?' && start > 0; tmp = this.announce.charAt(start)) {
				--start;
			}

			if (this.ip == null) {
				// Remove entire IP parameter from template when hiding
				this.announce = replaceAll(this.announce, this.announce.substring(start, ipIndex) + "{ip}", "");
			} else {
				// Move parameter prefix into this.ip for runtime substitution
				String prefix = this.announce.substring(start, ipIndex);
				this.ip = prefix + this.ip;
				this.announce = replaceAll(this.announce, this.announce.substring(start, ipIndex), "");
			}

			this.ipMode = c.announce.query.ipMode;
		}

		// Announce headers
		this.announceHeader = buildHeader(c.announce.header);
		this.announceHostIndex = findHostIndex(c.announce.header);
		this.announceHttpVersion = c.announce.protocol;
		this.announceShowDefaultPort = c.announce.header.showDefaultPort;

		// Scrape setup - handle null scrape section gracefully
		if (c.scrape == null) {
			this.isScrapeDisabled = true;
		} else {
			this.isScrapeDisabled = c.scrape.isDisabled;
			if (!this.isScrapeDisabled) {
				if (c.scrape.infoHash != null) {
					this.scrapeInfoHashToLowerCase = c.scrape.infoHash.isLowerCase;
					this.scrapeInfoHashExceptions = parseExceptions(c.scrape.infoHash.exceptions, c.scrape.infoHash.isLowerCase);
				}
				if (c.scrape.header != null) {
					this.scrapeHeader = buildHeader(c.scrape.header);
					this.scrapeHostIndex = findHostIndex(c.scrape.header);
					this.scrapeHttpVersion = c.scrape.protocol;
					this.scrapeShowDefaultPort = c.scrape.header.showDefaultPort;
				}
			}
		}

		this.message = message;
	}

	// --- Public getters ---

	public Object getLtepName() {
		return this.ltepName;
	}

	public String getAzmpName() {
		return this.azmpName;
	}

	public String getAzmpVersion() {
		return this.azmpVersion;
	}

	public byte[] getReservedBytes() {
		return this.reservedBytes;
	}

	public int getProtocol() {
		return this.protocol;
	}

	public int getProtocol(int current) {
		return current != 1 && this.protocol != 2 ? this.protocol : current;
	}

	public String getClientId() {
		return this.clientId;
	}

	public byte[] getPeerId() {
		if (this.peerId != null) {
			return this.peerId;
		}

		switch (this.idType) {
			case 0:
				int id = (int) (Math.random() * (double) this.idFile.getSize());
				this.clientId = this.preFix + this.idFile.getValue(id);
				break;
			case 1:
				byte[] newPeerId = ID.create(this.preFix);
				this.clientId = clientIdToString(newPeerId, this.idExceptions, this.idToLowerCase);
				return newPeerId;
			case 2:
				this.clientId = generateHexPeerId(this.preFix, this.idLength, this.idCharSet, this.idExceptions, this.idToLowerCase);
				break;
			default:
				this.clientId = generateAlphaNumPeerId(this.preFix, this.idLength, this.idCharSet);
		}

		return clientIdToByteArray(this.clientId);
	}

public String getClientKey() {
		return this.globalKey != null ? this.globalKey : generateClientKey(this.keyCharSet, this.keyLength);
	}

	public String getKey() {
		return getClientKey();
	}

	public String getUserAgent() {
		// Return the spoofed User-Agent if available, otherwise fall back to BiglyBT default
		if (this.spoofedUserAgent != null && !this.spoofedUserAgent.isEmpty()) {
			return this.spoofedUserAgent;
		}
		// Fallback for clients without explicit User-Agent in header
		String ua = "BiglyBT/" + Constants.BIGLYBT_VERSION;
		if (this.azmpName != null) {
			ua += " (" + this.azmpName + "/" + this.azmpVersion + ")";
		}
		return ua;
	}
	
	public String getSpoofedUserAgent() {
		return getUserAgent();
	}

	public String getInfoHash(String oldInfoHash, byte type) {
		oldInfoHash = normalizeInfoHash(oldInfoHash);
		switch (type) {
			case 0:
				if (this.announceInfoHashToLowerCase) {
					oldInfoHash = hexToLowerCase(oldInfoHash);
				}
				oldInfoHash = replaceAll(oldInfoHash, this.announceInfoHashExceptions);
				break;
			case 1:
				if (this.scrapeInfoHashToLowerCase) {
					oldInfoHash = hexToLowerCase(oldInfoHash);
				}
				oldInfoHash = this.scrapeInfoHashExceptions == null
						? replaceAll(oldInfoHash, this.announceInfoHashExceptions)
						: replaceAll(oldInfoHash, this.scrapeInfoHashExceptions);
		}

		return oldInfoHash;
	}

	public boolean isStaticNumwant() {
		return this.staticNumwant != 0;
	}

	public int getStaticNumwant() {
		return this.staticNumwant;
	}

	public String getAnnounce(String orgAnnounce, int paramIndex) {
		String announceBase = orgAnnounce.substring(0, paramIndex);
		Map<String, String> param = announceStringToHashMap(
				orgAnnounce.substring(paramIndex), this.announce, this.ip, this.ipMode);
		// Spoof corrupt value if configured
		if (this.fakeCorrupt >= 0) {
			param.put("{corrupt}", String.valueOf(this.fakeCorrupt));
		}
		char ch = orgAnnounce.charAt(paramIndex - 1);
		String concat = ch != '?' && ch != '&' ? this.conCat : "";
		return announceBase + concat + replaceAll(this.announce, param);
	}

	public boolean isScrapeDisabled() {
		return this.isScrapeDisabled;
	}

	public String getMessage() {
		return this.message;
	}

	// --- Announce/Scrape header accessors ---

	public String[][] getAnnounceHeader() {
		return this.announceHeader;
	}

	public int getAnnounceHostIndex() {
		return this.announceHostIndex;
	}

	public String getAnnounceHttpVersion() {
		return this.announceHttpVersion;
	}

	public boolean getAnnounceShowDefaultPort() {
		return this.announceShowDefaultPort;
	}

	public String[][] getScrapeHeader() {
		return this.scrapeHeader;
	}

	public int getScrapeHostIndex() {
		return this.scrapeHostIndex;
	}

	public String getScrapeHttpVersion() {
		return this.scrapeHttpVersion;
	}

	public boolean getScrapeShowDefaultPort() {
		return this.scrapeShowDefaultPort;
	}

	// --- Static utility methods ---

	private static byte[] decodeReservedBytes(String reservedBytes) {
		byte[] result = new byte[8];
		String[] hex = reservedBytes.split("\\|");

		for (int i = 0; i < hex.length; ++i) {
			result[i] = Integer.decode(hex[i]).byteValue();
		}

		return result;
	}

	private static byte[] clientIdToByteArray(String peerId) {
		byte[] id = new byte[getByteLength(peerId)];
		int i = 0;

		for (int k = 0; i < peerId.length(); ++k) {
			char ch = peerId.charAt(i);
			if (ch == '%') {
				ch = (char) Integer.parseInt(peerId.substring(i + 1, i + 3), 16);
				i += 2;
			}

			id[k] = (byte) ch;
			++i;
		}

		return id;
	}

	private static boolean isDefaultChar(char ch) {
		return ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9';
	}

	private static String replaceAll(String str, String regex, String replacement) {
		int index = str.indexOf(regex, 0);
		if (index == -1) {
			return str;
		}

		int currentIndex = 0;
		StringBuilder sb = new StringBuilder(str.length());
		while (index != -1) {
			sb.append(str, currentIndex, index);
			sb.append(replacement);
			currentIndex = index + regex.length();
			index = str.indexOf(regex, currentIndex);
		}

		sb.append(str.substring(currentIndex));
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private static String replaceAll(String str, Map replacement) {
		if (replacement == null) {
			return str;
		}

		for (Object key : replacement.keySet()) {
			str = replaceAll(str, (String) key, (String) replacement.get(key));
		}

		return str;
	}

	private static String hexToLowerCase(String s) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < s.length(); ++i) {
			String tmp = "" + s.charAt(i);
			if (tmp.equals("%")) {
				tmp = s.substring(i, i + 3).toLowerCase();
				i += 2;
			}

			sb.append(tmp);
		}

		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private static String clientIdToString(byte[] peerId, Map exceptions, boolean isLowerCase) {
		StringBuilder sb = new StringBuilder();

		for (byte b : peerId) {
			char ch = (char) (b & 255);
			if (isDefaultChar(ch)) {
				sb.append(ch);
			} else {
				sb.append('%');
				String hex = Integer.toHexString(ch);
				if (hex.length() == 1) {
					sb.append("0");
				}
				sb.append(hex);
			}
		}

		String result = sb.toString();
		if (isLowerCase) {
			result = hexToLowerCase(result);
		}

		result = replaceAll(result, exceptions);
		return result;
	}

	private static Map<String, String> parseExceptions(String exceptions, boolean isLowerCase) {
		if (exceptions != null && exceptions.length() != 0) {
			Map<String, String> map = new HashMap<>(exceptions.length());

			for (int i = 0; i < exceptions.length(); ++i) {
				char ch = exceptions.charAt(i);
				String hex = Integer.toHexString(ch);
				String normalized = hex.length() == 1 ? "0" + hex : hex;
				String key = "%" + normalized;
				map.put(isLowerCase ? key : key.toUpperCase(), "" + ch);
			}

			return map;
		}

		return null;
	}

	private static String getCharset(String charSet, String type) {
		if (charSet != null) {
			return charSet;
		}

		return type.equalsIgnoreCase("hex")
				? "0123456789ABCDEF"
				: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	}

	private static String generateHexPeerId(String preFix, int length, String charSet,
											Map<String, String> exceptions, boolean isLowerCase) {
		StringBuilder sb = new StringBuilder(preFix);

		for (int i = 0; i < length; ++i) {
			int pos1 = (int) (Math.random() * (double) charSet.length());
			int pos2 = (int) (Math.random() * (double) charSet.length());
			String hex = "" + charSet.charAt(pos1) + charSet.charAt(pos2);
			char hexVal = (char) Integer.parseInt(hex, 16);
			if (isDefaultChar(hexVal)) {
				sb.append(hexVal);
			} else {
				sb.append('%').append(hex);
			}
		}

		String result = sb.toString();
		if (isLowerCase) {
			result = hexToLowerCase(result);
		}

		result = replaceAll(result, exceptions);
		return result;
	}

	private static String generateAlphaNumPeerId(String preFix, int length, String charSet) {
		StringBuilder sb = new StringBuilder(preFix);

		for (int i = 0; i < length; ++i) {
			int pos = (int) (Math.random() * (double) charSet.length());
			sb.append(charSet.charAt(pos));
		}

		return sb.toString();
	}

	private static String generateClientKey(String charSet, int length) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < length; ++i) {
			int pos = (int) (Math.random() * (double) charSet.length());
			sb.append(charSet.charAt(pos));
		}

		return sb.toString();
	}

	private static String normalizeInfoHash(String infoHash) {
		infoHash = replaceAll(infoHash, "*", "%2A");
		infoHash = replaceAll(infoHash, "-", "%2D");
		infoHash = replaceAll(infoHash, ".", "%2E");
		infoHash = replaceAll(infoHash, "_", "%5F");
		return infoHash;
	}

	private static String prepareAnnounceString(String announce) {
		announce = replaceAll(announce, "[info_hash]", "&info_hash={info_hash}");
		announce = replaceAll(announce, "[peer_id]", "&peer_id={peer_id}");
		announce = replaceAll(announce, "[port]", "&port={port}");
		announce = replaceAll(announce, "[azudp]", "&azudp={azudp}");
		announce = replaceAll(announce, "[uploaded]", "&uploaded={uploaded}");
		announce = replaceAll(announce, "[downloaded]", "&downloaded={downloaded}");
		announce = replaceAll(announce, "[left]", "&left={left}");
		announce = replaceAll(announce, "[corrupt]", "&corrupt={corrupt}");
		announce = replaceAll(announce, "[numwant]", "&numwant={numwant}");
		announce = replaceAll(announce, "[no_peer_id]", "&no_peer_id={no_peer_id}");
		announce = replaceAll(announce, "[compact]", "&compact={compact}");
		announce = replaceAll(announce, "[key]", "&key={key}");
		announce = replaceAll(announce, "[azver]", "&azver={azver}");
		announce = replaceAll(announce, "[ip]", "&ip={ip}");
		return announce;
	}

	private static Map<String, String> announceStringToHashMap(String announce, String announceTemplate,
															   String ip, int ipMode) {
		Map<String, String> announceParameter = new HashMap<>();
		int index = 0;

		for (int endIndex = announce.indexOf("=", index); endIndex != -1; endIndex = announce.indexOf("=", index)) {
			String param = announce.substring(index, endIndex);
			index = endIndex + 1;
			endIndex = announce.indexOf("&", index);
			if (endIndex == -1) {
				endIndex = announce.length();
			}

			String val = announce.substring(index, endIndex);
			announceParameter.put("{" + param + "}", val);
			index = endIndex + 1;
		}

		if (announceParameter.containsKey("{cryptoport}") && !announceTemplate.contains("cryptoport")) {
			String port = announceParameter.get("{cryptoport}");
			announceParameter.put("{port}", port);
		}

		// IP handling at announce time:
		// - ip null: always hide (no IP parameter sent)
		// - ipMode 1: hide on stopped events only
		// - ipMode 0 or other: include IP on all announces
		boolean isStopped = announceParameter.containsKey("{event}")
				&& announceParameter.get("{event}").contains("stopped");
		if (ip == null || (isStopped && ipMode == 1)) {
			announceParameter.put("{ip}", "");
		} else {
			announceParameter.put("{ip}", ip);
		}

		fillAnnounceMapWithDefaults(announceParameter);
		return announceParameter;
	}

	private static void fillAnnounceMapWithDefaults(Map<String, String> map) {
		for (String key : OPTIONAL_ANNOUNCE_KEYS) {
			String mapKey = String.format("{%s}", key);
			String val = map.get(mapKey);
			if (val != null) {
				map.put(mapKey, String.format("&%s=%s", key, val));
			} else {
				map.put(mapKey, "");
			}
		}
	}

	private static String[] getIps() {
		String[] ips = new String[3];

		try {
			NetworkInterface ni = getActiveNetworkInterface();
			Enumeration<InetAddress> eia = ni.getInetAddresses();

			while (eia.hasMoreElements()) {
				InetAddress ia = eia.nextElement();
				if (ia instanceof Inet4Address) {
					ips[0] = ia.toString();
				} else if (ia instanceof Inet6Address) {
					ips[1] = ia.toString();
				}
			}
		} catch (Exception ignored) {
		}

		try {
			Enumeration<NetworkInterface> eni = NetworkInterface.getNetworkInterfaces();

			while (eni.hasMoreElements()) {
				NetworkInterface ni = eni.nextElement();
				Enumeration<InetAddress> eia = ni.getInetAddresses();

				while (eia.hasMoreElements()) {
					InetAddress ia = eia.nextElement();
					if (ia instanceof Inet6Address) {
						String s = ia.toString();
						if (s.startsWith("/2001:") || s.startsWith("/2002:")
								|| s.startsWith("2001:") || s.startsWith("2002:")) {
							ips[2] = s;
							break;
						}
					}
				}

				if (ips[2] != null) {
					break;
				}
			}
		} catch (Exception ignored) {
		}

		return ips;
	}

	private static NetworkInterface getActiveNetworkInterface() throws Exception {
		String[] hosts = new String[]{"www.biglybt.com", "www.google.com", "www.yahoo.com"};

		for (String host : hosts) {
			try {
				Socket s = new Socket(host, 80);
				NetworkInterface ni = NetworkInterface.getByInetAddress(s.getLocalAddress());
				s.close();
				return ni;
			} catch (Exception ignored) {
			}
		}

		throw new Exception("Can't find active network interface");
	}

	private String[][] buildHeader(Client.Header headerConfig) {
		String[][] header = new String[headerConfig.field.size()][2];
		int i = 0;

		for (String s : headerConfig.field) {
			if (s.contains("{osName}")) {
				s = replaceAll(s, "{osName}", Constants.OSName);
			}

			if (s.contains("{javaVersion}")) {
				s = replaceAll(s, "{javaVersion}", Constants.JAVA_VERSION);
			}

			// Extract User-Agent value from header field
			if (s.toUpperCase().startsWith("USER-AGENT:")) {
				String uaValue = s.substring(11).trim(); // Skip "User-Agent:"
				if (this.spoofedUserAgent == null && !uaValue.isEmpty()) {
					this.spoofedUserAgent = uaValue;
					System.out.println("[PSClient] Extracted User-Agent from header: " + uaValue);
				}
			}

			String[] tmp = s.split(":", 2);
			header[i][0] = tmp[0];
			header[i++][1] = tmp[1];
		}

		return header;
	}

	private int findHostIndex(Client.Header headerConfig) {
		int i = 0;
		for (String s : headerConfig.field) {
			if (s.toUpperCase().contains("HOST")) {
				return i;
			}
			i++;
		}
		return 0;
	}

	public static int getByteLength(String s) {
		int length = 0;

		for (int i = 0; i < s.length(); ++i) {
			if (s.charAt(i) == '%') {
				i += 2;
			}

			++length;
		}

		return length;
	}

	public static boolean isValidCid(String cid, int length) {
		if (cid != null && length != 0) {
			int chars = cid.length();

			for (int i = 0; i < cid.length(); ++i) {
				if (cid.charAt(i) == '%') {
					if (cid.substring(i).length() < 3) {
						return false;
					}

					chars -= 2;
				}
			}

			return chars == length;
		}

		return false;
	}
}
