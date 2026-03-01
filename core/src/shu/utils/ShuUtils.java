package shu.utils;

import com.biglybt.core.util.Constants;

public class ShuUtils {

	public static boolean spoof = Boolean.parseBoolean(FileManager.getValue("azureus.spoof.boolean"));

	public static String spoofValue = FileManager.getValue("azureus.spoof.value") == null
			? Constants.BIGLYBT_VERSION
			: FileManager.getValue("azureus.spoof.value");

	public static boolean spoofBiglyBT = Boolean.parseBoolean(FileManager.getValue("azureus.spoof.spoofbiglybt"));

	public static boolean isPost4813;
	public static boolean isAz2Spoof;

	static {
		isPost4813 = spoof && !spoofBiglyBT
				&& (spoofValue.startsWith("5.") || spoofValue.startsWith("4.9") || spoofValue.startsWith("4.8.1.3"));
		isAz2Spoof = spoof && !spoofBiglyBT && spoofValue.startsWith("2.");
	}
}
