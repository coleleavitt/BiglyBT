package shu.utils;

import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.core.util.Constants;
import com.biglybt.ui.swt.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class ShuUI {

	public static boolean auth = true;
	public static String authStatut = "admin";
	public static String news = "";
	public static int numtry = 0;

	public static final String SHUVERSION = Constants.BIGLYBT_VERSION + "_DDJ";

	public static String[] splashs = new String[]{
			"Original Vuze Splash",
			"Dark Blue DDJ Hack by Manas",
			"\"S2 Mod\" by zoplex",
			"\"The X Style\" by zoplex",
			"\"Full red stylish\" by cubieuk",
			"\"Full red strange\" by unreal666",
			"\"Blue stylish\" by masterchief",
			"\"Blue frog\" by masterchief",
			"\"Green frog\" by masterchief",
			"\"Brown frog\" by masterchief",
			"\"Celtic Frog 1\" by Javed",
			"\"Celtic Frog 2\" by Javed",
			"\"Hardcore Style\" by Javed",
			"\"Dark orange frog\" by zoplex",
			"Abstract DigitalDJ Edition",
			"Toxic DigitalDJ Edition",
			"Dark Blue DigitalDJ Edition",
			"Light Blue DigitalDJ Edition",
			"Squareish DigitalDJ Edition",
			"SB-Innovation DigitalDJ",
			"SB-Innovation Butcho",
			"Dark Blue Butcho Edition",
			"Light Blue Butcho Edition",
			"Abstract Butcho Edition",
			"Light Green Butcho Edition",
			"Squareish Butcho Edition",
			"Toxic Butcho Edition",
			"\"S Mod\" by zoplex",
			"Original Azureus Splash",
			"Vuze White Splash",
			"No Splash Image",
			"\"Abuze blue frog 1\" by masterchief",
			"\"Abuze blue frog 2\" by masterchief",
			"\"Abuze green frog 1\" by masterchief",
			"\"Abuze green frog 2\" by masterchief",
			"Old Vuze Splash",
			"Tessy",
			"Vuze Frog",
			"BiglyBT"
	};

	public static boolean checking() {
		return true;
	}

	public static boolean isShowDonationWindow() {
		return false;
	}

	public static void news() {
	}

	public static boolean allowExtendedFeatures() {
		return true;
	}

	public static boolean isGodMode() {
		return true;
	}

	public static void openReleaseNotes() {
		String tempdir = System.getProperty("java.io.tmpdir", "");
		String filename = "SBIHack_ReleaseNotes.html";
		String releaseNotes = "";
		StringBuilder contents = new StringBuilder();

		try (BufferedReader input = new BufferedReader(
				new InputStreamReader(ShuUI.class.getResourceAsStream("/" + filename)))) {
			String line;
			while ((line = input.readLine()) != null) {
				contents.append(line);
				contents.append(System.getProperty("line.separator"));
			}
		} catch (Exception ignored) {
		}

		try {
			releaseNotes = contents.toString();
		} catch (Exception ignored) {
		}

		File f = new File(tempdir, filename);
		try (FileWriter fw = new FileWriter(f);
			 PrintWriter out = new PrintWriter(fw)) {
			out.println(releaseNotes);
			Utils.launch(f.toURI().toURL().toString());
		} catch (Exception ignored) {
		}
	}

	public static String getSplash() {
		String sSplashValue = COConfigurationManager.getStringParameter("Splash Selection", "Original Vuze Splash");

		for (int i = 0; i < splashs.length; ++i) {
			if (sSplashValue.equals(splashs[i])) {
				if (i != 0) {
					return "azureus_splash" + i;
				}
				return "azureus_splash";
			}
		}

		return "azureus_splash";
	}
}
