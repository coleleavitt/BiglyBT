package ghostfucker.spoof;

import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.core.logging.LogAlert;
import com.biglybt.core.logging.Logger;
import ghostfucker.spoof.client.Client;
import ghostfucker.spoof.client.ClientXmlParser;
import ghostfucker.spoof.client.PSClient;
import ghostfucker.spoof.client.Validator;
import shu.utils.ShuUtils;
import com.biglybt.core.peermanager.messaging.bittorrent.BTHandshake;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * PerfectSpoof orchestrator. Manages the spoofing lifecycle:
 * - Reads config from COConfigurationManager ("PSisActive", "PSclientName", "PSclientVersion")
 * - Loads client profiles from clientfiles/[client]/[version].client
 * - Creates and validates PSClient instances
 * - Provides HTTP header spoofing for the SPI-based HTTP interception layer
 *
 * Thread-safety: All shared mutable state is volatile. State mutations are
 * synchronized on {@code LOCK}. An initialization barrier ({@code initLatch})
 * ensures client profiles are fully loaded before any tracker announce can
 * observe stale state.
 */
public class PerfectSpoof {

	public static final byte TYPE_ANNOUNCE = 0;
	public static final byte TYPE_SCRAPE = 1;

	/** Lock for all state mutations (setActive, initialize, createClient). */
	private static final Object LOCK = new Object();

	/**
	 * Initialization barrier. Released once the static initializer (and any
	 * fallback {@link #initialize()} path) has completed. Callers of
	 * {@link #awaitInitialization(long, TimeUnit)} can block until spoofing
	 * state is settled, guaranteeing the first tracker announce sees the
	 * correct isActive/psClient values.
	 */
	private static final CountDownLatch initLatch = new CountDownLatch(1);


	public static volatile boolean isAvailable;
	public static volatile boolean isActive;
	private static volatile PSClient psClient;
	private static volatile PerfectSpoof instance;

	// HTTP header configurations per request type
	private final Map<Byte, String[][]> httpHeaders = new HashMap<>();
	private final Map<Byte, Integer> hostIndices = new HashMap<>();
	private boolean displayDefaultPort = false;

	// Static initializer: allow setting restricted HTTP headers (Host, etc.)
	// Must be set before any HttpURLConnection is created
	static {
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
		System.out.println("[PerfectSpoof] Set sun.net.http.allowRestrictedHeaders=true");
	}
	private PerfectSpoof() {
		// Private constructor for singleton
	}

	/**
	 * Get the singleton instance.
	 */
	public static PerfectSpoof getInstance() {
		return instance;
	}

	/**
	 * Block until the initialization sequence has completed (static initializer
	 * + any fallback {@link #initialize()} path). This guarantees that the
	 * first tracker announce will see the fully-resolved isActive / psClient
	 * state rather than a stale default.
	 *
	 * @return {@code true} if initialization completed within the timeout
	 */
	public static boolean awaitInitialization(long timeout, TimeUnit unit) {
		try {
			return initLatch.await(timeout, unit);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/**
	 * Non-blocking check: has initialization completed?
	 */
	public static boolean isInitialized() {
		return initLatch.getCount() == 0;
	}

	/**
	 * Create and validate the PSClient from config. Caller MUST hold {@code LOCK}.
	 * <p>
	 * On success, sets {@code psClient} and {@code instance} <b>before</b>
	 * returning {@code true}, so that any concurrent reader that sees
	 * {@code isActive == true} is guaranteed to also see a non-null client.
	 */
	private static boolean createClient() {
		String clientName = COConfigurationManager.getStringParameter("PSclientName");
		String clientVersion = COConfigurationManager.getStringParameter("PSclientVersion");
		String clientFile = clientName + "/" + clientVersion;
		
		System.out.println("[PerfectSpoof] createClient() called for: " + clientFile);
		

		File clientFilesDir = findClientFilesDir();
		if (clientFilesDir == null) {
			System.out.println("[PerfectSpoof] ERROR: Could not find clientfiles directory");
			Logger.log(new LogAlert(false, LogAlert.AT_ERROR, "Spoof deactivated: Cannot find clientfiles directory"));
			return false;
		}
		
		File path = new File(clientFilesDir, clientFile + ".client");
		System.out.println("[PerfectSpoof] Looking for client file: " + path.getAbsolutePath());
		
		if (!path.exists()) {
			System.out.println("[PerfectSpoof] ERROR: Client file not found: " + path.getAbsolutePath());
			Logger.log(new LogAlert(false, LogAlert.AT_ERROR, "Spoof deactivated: Cannot find " + path));
			return false;
		}

		try {
			System.out.println("[PerfectSpoof] Parsing client file...");
			Client client = ClientXmlParser.parseClient(path);
			System.out.println("[PerfectSpoof] Client parsed successfully");
			
			(new Validator(client)).run();
			System.out.println("[PerfectSpoof] Client validated successfully");


			PSClient newClient = new PSClient();
			newClient.init(client, clientFile);
			System.out.println("[PerfectSpoof] PSClient initialized:");
			System.out.println("[PerfectSpoof]   - Peer ID: " + new String(newClient.getPeerId()));
			System.out.println("[PerfectSpoof]   - User Agent: " + newClient.getUserAgent());

			// Always create a fresh instance to avoid race condition
			// (concurrent readers may be accessing the old instance's maps)
			PerfectSpoof newInstance = new PerfectSpoof();

			// Configure HTTP headers from the loaded client profile
			newInstance.configureHttpHeaders(client, newClient);
			System.out.println("[PerfectSpoof] HTTP headers configured");


			psClient = newClient;
			instance = newInstance;


			return true;
		} catch (Validator.ValidationException ve) {
			System.out.println("[PerfectSpoof] ERROR: Validation failed: " + ve.getMessage());
			Logger.log(new LogAlert(false, LogAlert.AT_ERROR,
					"Spoof deactivated: Invalid client file: " + clientFile, ve));
			return false;
		} catch (Exception e) {
			System.out.println("[PerfectSpoof] ERROR: Exception during client creation: " + e.getMessage());
			e.printStackTrace();
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
		System.out.println("[PerfectSpoof] isActive() called, returning: " + isActive);
		return isActive;
	}

	/**
	 * Activate or deactivate spoofing. Thread-safe: serialized via {@code LOCK}.
	 * <p>
	 * On activation: loads client profile, sets psClient/instance, THEN sets
	 * isActive=true (volatile write provides happens-before for readers).
	 * <p>
	 * On deactivation: sets isActive=false FIRST, then nulls psClient/instance.
	 */
	public static void setActive(boolean active) {
		synchronized (LOCK) {
			System.out.println("[PerfectSpoof] setActive(" + active + ") called, current isActive=" + isActive);
			if (active && !isActive) {
				System.out.println("[PerfectSpoof] Activating spoof...");
				boolean created = createClient();
				System.out.println("[PerfectSpoof] createClient() returned: " + created);
				if (!created) {
					System.out.println("[PerfectSpoof] Client creation failed during setActive");

					isActive = false;
					psClient = null;
					instance = null;
				} else {

					isActive = true;

					COConfigurationManager.setParameter("PSisActive", true);
				}
			} else if (!active) {
				System.out.println("[PerfectSpoof] Deactivating spoof");

				isActive = false;
				psClient = null;
				instance = null;
				// Reset BTHandshake reserved bytes to defaults
				try {
					BTHandshake.updateReservedBytes();
					System.out.println("[PerfectSpoof] BTHandshake reserved bytes reset to defaults");
				} catch (Exception e) {
					System.out.println("[PerfectSpoof] Warning: Could not reset BTHandshake reserved bytes");
				}
			} else {
				System.out.println("[PerfectSpoof] setActive(true) called but already active, skipping");
			}
		}
	}

	/**
	 * Called by CoreImpl during startup. This method exists for legacy compatibility.
	 * The static initializer already handles activation based on client file existence,
	 * so this method only acts if spoofing wasn't already activated.
	 * <p>
	 * Thread-safe: serialized via {@code LOCK}. Releases {@code initLatch} on
	 * completion so any blocked tracker threads can proceed.
	 */
	public static void initialize() {
		synchronized (LOCK) {
			System.out.println("[PerfectSpoof] initialize() called - isActive=" + isActive + ", psClient=" + (psClient != null ? "exists" : "null"));


			if (isActive && psClient != null) {
				System.out.println("[PerfectSpoof] initialize(): Already active, skipping re-initialization");

				initLatch.countDown();
				return;
			}


			boolean configEnabled = COConfigurationManager.getBooleanParameter("PSisActive", false);
			System.out.println("[PerfectSpoof] initialize(): PSisActive config = " + configEnabled);

			if (configEnabled && !isActive) {
				System.out.println("[PerfectSpoof] initialize(): Config says enabled but not active, attempting activation");
				boolean created = createClient();
				if (!created) {
					System.out.println("[PerfectSpoof] initialize(): Client creation failed");

					isActive = false;
					psClient = null;
					instance = null;
				} else {

					isActive = true;
					System.out.println("[PerfectSpoof] initialize(): Successfully activated from config");
				}
			}


			initLatch.countDown();
		}
	}

	static {
		synchronized (LOCK) {
			isAvailable = !ShuUtils.spoof;
			isActive = false;
			System.out.println("[PerfectSpoof] ========== STATIC INITIALIZER START ==========");
			System.out.println("[PerfectSpoof] isAvailable=" + isAvailable + " (ShuUtils.spoof=" + ShuUtils.spoof + ")");
			System.out.println("[PerfectSpoof] Working directory: " + System.getProperty("user.dir"));
			System.out.println("[PerfectSpoof] User path: " + System.getProperty("user.home") + "/.biglybt");


			Object rawValue = COConfigurationManager.getParameter("PSisActive");
			System.out.println("[PerfectSpoof] PSisActive raw value: " + rawValue + " (type: " + (rawValue != null ? rawValue.getClass().getSimpleName() : "null") + ")");


			String rawClientName = COConfigurationManager.getStringParameter("PSclientName", "");
			String rawClientVersion = COConfigurationManager.getStringParameter("PSclientVersion", "");
			System.out.println("[PerfectSpoof] Config PSclientName='" + rawClientName + "'");
			System.out.println("[PerfectSpoof] Config PSclientVersion='" + rawClientVersion + "'");

			// Set defaults if not already configured
			if (rawClientName.isEmpty()) {
				rawClientName = "qBitTorrent";
				COConfigurationManager.setParameter("PSclientName", rawClientName);
				System.out.println("[PerfectSpoof] Set default client name: " + rawClientName);
			}
			if (rawClientVersion.isEmpty()) {
				rawClientVersion = "5.0.3";
				COConfigurationManager.setParameter("PSclientVersion", rawClientVersion);
				System.out.println("[PerfectSpoof] Set default client version: " + rawClientVersion);
			}

			// Check if clientfiles directory and client file exist
			File clientFilesDir = findClientFilesDir();
			System.out.println("[PerfectSpoof] clientfiles directory: " + (clientFilesDir != null ? clientFilesDir.getAbsolutePath() : "NOT FOUND"));

			boolean clientFileExists = false;
			if (clientFilesDir != null) {
				File clientFile = new File(clientFilesDir, rawClientName + "/" + rawClientVersion + ".client");
				clientFileExists = clientFile.exists();
				System.out.println("[PerfectSpoof] Client file: " + clientFile.getAbsolutePath());
				System.out.println("[PerfectSpoof] Client file exists: " + clientFileExists);
			}


			if (isAvailable && clientFileExists) {
				System.out.println("[PerfectSpoof] *** ATTEMPTING ACTIVATION (client file exists) ***");
				boolean created = createClient();
				System.out.println("[PerfectSpoof] createClient() returned: " + created);

				if (created) {

					isActive = true;
					System.out.println("[PerfectSpoof] *** SPOOF ACTIVATED SUCCESSFULLY ***");
					COConfigurationManager.setParameter("PSisActive", true);
					if (psClient != null) {
						System.out.println("[PerfectSpoof]   Peer ID: " + new String(psClient.getPeerId()));
						System.out.println("[PerfectSpoof]   User-Agent: " + psClient.getUserAgent());
					}

					try {
						BTHandshake.updateReservedBytes();
						System.out.println("[PerfectSpoof] BTHandshake reserved bytes updated to spoofed values");
					} catch (Exception e) {
						System.out.println("[PerfectSpoof] Warning: Could not update BTHandshake reserved bytes: " + e.getMessage());
					}
				} else {
					System.out.println("[PerfectSpoof] *** CLIENT CREATION FAILED ***");
					System.out.println("[PerfectSpoof] NOTE: NOT disabling PSisActive - user can retry");

					psClient = null;
					instance = null;
				}
			} else {
				System.out.println("[PerfectSpoof] *** NOT ACTIVATING ***");
				if (!isAvailable) {
					System.out.println("[PerfectSpoof]   Reason: isAvailable=false (ShuUtils.spoof=" + ShuUtils.spoof + ")");
				}
				if (!clientFileExists) {
					System.out.println("[PerfectSpoof]   Reason: Client file does not exist");
				}
			}
			System.out.println("[PerfectSpoof] Final state: isActive=" + isActive);
			System.out.println("[PerfectSpoof] ========== STATIC INITIALIZER END ==========");


			initLatch.countDown();
		}
	}

	/**
	 * Searches multiple locations for the clientfiles directory.
	 * Same logic as Configuration.findClientFilesDir()
	 */
	private static File findClientFilesDir() {
		// 1. Current working directory
		File cwd = new File("clientfiles");
		if (cwd.exists() && cwd.isDirectory()) {
			System.out.println("[PerfectSpoof] findClientFilesDir: found in CWD");
			return cwd;
		}
		
		// 2. User home: ~/.biglybt/clientfiles
		File userHome = new File(System.getProperty("user.home"), ".biglybt" + File.separator + "clientfiles");
		if (userHome.exists() && userHome.isDirectory()) {
			System.out.println("[PerfectSpoof] findClientFilesDir: found in user home");
			return userHome;
		}
		
		// 3. Next to the JAR file
		try {
			URI jarUri = PerfectSpoof.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			File jarDir = new File(jarUri).getParentFile();
			if (jarDir != null) {
				File nextToJar = new File(jarDir, "clientfiles");
				if (nextToJar.exists() && nextToJar.isDirectory()) {
					System.out.println("[PerfectSpoof] findClientFilesDir: found next to JAR");
					return nextToJar;
				}
			}
		} catch (URISyntaxException | SecurityException | NullPointerException e) {
			// Ignore
		}
		
		// 4. Classpath resource
		try {
			URL resource = PerfectSpoof.class.getClassLoader().getResource("clientfiles");
			if (resource != null && "file".equals(resource.getProtocol())) {
				File classpathDir = new File(resource.toURI());
				if (classpathDir.exists() && classpathDir.isDirectory()) {
					System.out.println("[PerfectSpoof] findClientFilesDir: found on classpath");
					return classpathDir;
				}
			}
		} catch (URISyntaxException | SecurityException | NullPointerException e) {
			// Ignore
		}
		
		System.out.println("[PerfectSpoof] findClientFilesDir: NOT FOUND in any location");
		return null;
	}
}
