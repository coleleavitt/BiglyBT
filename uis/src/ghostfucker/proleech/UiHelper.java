package ghostfucker.proleech;

import com.biglybt.core.download.DownloadManager;
import com.biglybt.ui.swt.config.BooleanSwtParameter;
import com.biglybt.ui.swt.config.IntSwtParameter;
import com.biglybt.ui.swt.config.StringSwtParameter;
import com.biglybt.ui.swt.config.SwtParameterValueProcessor;
import com.biglybt.ui.swt.imageloader.ImageLoader;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class UiHelper {

	public static Control addToConfigSection(final Composite composite, Control control) {
		Group group = new Group(composite, 0);
		group.setText("ProLeech");
		FormLayout formLayout = new FormLayout();
		formLayout.marginBottom = 6;
		formLayout.marginRight = 6;
		formLayout.marginLeft = 6;
		formLayout.marginTop = 9;
		group.setLayout(formLayout);

		FormData formData = new FormData();
		formData.top = new FormAttachment(control, 3);
		group.setLayoutData(formData);

		// Export directory label
		Label label = new Label(group, 0);
		label.setText("Export directory:");
		formData = new FormData();
		formData.top = new FormAttachment(control, 2);
		label.setLayoutData(formData);

		// Export directory text field
		final StringSwtParameter stringParam = new StringSwtParameter(
				group, "plSaveDir", null, null, (SwtParameterValueProcessor) null);
		formData = new FormData();
		formData.top = new FormAttachment(control);
		formData.left = new FormAttachment(label, 4);
		formData.width = 290;
		stringParam.setLayoutData(formData);

		// Browse button
		ImageLoader imageLoader = ImageLoader.getInstance();
		Image image = imageLoader.getImage("openFolderButton");
		Button button = new Button(group, 8);
		button.setImage(image);
		image.setBackground(button.getBackground());
		formData = new FormData();
		formData.top = new FormAttachment(control, -2);
		formData.left = new FormAttachment(stringParam.getMainControl());
		button.setLayoutData(formData);
		button.addListener(13, new Listener() {
			public void handleEvent(Event event) {
				DirectoryDialog dialog = new DirectoryDialog(composite.getShell(), 65536);
				dialog.setFilterPath((String) stringParam.getValue());
				dialog.setText("Save directory:");
				dialog.setMessage("Please choose the ProLeech torrent save directory");
				String path = dialog.open();
				if (path != null) {
					stringParam.setValue(path);
				}
			}
		});

		// Auto-export checkbox
		BooleanSwtParameter autoCapParam = new BooleanSwtParameter(
				group, "plAutoCap", null, null, (SwtParameterValueProcessor) null);
		((Button) autoCapParam.getMainControl()).setText("Automatic export on torrent-startup");
		formData = new FormData();
		formData.top = new FormAttachment(button, 10);
		autoCapParam.setLayoutData(formData);

		// Min peers checkbox
		BooleanSwtParameter peersCntParam = new BooleanSwtParameter(
				group, "plAutoCapPeersCnt", null, null, (SwtParameterValueProcessor) null);
		((Button) peersCntParam.getMainControl()).setText("Export torrents with at least X peers.   X =");
		formData = new FormData();
		formData.top = new FormAttachment(autoCapParam.getMainControl(), 2);
		peersCntParam.setLayoutData(formData);

		// Min peers value
		IntSwtParameter peersCntValParam = new IntSwtParameter(
				group, "plAutoCapPeersCntVal", null, null, 5, 200, (SwtParameterValueProcessor) null);
		formData = new FormData();
		formData.top = new FormAttachment(autoCapParam.getMainControl(), -1);
		formData.left = new FormAttachment(peersCntParam.getMainControl());
		peersCntValParam.setLayoutData(formData);

		// Override existing torrents checkbox
		BooleanSwtParameter overrideParam = new BooleanSwtParameter(
				group, "plOverrideTorrent", null, null, (SwtParameterValueProcessor) null);
		((Button) overrideParam.getMainControl()).setText("Replace existing torrents");
		formData = new FormData();
		formData.top = new FormAttachment(peersCntParam.getMainControl(), 2);
		overrideParam.setLayoutData(formData);

		return group;
	}

	public static void addToMenu(Menu menu, final DownloadManager[] dms, boolean enabled) {
		final Image image = new Image(menu.getDisplay(), UiHelper.class.getResourceAsStream("img.png"));

		new MenuItem(menu, 2); // Separator
		MenuItem menuItem = new MenuItem(menu, 8);
		menuItem.setText("Export ProLeech torrent(s)");
		menuItem.setImage(image);
		menuItem.addListener(13, new Listener() {
			public void handleEvent(Event event) {
				TorrentHelper.exportTorrents(dms);
			}
		});
		menuItem.setEnabled(enabled);
		menuItem.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (image != null && !image.isDisposed()) {
					image.dispose();
				}
			}
		});
	}
}
