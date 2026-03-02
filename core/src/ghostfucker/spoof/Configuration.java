package ghostfucker.spoof;

import com.biglybt.core.logging.LogAlert;
import com.biglybt.core.logging.Logger;
import ghostfucker.spoof.client.ClientXmlParser;
import ghostfucker.spoof.client.SimpleClient;
import ghostfucker.spoof.client.Validator;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Scans and caches client profile configurations from the clientfiles/ directory.
 * Each subdirectory represents a client, each .client file represents a version.
 */
public class Configuration {

	private final Map<String, Map<String, SimpleClient>> versionClientMapping = new HashMap<>();

	public Configuration() {
		this.readClientFiles();
		if (this.versionClientMapping.keySet().size() == 0) {
			PerfectSpoof.isAvailable = false;
		}
	}

	public String[] getNames() {
		Set<String> set = this.versionClientMapping.keySet();
		String[] names = set.size() > 0 ? set.toArray(new String[set.size()]) : new String[]{""};
		Arrays.sort(names);
		return names;
	}

	public String[] getVersions(String client) {
		Map<String, SimpleClient> map = this.versionClientMapping.get(client);
		Set<String> set = (map == null ? new HashSet<>(0) : map.keySet());
		String[] versions = set.size() > 0 ? set.toArray(new String[set.size()]) : new String[]{""};
		Arrays.sort(versions);
		return versions;
	}

	public SimpleClient getClient(String client, String version) {
		System.out.println("[Configuration] getClient(" + client + ", " + version + ")");
		Map<String, SimpleClient> map = this.versionClientMapping.get(client);
		if (map != null) {
			SimpleClient sc = map.get(version);
			if (sc != null) {
				System.out.println("[Configuration] Found client: " + client + "/" + version);
				return sc;
			}
			System.out.println("[Configuration] Version not found: " + version + ", available: " + map.keySet());
		} else {
			System.out.println("[Configuration] Client not found: " + client + ", available: " + this.versionClientMapping.keySet());
		}

		// Return safe fallback with initialized peerId
		System.out.println("[Configuration] Returning fallback SimpleClient");
		SimpleClient sc = new SimpleClient();
		sc.peerId = new SimpleClient.PeerId();
		sc.peerId.type = "";
		sc.peerId.preFix = "";
		sc.peerId.length = 0;
		sc.peerId.isGlobal = true;
		return sc;
	}

	private void readClientFiles() {
		File dir = findClientFilesDir();
		if (dir != null && dir.exists()) {
			System.out.println("[PerfectSpoof] Found clientfiles at: " + dir.getAbsolutePath());
			File[] clients = dir.listFiles(new FileFilter() {
				public boolean accept(File file) {
					return file.isDirectory();
				}
			});

			if (clients == null) {
				return;
			}

			FileFilter versionFilter = new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(".client");
				}
			};

			for (File client : clients) {
				Map<String, SimpleClient> versionClientMap = new HashMap<>();

				File[] versions = client.listFiles(versionFilter);
				if (versions == null) {
					continue;
				}

				for (File version : versions) {
					SimpleClient simpleClient = this.readClient(version);
					if (simpleClient != null && this.isValid(simpleClient, version)) {
						versionClientMap.put(version.getName().replace(".client", ""), simpleClient);
					}
				}

				if (versionClientMap.size() > 0) {
					this.versionClientMapping.put(client.getName(), versionClientMap);
				}
			}
		} else {
			System.out.println("[PerfectSpoof] WARNING: clientfiles directory not found in any searched location");
		}
	}

	/**
	 * Searches multiple locations for the clientfiles directory:
	 * 1. Current working directory: ./clientfiles/
	 * 2. BiglyBT user config: ~/.biglybt/clientfiles
	 * 3. Next to the running JAR file
	 * 4. Classpath resource
	 */
	private static File findClientFilesDir() {
		// 1. Current working directory
		File cwd = new File("clientfiles");
		if (cwd.exists() && cwd.isDirectory()) {
			System.out.println("[PerfectSpoof] clientfiles: found in current working directory");
			return cwd;
		}
		System.out.println("[PerfectSpoof] clientfiles: not in CWD (" + new File(".").getAbsolutePath() + ")");

		// 2. User home: ~/.biglybt/clientfiles
		File userHome = new File(System.getProperty("user.home"), ".biglybt" + File.separator + "clientfiles");
		if (userHome.exists() && userHome.isDirectory()) {
			System.out.println("[PerfectSpoof] clientfiles: found in user home config");
			return userHome;
		}
		System.out.println("[PerfectSpoof] clientfiles: not in " + userHome.getAbsolutePath());

		// 3. Next to the JAR file
		try {
			URI jarUri = Configuration.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			File jarDir = new File(jarUri).getParentFile();
			if (jarDir != null) {
				File nextToJar = new File(jarDir, "clientfiles");
				if (nextToJar.exists() && nextToJar.isDirectory()) {
					System.out.println("[PerfectSpoof] clientfiles: found next to JAR");
					return nextToJar;
				}
				System.out.println("[PerfectSpoof] clientfiles: not next to JAR (" + jarDir.getAbsolutePath() + ")");
			}
		} catch (URISyntaxException | SecurityException | NullPointerException e) {
			System.out.println("[PerfectSpoof] clientfiles: could not resolve JAR location: " + e.getMessage());
		}

		// 4. Classpath resource
		try {
			URL resource = Configuration.class.getClassLoader().getResource("clientfiles");
			if (resource != null && "file".equals(resource.getProtocol())) {
				File classpathDir = new File(resource.toURI());
				if (classpathDir.exists() && classpathDir.isDirectory()) {
					System.out.println("[PerfectSpoof] clientfiles: found on classpath");
					return classpathDir;
				}
			}
		} catch (URISyntaxException | SecurityException | NullPointerException e) {
			System.out.println("[PerfectSpoof] clientfiles: could not resolve classpath resource: " + e.getMessage());
		}

		return null;
	}

	private SimpleClient readClient(File client) {
		try {
			return ClientXmlParser.parseSimpleClient(client);
		} catch (Exception e) {
			Logger.log(new LogAlert(false, LogAlert.AT_ERROR,
					"Cannot read " + client.getParentFile().getName() + "/" + client.getName(), e));
			return null;
		}
	}

	private boolean isValid(SimpleClient c, File file) {
		try {
			(new Validator(c)).run();
			return true;
		} catch (Validator.ValidationException ve) {
			Logger.log(new LogAlert(false, LogAlert.AT_ERROR,
					"Invalid client configuration: " + file.getParentFile().getName() + "/" + file.getName(), ve));
			return false;
		}
	}
}
