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

/**
 * PerfectSpoof orchestrator. Manages the spoofing lifecycle:
 * - Reads config from COConfigurationManager ("PSisActive", "PSclientName", "PSclientVersion")
 * - Loads client profiles from clientfiles/[client]/[version].client
 * - Creates and validates PSClient instances
 */
public class PerfectSpoof {

	public static boolean isAvailable;
	public static boolean isActive;
	private static PSClient psClient;

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
			}
		} else if (!active) {
			isActive = false;
			psClient = null;
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
			}
		}
	}
}
