package ghostfucker.spoof;

import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.core.logging.LogAlert;
import com.biglybt.core.logging.Logger;
import ghostfucker.spoof.client.Client;
import ghostfucker.spoof.client.ClientXmlParser;
import ghostfucker.spoof.client.PSClient;
import ghostfucker.spoof.client.Validator;
import shu.utils.ShuUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * PerfectSpoof orchestrator. Manages the spoofing lifecycle:
 * - Reads config from COConfigurationManager ("PSisActive", "PSclientName", "PSclientVersion")
 * - Loads client profiles from clientfiles/[client]/[version].client
 * - Creates and validates PSClient instances
 * - Provides HTTP header spoofing for the SPI-based HTTP interception layer
 */
public class PerfectSpoof {

	public static final byte TYPE_ANNOUNCE = 0;
	public static final byte TYPE_SCRAPE = 1;

	public static boolean isAvailable;
	public static boolean isActive;
	private static PSClient psClient;
	private static PerfectSpoof instance;

	// HTTP header configurations per request type
	private final Map<Byte, String[][]> httpHeaders = new HashMap<>();
	private final Map<Byte, Integer> hostIndices = new HashMap<>();
	private boolean displayDefaultPort = false;

	private PerfectSpoof() {
		// Private constructor for singleton
	}

	/**
	 * Get the singleton instance.
	 */
	public static PerfectSpoof getInstance() {
		return instance;
	}

	private static boolean createClient() {
		String clientName = COConfigurationManager.getStringParameter("PSclientName");
		String clientFile = clientName + "/" + COConfigurationManager.getStringParameter("PSclientVersion");
		File path = new File("clientfiles/" + clientFile + ".client");
		if (!path.exists()) {
			Logger.log(new LogAlert(false, LogAlert.AT_ERROR, "Spoof deactivated: Cannot find " + path));
			return false;
		}

		try {
			Client client = ClientXmlParser.parseClient(path);
			(new Validator(client)).run();
			psClient = new PSClient();
			psClient.init(client, clientFile);

			// Initialize the singleton instance
			if (instance == null) {
				instance = new PerfectSpoof();
			}

			// Configure HTTP headers from the loaded client profile
			instance.configureHttpHeaders(client, psClient);

			return true;
		} catch (Validator.ValidationException ve) {
			Logger.log(new LogAlert(false, LogAlert.AT_ERROR,
					"Spoof deactivated: Invalid client file: " + clientFile, ve));
			return false;
		} catch (Exception e) {
			Logger.log(new LogAlert(false, LogAlert.AT_ERROR,
					"Spoof deactivated: Something gone wrong, please report this.", e));
			return false;
		}
	}

	/**
	 * Configure HTTP headers from the client profile.
	 */
	private void configureHttpHeaders(Client client, PSClient psClient) {
		// Clear existing headers
		httpHeaders.clear();
		hostIndices.clear();

		try {
			// Get announce headers
			if (client.announce != null && client.announce.header != null && client.announce.header.field != null) {
				String[][] announceHeaders = new String[client.announce.header.field.size()][2];
				int hostIndex = 0;
				int i = 0;
				for (String s : client.announce.header.field) {
					if (s.toUpperCase().contains("HOST")) {
						hostIndex = i;
					}
					String[] parts = s.split(":", 2);
					if (parts.length == 2) {
						announceHeaders[i][0] = parts[0].trim();
						announceHeaders[i][1] = parts[1].trim();
					}
					i++;
				}
				httpHeaders.put(TYPE_ANNOUNCE, announceHeaders);
				hostIndices.put(TYPE_ANNOUNCE, hostIndex);
				displayDefaultPort = client.announce.header.showDefaultPort;
			}

			// Get scrape headers (often same as announce)
			if (client.scrape != null && client.scrape.header != null && client.scrape.header.field != null) {
				String[][] scrapeHeaders = new String[client.scrape.header.field.size()][2];
				int hostIndex = 0;
				int i = 0;
				for (String s : client.scrape.header.field) {
					if (s.toUpperCase().contains("HOST")) {
						hostIndex = i;
					}
					String[] parts = s.split(":", 2);
					if (parts.length == 2) {
						scrapeHeaders[i][0] = parts[0].trim();
						scrapeHeaders[i][1] = parts[1].trim();
					}
					i++;
				}
				httpHeaders.put(TYPE_SCRAPE, scrapeHeaders);
				hostIndices.put(TYPE_SCRAPE, hostIndex);
			} else {
				// Fall back to announce headers for scrape
				httpHeaders.put(TYPE_SCRAPE, httpHeaders.get(TYPE_ANNOUNCE));
				hostIndices.put(TYPE_SCRAPE, hostIndices.getOrDefault(TYPE_ANNOUNCE, 0));
			}
		} catch (Exception e) {
			Logger.log(new LogAlert(false, LogAlert.AT_WARNING,
					"Failed to configure HTTP headers from client profile", e));
		}
	}

	/**
	 * Get HTTP headers for the specified request type.
	 * @param requestType TYPE_ANNOUNCE (0) or TYPE_SCRAPE (1)
	 * @return Array of [name, value] pairs, or null if not configured
	 */
	public String[][] getHttpHeaders(byte requestType) {
		return httpHeaders.get(requestType);
	}

	/**
	 * Get the host header index for the specified request type.
	 */
	public int getHostIndex(byte requestType) {
		return hostIndices.getOrDefault(requestType, 0);
	}

	/**
	 * Whether to display default port in Host header.
	 */
	public boolean shouldDisplayDefaultPort() {
		return displayDefaultPort;
	}

	/**
	 * Check if spoofing is enabled.
	 */
	public boolean isEnabled() {
		return isActive;
	}

	public static PSClient getClient() {
		return psClient;
	}

	public static boolean isActive() {
		return isActive;
	}

	public static void setActive(boolean active) {
		if (active && !isActive) {
			isActive = createClient();
			if (!isActive) {
				COConfigurationManager.setParameter("PSisActive", false);
				psClient = null;
				instance = null;
			}
		} else if (!active) {
			isActive = false;
			psClient = null;
			instance = null;
		}
	}

	public static void initialize() {
		isActive = COConfigurationManager.getBooleanParameter("spoof.enabled", false);
		if (isActive) {
			// Load client profile
			String clientName = COConfigurationManager.getStringParameter("spoof.client", "");
			if (!clientName.isEmpty()) {
				isActive = createClient();
				if (!isActive) {
					COConfigurationManager.setParameter("spoof.enabled", false);
					psClient = null;
					instance = null;
				}
			}
		}
	}

	static {
		isAvailable = !ShuUtils.spoof;
		isActive = false;
		if (isAvailable && COConfigurationManager.getBooleanParameter("PSisActive")) {
			isActive = createClient();
			if (!isActive) {
				COConfigurationManager.setParameter("PSisActive", false);
				psClient = null;
				instance = null;
			}
		}
	}
}
