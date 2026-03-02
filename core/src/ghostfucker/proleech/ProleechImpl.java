package ghostfucker.proleech;

import com.biglybt.core.download.DownloadManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ProleechImpl implements IProleech, IProleechUi {

	public Control addToConfigSection(Composite composite, Control control) {
		// UI implementation delegated to uis module
		return null;
	}

	public void addToMenu(Menu menu, DownloadManager[] dms, boolean enabled) {
		try {
			Class<?> uiHelperClass = Class.forName("ghostfucker.proleech.UiHelper");
			java.lang.reflect.Method addToMenuMethod = uiHelperClass.getMethod("addToMenu", Menu.class, DownloadManager[].class, boolean.class);
			addToMenuMethod.invoke(null, menu, dms, enabled);
		} catch (Exception e) {
			// UI module not available, silently ignore
		}
	}

	public void exportTorrent(DownloadManager dm) {
		TorrentHelper.exportTorrent(dm);
	}

	public void exportTorrents(DownloadManager[] dms) {
		TorrentHelper.exportTorrents(dms);
	}
}
