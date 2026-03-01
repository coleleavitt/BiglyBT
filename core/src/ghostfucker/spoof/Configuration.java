package ghostfucker.spoof;

import com.biglybt.core.logging.LogAlert;
import com.biglybt.core.logging.Logger;
import ghostfucker.spoof.client.ClientXmlParser;
import ghostfucker.spoof.client.SimpleClient;
import ghostfucker.spoof.client.Validator;

import java.io.File;
import java.io.FileFilter;
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
		Map<String, SimpleClient> map = this.versionClientMapping.get(client);
		if (map != null) {
			return map.get(version);
		}

		// Return empty fallback
		SimpleClient sc = new SimpleClient();
		sc.peerId = new SimpleClient.PeerId();
		sc.peerId.type = "";
		sc.peerId.preFix = "";
		return sc;
	}

	private void readClientFiles() {
		File dir = new File("clientfiles/");
		if (dir.exists()) {
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
		}
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
