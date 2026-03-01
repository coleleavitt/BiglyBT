package ghostfucker.proleech;

import com.biglybt.core.download.DownloadManager;
import com.biglybt.ui.swt.config.BooleanSwtParameter;
import com.biglybt.ui.swt.config.IntSwtParameter;
import com.biglybt.ui.swt.config.StringSwtParameter;
import com.biglybt.ui.swt.config.SwtParameterValueProcessor;
import com.biglybt.ui.swt.imageloader.ImageLoader;
import org.eclipse.swt.SWT;
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
		Group group = new Group(composite, SWT.NONE);
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

		Label label = new Label(group, SWT.NONE);
		label.setText("Export directory:");
		formData = new FormData();
		formData.top = new FormAttachment(control, 2);
		label.setLayoutData(formData);

		final StringSwtParameter stringParameter = new StringSwtParameter(group, "plSaveDir", null, null, (SwtParameterValueProcessor) null);
		formData = new FormData();
		formData.top = new FormAttachment(control);
		formData.left = new FormAttachment(label, 4);
		formData.width = 290;
		stringParameter.setLayoutData(formData);

		ImageLoader imageLoader = ImageLoader.getInstance();
		Image image = imageLoader.getImage("openFolderButton");
		Button button = new Button(group, SWT.PUSH);
		button.setImage(image);
		image.setBackground(button.getBackground());
		formData = new FormData();
		formData.top = new FormAttachment(control, -2);
		formData.left = new FormAttachment(stringParameter.getMainControl());
		button.setLayoutData(formData);
		button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				DirectoryDialog directoryDialog = new DirectoryDialog(composite.getShell(), SWT.SHEET);
				directoryDialog.setFilterPath((String) stringParameter.getValue());
				directoryDialog.setText("Save directory:");
				directoryDialog.setMessage("Please choose the ProLeech torrent save directory");
				String string = directoryDialog.open();
				if (string != null) {
					stringParameter.setValue(string);
				}
			}
		});

		BooleanSwtParameter booleanParameter = new BooleanSwtParameter(group, "plAutoCap", null, null, null);
		((Button) booleanParameter.getMainControl()).setText("Automatic export on torrent-startup");
		formData = new FormData();
		formData.top = new FormAttachment(button, 10);
		booleanParameter.setLayoutData(formData);

		BooleanSwtParameter booleanParameter2 = new BooleanSwtParameter(group, "plAutoCapPeersCnt", null, null, null);
		((Button) booleanParameter2.getMainControl()).setText("Export torrents with at least X peers.   X =");
		formData = new FormData();
		formData.top = new FormAttachment(booleanParameter.getMainControl(), 2);
		booleanParameter2.setLayoutData(formData);

		IntSwtParameter intParameter = new IntSwtParameter(group, "plAutoCapPeersCntVal", null, null, 5, 200, null);
		formData = new FormData();
		formData.top = new FormAttachment(booleanParameter.getMainControl(), -1);
		formData.left = new FormAttachment(booleanParameter2.getMainControl());
		intParameter.setLayoutData(formData);

		BooleanSwtParameter booleanParameter3 = new BooleanSwtParameter(group, "plOverrideTorrent", null, null, null);
		((Button) booleanParameter3.getMainControl()).setText("Replace existing torrents");
		formData = new FormData();
		formData.top = new FormAttachment(booleanParameter2.getMainControl(), 2);
		booleanParameter3.setLayoutData(formData);

		return group;
	}

	public static void addToMenu(Menu menu, final DownloadManager[] arrdownloadManager, boolean bl) {
		final Image image = new Image(menu.getDisplay(), UiHelper.class.getResourceAsStream("img.png"));
		new MenuItem(menu, SWT.SEPARATOR);
		MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setText("Export ProLeech torrent(s)");
		menuItem.setImage(image);
		menuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TorrentHelper.exportTorrents(arrdownloadManager);
			}
		});
		menuItem.setEnabled(bl);
		menuItem.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent disposeEvent) {
				if (image != null && !image.isDisposed()) {
					image.dispose();
				}
			}
		});
	}
}
