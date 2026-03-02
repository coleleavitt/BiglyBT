package ghostfucker.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyReader {
	private Properties properties = null;

	public PropertyReader(String path) throws IOException {
		this.properties = new Properties();
		InputStream inputStream = PropertyReader.class.getResourceAsStream(path);
		if (inputStream == null) {
			inputStream = new FileInputStream(path);
		}

		InputStream buffered = new BufferedInputStream(inputStream);
		this.properties.load(buffered);

		try {
			buffered.close();
		} catch (Exception var4) {
		}

	}

	public String getValue(int id) {
		return this.getValue(Integer.toString(id));
	}

	public String getValue(String key) {
		return (String)this.properties.get(key);
	}

	public int getSize() {
		return this.properties.size();
	}
}
