package ghostfucker.proleech;

import ghostfucker.proleech.IProleech;

public class Config {
	public static final boolean IS_AVAILABLE = getInstance() != null;
	private static IProleech instance;

	public static IProleech getInstance() {
		if (instance == null) {
			try {
				instance = (IProleech) Class.forName("ghostfucker.proleech.ProleechImpl")
						.asSubclass(IProleech.class)
						.newInstance();
			} catch (Throwable ignored) {
			}
		}
		return instance;
	}
}
