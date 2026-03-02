package shu.utils;

import com.biglybt.core.util.Constants;

import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

public class FileManager {

	private static Properties dbProps = createProperties();

	public static Object loadXML(String nomFichier) throws FileNotFoundException {
		XMLDecoder d = new XMLDecoder(new BufferedInputStream(new FileInputStream(nomFichier)));
		Object result = d.readObject();
		d.close();
		return result;
	}

	public static String getPref(int type, String key) {
		return (String) dbProps.get(type + "." + key);
	}

	public static String getValue(String key) {
		return (String) dbProps.get(key);
	}

	public static Properties createProperties() {
		Properties dbProps = new Properties();

		try {
			dbProps = new Properties();
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			InputStream in;
			if (ClassLoader.getSystemResourceAsStream("BiglyBTSpoof.properties") == null) {
				in = new FileInputStream("BiglyBTSpoof.properties");
			} else {
				in = ClassLoader.getSystemResourceAsStream("BiglyBTSpoof.properties");
			}

			dbProps.load(in);
			in.close();
		} catch (Exception var3) {
			dbProps.setProperty("azureus.spoof.boolean", "False");
			dbProps.setProperty("azureus.spoof.spoofbiglybt", "False");
			dbProps.setProperty("azureus.spoof.value", Constants.BIGLYBT_VERSION);
		}

		return dbProps;
	}
}
