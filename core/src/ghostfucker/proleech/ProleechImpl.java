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
		// UI implementation delegated to uis module
	}

	public void exportTorrent(DownloadManager dm) {
		TorrentHelper.exportTorrent(dm);
	}

	public void exportTorrents(DownloadManager[] dms) {
		TorrentHelper.exportTorrents(dms);
	}
}
