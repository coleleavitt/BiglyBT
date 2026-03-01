package ghostfucker.proleech;

import com.biglybt.core.download.DownloadManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public interface IProleechUi {
	Control addToConfigSection(Composite composite, Control control);

	void addToMenu(Menu menu, DownloadManager[] dms, boolean enabled);
}
