/*
 * ShuView - Per-download spoofing options view
 * Provides per-torrent spoof configuration through the torrent details panel.
 */

package com.biglybt.ui.swt.views;

import com.biglybt.core.download.DownloadManager;
import com.biglybt.core.download.DownloadManagerOptionsHandler;
import com.biglybt.core.download.DownloadManagerStateAttributeListener;
import com.biglybt.core.internat.MessageText;
import com.biglybt.core.util.AERunnable;
import com.biglybt.core.util.CopyOnWriteList;
import com.biglybt.core.util.Debug;
import com.biglybt.ui.swt.Messages;
import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.config.BooleanSwtParameter;
import com.biglybt.ui.swt.config.FloatSwtParameter;
import com.biglybt.ui.swt.config.IntSwtParameter;
import com.biglybt.ui.swt.config.SwtParameter;
import com.biglybt.ui.swt.config.actionperformer.ChangeSelectionActionPerformer;
import com.biglybt.ui.swt.config.actionperformer.ChangeSelectionControlsActionPerformer;
import com.biglybt.ui.swt.config.actionperformer.IAdditionalActionPerformer;
import com.biglybt.ui.swt.pif.UISWTView;
import com.biglybt.ui.swt.pif.UISWTViewEvent;
import com.biglybt.ui.swt.pifimpl.UISWTViewCoreEventListener;
import com.biglybt.util.DataSourceUtils;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

public class ShuView implements UISWTViewCoreEventListener, DownloadManagerOptionsHandler.ParameterChangeListener {
   public static final String MSGID_PREFIX = "ShuView";
   public static final String VIEW_ID = "ShuView";
   private Display display;
   private boolean multi_view;
   private downloadStateBooleanParameterAdapter ds_boolparam_adapter = new downloadStateBooleanParameterAdapter();
   private downloadStateIntParameterAdapter ds_intparam_adapter = new downloadStateIntParameterAdapter();
   private downloadStateFloatParameterAdapter ds_floatparam_adapter = new downloadStateFloatParameterAdapter();
   private Map<String, Object> ds_parameters = new HashMap<>();
   private DownloadManagerOptionsHandler[] managers;
   Composite cShu;
   Composite parent;
   private ScrolledComposite scrolled_comp;
   private UISWTView swtView;
   Group gCheat;

   private void dataSourceChanged(Object newDataSource) {
      DownloadManagerOptionsHandler[] old_managers = this.managers;
      if (old_managers != null) {
         for(int i = 0; i < old_managers.length; ++i) {
            old_managers[i].removeListener(this);
         }
      }

      if (newDataSource instanceof DownloadManagerOptionsHandler) {
         this.multi_view = false;
         this.managers = new DownloadManagerOptionsHandler[]{(DownloadManagerOptionsHandler) newDataSource};
      } else {
         DownloadManager[] dms = DataSourceUtils.getDMs(newDataSource);
         if (dms.length > 0) {
            this.managers = new DownloadManagerOptionsHandler[dms.length];
            for (int i = 0; i < dms.length; i++) {
               this.managers[i] = new DMWrapper(dms[i]);
            }
            this.multi_view = dms.length > 1;
         }
      }

      if (this.parent != null && !this.parent.isDisposed()) {
         Utils.execSWTThread(new AERunnable() {
            public void runSupport() {
               ShuView.this.swt_refreshInfo();
            }
         });
      }

   }

   public boolean isCloneable() {
      return true;
   }

   public UISWTViewCoreEventListener getClone() {
      return new ShuView();
   }

   public void initialize(Composite composite) {
      this.display = composite.getDisplay();
      this.parent = composite;
      this.scrolled_comp = new ScrolledComposite(composite, 768);
      this.scrolled_comp.setExpandHorizontal(true);
      this.scrolled_comp.setExpandVertical(true);
      GridLayout layout1 = new GridLayout();
      layout1.horizontalSpacing = 0;
      layout1.verticalSpacing = 0;
      layout1.marginHeight = 0;
      layout1.marginWidth = 0;
      this.scrolled_comp.setLayout(layout1);
      GridData gridData1 = new GridData(4, 4, true, true);
      this.scrolled_comp.setLayoutData(gridData1);
      this.cShu = new Canvas(this.scrolled_comp, 0);
      GridData gridData2 = new GridData(272);
      this.cShu.setLayoutData(gridData2);
      FormLayout layout = new FormLayout();

      try {
         layout.spacing = 5;
      } catch (NoSuchFieldError var7) {
      }

      this.cShu.setLayout(layout);
      this.scrolled_comp.setContent(this.cShu);
      this.scrolled_comp.addControlListener(new ControlAdapter() {
         public void controlResized(ControlEvent e) {
            Utils.updateScrolledComposite(ShuView.this.scrolled_comp);
         }
      });
      Utils.execSWTThread(() -> this.swt_refreshInfo());
   }

   private void swt_refreshInfo() {
      if (this.managers != null && this.parent != null && !this.parent.isDisposed()) {
         Utils.disposeComposite(this.cShu, false);
         this.display = this.parent.getDisplay();
         final BooleanSwtParameter noUploadCtrl = new BooleanSwtParameter(this.cShu, "No Upload", "ConfigView.label.noupload", (String)null, this.ds_boolparam_adapter);
         FormData formData = new FormData();
         noUploadCtrl.setLayoutData(formData);
         this.ds_parameters.put("No Upload", noUploadCtrl);
         BooleanSwtParameter disableNoUpload4LanConnectionsCtrl = new BooleanSwtParameter(this.cShu, "Disable No Upload 4 Lan", "ConfigView.label.disablenoupload4lan", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(noUploadCtrl.getMainControl());
         formData.left = new FormAttachment(0, 18);
         disableNoUpload4LanConnectionsCtrl.setLayoutData(formData);
         this.ds_parameters.put("Disable No Upload 4 Lan", disableNoUpload4LanConnectionsCtrl);
         BooleanSwtParameter loggingCtrl = new BooleanSwtParameter(this.cShu, "ShuLogging", "ConfigView.label.shulogging", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(disableNoUpload4LanConnectionsCtrl.getMainControl(), 8);
         loggingCtrl.setLayoutData(formData);
         this.ds_parameters.put("ShuLogging", loggingCtrl);
         BooleanSwtParameter noUpAfterDLCtrl = new BooleanSwtParameter(this.cShu, "ctadl", "ConfigView.label.noupafterdl", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(loggingCtrl.getMainControl());
         noUpAfterDLCtrl.setLayoutData(formData);
         this.ds_parameters.put("ctadl", noUpAfterDLCtrl);
         final BooleanSwtParameter stopAfterXHoursCtrl = new BooleanSwtParameter(this.cShu, "stopafterxhours", "ConfigView.label.stopafterxhours", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(noUpAfterDLCtrl.getMainControl());
         formData.left = new FormAttachment(0, 18);
         stopAfterXHoursCtrl.setLayoutData(formData);
         this.ds_parameters.put("stopafterxhours", stopAfterXHoursCtrl);
         FloatSwtParameter stopHoursValue = new FloatSwtParameter(this.cShu, "Stop After X Hours Value", (String)null, (String)null, 0.0F, 6000.0F, true, 2, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(noUpAfterDLCtrl.getMainControl());
         formData.left = new FormAttachment(stopAfterXHoursCtrl.getMainControl());
         stopHoursValue.setLayoutData(formData);
         this.ds_parameters.put("Stop After X Hours Value", stopHoursValue);
         Label ltxtStop = new Label(this.cShu, 0);
         Messages.setLanguageText(ltxtStop, "ConfigView.label.stopafterxhourstxt");
         formData = new FormData();
         formData.top = new FormAttachment(noUpAfterDLCtrl.getMainControl());
         formData.left = new FormAttachment(stopHoursValue.getMainControl());
         ltxtStop.setLayoutData(formData);
         BooleanSwtParameter keepTorrentXHoursCtrl = new BooleanSwtParameter(this.cShu, "keeptorrentxhours", "ConfigView.label.keeptorrentxhours", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(stopAfterXHoursCtrl.getMainControl());
         formData.left = new FormAttachment(0, 18);
         keepTorrentXHoursCtrl.setLayoutData(formData);
         this.ds_parameters.put("keeptorrentxhours", keepTorrentXHoursCtrl);
         FloatSwtParameter keepTorrentValue = new FloatSwtParameter(this.cShu, "Keep Torrent X Hours Value", (String)null, (String)null, 0.0F, 6000.0F, true, 2, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(stopAfterXHoursCtrl.getMainControl());
         formData.left = new FormAttachment(keepTorrentXHoursCtrl.getMainControl());
         keepTorrentValue.setLayoutData(formData);
         this.ds_parameters.put("Keep Torrent X Hours Value", keepTorrentValue);
         Label ltxtKeep = new Label(this.cShu, 0);
         Messages.setLanguageText(ltxtKeep, "ConfigView.label.keeptorrentxhourstxt");
         formData = new FormData();
         formData.top = new FormAttachment(stopAfterXHoursCtrl.getMainControl());
         formData.left = new FormAttachment(keepTorrentValue.getMainControl());
         ltxtKeep.setLayoutData(formData);
         final BooleanSwtParameter noUpAfterDLRemoveCtrl = new BooleanSwtParameter(this.cShu, "ctadlremove", "ConfigView.label.noupafterdlremove", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(keepTorrentXHoursCtrl.getMainControl());
         formData.left = new FormAttachment(0, 18);
         noUpAfterDLRemoveCtrl.setLayoutData(formData);
         this.ds_parameters.put("ctadlremove", noUpAfterDLRemoveCtrl);
         final BooleanSwtParameter noUpAfterDLRemoveTorrentCtrl = new BooleanSwtParameter(this.cShu, "ctadlremovetorrent", "ConfigView.label.noupafterdlremovetorrent", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(noUpAfterDLRemoveCtrl.getMainControl());
         formData.left = new FormAttachment(0, 18);
         noUpAfterDLRemoveTorrentCtrl.setLayoutData(formData);
         this.ds_parameters.put("ctadlremovetorrent", noUpAfterDLRemoveTorrentCtrl);
         BooleanSwtParameter dontSendCompletedFlagCtrl = new BooleanSwtParameter(this.cShu, "dontsendcompletedflag", "ConfigView.label.dontsendcompletedflag", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(noUpAfterDLRemoveTorrentCtrl.getMainControl());
         dontSendCompletedFlagCtrl.setLayoutData(formData);
         this.ds_parameters.put("dontsendcompletedflag", dontSendCompletedFlagCtrl);
         Label uploadKickerTitle = new Label(this.cShu, 0);
         Messages.setLanguageText(uploadKickerTitle, "ConfigView.label.uploadkickertitle");
         formData = new FormData();
         formData.top = new FormAttachment(dontSendCompletedFlagCtrl.getMainControl(), 2);
         uploadKickerTitle.setLayoutData(formData);
         final FloatSwtParameter uploadKickerValue = new FloatSwtParameter(this.cShu, "Upload kicker value", (String)null, (String)null, 0.0F, 5000.0F, true, 2, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(dontSendCompletedFlagCtrl.getMainControl());
         formData.left = new FormAttachment(uploadKickerTitle);
         uploadKickerValue.setLayoutData(formData);
         this.ds_parameters.put("Upload kicker value", uploadKickerValue);
         Label uploadKickerString = new Label(this.cShu, 0);
         Messages.setLanguageText(uploadKickerString, "ConfigView.label.uploadkickerstring");
         formData = new FormData();
         formData.top = new FormAttachment(dontSendCompletedFlagCtrl.getMainControl(), 2);
         formData.left = new FormAttachment(uploadKickerValue.getMainControl());
         uploadKickerString.setLayoutData(formData);
         Button uploadKickerAddButton = new Button(this.cShu, 8);
         Messages.setLanguageText(uploadKickerAddButton, "ConfigView.label.uploadkickerAddButton");
         formData = new FormData();
         formData.top = new FormAttachment(dontSendCompletedFlagCtrl.getMainControl(), -2);
         formData.left = new FormAttachment(uploadKickerString, 5);
         formData.width = 75;
         uploadKickerAddButton.setLayoutData(formData);
         Button uploadKickerResetButton = new Button(this.cShu, 8);
         Messages.setLanguageText(uploadKickerResetButton, "ConfigView.label.uploadkickerResetButton");
         formData = new FormData();
         formData.top = new FormAttachment(dontSendCompletedFlagCtrl.getMainControl(), -2);
         formData.left = new FormAttachment(uploadKickerAddButton, -5);
         formData.width = 75;
         uploadKickerResetButton.setLayoutData(formData);
         Button uploadKickerFlashButton = new Button(this.cShu, 8);
         Messages.setLanguageText(uploadKickerFlashButton, "ConfigView.label.uploadkickerFlashButton");
         formData = new FormData();
         formData.top = new FormAttachment(dontSendCompletedFlagCtrl.getMainControl(), -2);
         formData.left = new FormAttachment(uploadKickerResetButton, 5);
         formData.width = 75;
         uploadKickerFlashButton.setLayoutData(formData);
         final Text uploadKickerInfo = new Text(this.cShu, 64);
         formData = new FormData();
         formData.top = new FormAttachment(dontSendCompletedFlagCtrl.getMainControl(), 2);
         formData.left = new FormAttachment(uploadKickerFlashButton, 5);
         formData.width = 300;
         uploadKickerInfo.setLayoutData(formData);
         uploadKickerInfo.setBackground(this.cShu.getBackground());
         uploadKickerInfo.setEnabled(false);
         final Runnable refresh = new Runnable() {
            public void run() {
               try {
                  String tmp = "";

                  for(int i = 0; i < ShuView.this.managers.length; ++i) {
                     DownloadManagerOptionsHandler dmos = ShuView.this.managers[i];
                     tmp = tmp + "Current [" + i + "]: " + Float.toString(dmos.getDownloadManager().getUploadKickerValue()) + "MB\n";
                  }

                  final String displayText = tmp;

                  Utils.execSWTThread(() -> {
                     if (uploadKickerInfo != null && !uploadKickerInfo.isDisposed()) {
                        uploadKickerInfo.setText(displayText);
                     }

                  });
               } catch (Exception var4) {
               }

            }
         };
         refresh.run();

         for(DownloadManagerOptionsHandler dmos : this.managers) {
            dmos.getDownloadManager().setUploadKickerRefreshRunnable(refresh);
         }

         uploadKickerAddButton.addListener(13, new Listener() {
            public void handleEvent(Event event) {
               try {
                  for(DownloadManagerOptionsHandler dmos : ShuView.this.managers) {
                     float val = dmos.getDownloadManager().getUploadKickerValue() + (Float)uploadKickerValue.getValue();
                     dmos.getDownloadManager().setUploadKickerValue(val);
                     refresh.run();
                  }
               } catch (Exception var7) {
               }

            }
         });
         uploadKickerResetButton.addListener(13, new Listener() {
            public void handleEvent(Event event) {
               try {
                  for(DownloadManagerOptionsHandler dmos : ShuView.this.managers) {
                     dmos.getDownloadManager().setUploadKickerValue(0.0F);
                     refresh.run();
                  }
               } catch (Exception var6) {
               }

            }
         });
         uploadKickerFlashButton.addListener(13, new Listener() {
            public void handleEvent(Event event) {
               try {
                  for(DownloadManagerOptionsHandler dmos : ShuView.this.managers) {
                     float f = dmos.getDownloadManager().getUploadKickerValue();
                     dmos.getDownloadManager().setUploadKickerValue(f + (Float)uploadKickerValue.getValue());
                     dmos.getDownloadManager().requestTrackerAnnounce(true);
                  }
               } catch (Exception var7) {
               }

            }
         });
         final BooleanSwtParameter ratioTool = new BooleanSwtParameter(this.cShu, "RatioTool", (String)null, (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(uploadKickerFlashButton);
         ratioTool.setLayoutData(formData);
         this.ds_parameters.put("RatioTool", ratioTool);
         Group ratioToolGroup = new Group(this.cShu, 0);
         Messages.setLanguageText(ratioToolGroup, "ConfigView.label.ratioTool");
         FormLayout formLayout = new FormLayout();
         formLayout.marginBottom = 6;
         formLayout.marginRight = 6;
         formLayout.marginLeft = 6;
         formLayout.marginTop = 9;
         ratioToolGroup.setLayout(formLayout);
         formData = new FormData();
         formData.top = new FormAttachment(uploadKickerFlashButton, 3);
         formData.left = new FormAttachment(ratioTool.getMainControl());
         ratioToolGroup.setLayoutData(formData);
         final BooleanSwtParameter ratioToolUploadSpeed1 = new BooleanSwtParameter(ratioToolGroup, "RatioToolUpload", "ConfigView.label.ratioToolUploadSpeed1", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(3);
         ratioToolUploadSpeed1.setLayoutData(formData);
         this.ds_parameters.put("RatioToolUpload", ratioToolUploadSpeed1);
         FloatSwtParameter ratioToolUploadSpeedMin = new FloatSwtParameter(ratioToolGroup, "RatioToolUploadSpeedMin", (String)null, (String)null, 0.0F, 1.0E7F, true, 3, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.left = new FormAttachment(ratioToolUploadSpeed1.getMainControl());
         ratioToolUploadSpeedMin.setLayoutData(formData);
         this.ds_parameters.put("RatioToolUploadSpeedMin", ratioToolUploadSpeedMin);
         Label ratioToolUploadSpeed2 = new Label(ratioToolGroup, 0);
         Messages.setLanguageText(ratioToolUploadSpeed2, "ConfigView.label.ratioToolUploadSpeed2");
         formData = new FormData();
         formData.top = new FormAttachment(4);
         formData.left = new FormAttachment(ratioToolUploadSpeedMin.getMainControl(), 1);
         ratioToolUploadSpeed2.setLayoutData(formData);
         FloatSwtParameter ratioToolUploadSpeedMax = new FloatSwtParameter(ratioToolGroup, "RatioToolUploadSpeedMax", (String)null, (String)null, 0.0F, 1.0E7F, true, 3, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.left = new FormAttachment(ratioToolUploadSpeed2, 3);
         ratioToolUploadSpeedMax.setLayoutData(formData);
         this.ds_parameters.put("RatioToolUploadSpeedMax", ratioToolUploadSpeedMax);
         Label ratioToolUploadSpeed3 = new Label(ratioToolGroup, 0);
         Messages.setLanguageText(ratioToolUploadSpeed3, "ConfigView.label.ratioToolUploadSpeed3");
         formData = new FormData();
         formData.top = new FormAttachment(4);
         formData.left = new FormAttachment(ratioToolUploadSpeedMax.getMainControl(), 1);
         ratioToolUploadSpeed3.setLayoutData(formData);
         final BooleanSwtParameter ratioToolDownloadSpeed1 = new BooleanSwtParameter(ratioToolGroup, "RatioToolDownload", "ConfigView.label.ratioToolDownloadSpeed1", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolUploadSpeedMax.getMainControl(), 2);
         ratioToolDownloadSpeed1.setLayoutData(formData);
         this.ds_parameters.put("RatioToolDownload", ratioToolDownloadSpeed1);
         FloatSwtParameter ratioToolDownloadSpeedMin = new FloatSwtParameter(ratioToolGroup, "RatioToolDownloadSpeedMin", (String)null, (String)null, 0.0F, 1.0E7F, true, 3, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolUploadSpeedMax.getMainControl());
         formData.left = new FormAttachment(ratioToolDownloadSpeed1.getMainControl());
         ratioToolDownloadSpeedMin.setLayoutData(formData);
         this.ds_parameters.put("RatioToolDownloadSpeedMin", ratioToolDownloadSpeedMin);
         Label ratioToolDownloadSpeed2 = new Label(ratioToolGroup, 0);
         Messages.setLanguageText(ratioToolDownloadSpeed2, "ConfigView.label.ratioToolUploadSpeed2");
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolUploadSpeedMax.getMainControl(), 3);
         formData.left = new FormAttachment(ratioToolDownloadSpeedMin.getMainControl(), 1);
         ratioToolDownloadSpeed2.setLayoutData(formData);
         FloatSwtParameter ratioToolDownloadSpeedMax = new FloatSwtParameter(ratioToolGroup, "RatioToolDownloadSpeedMax", (String)null, (String)null, 0.0F, 1.0E7F, true, 3, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolUploadSpeedMax.getMainControl());
         formData.left = new FormAttachment(ratioToolDownloadSpeed2, 3);
         ratioToolDownloadSpeedMax.setLayoutData(formData);
         this.ds_parameters.put("RatioToolDownloadSpeedMax", ratioToolDownloadSpeedMax);
         Label ratioToolDownloadSpeed3 = new Label(ratioToolGroup, 0);
         Messages.setLanguageText(ratioToolDownloadSpeed3, "ConfigView.label.ratioToolUploadSpeed3");
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolUploadSpeedMax.getMainControl(), 3);
         formData.left = new FormAttachment(ratioToolDownloadSpeedMax.getMainControl(), 1);
         ratioToolDownloadSpeed3.setLayoutData(formData);
         Label ratioToolPercentDone1 = new Label(ratioToolGroup, 0);
         Messages.setLanguageText(ratioToolPercentDone1, "ConfigView.label.ratioToolPercentDone");
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolUploadSpeedMax.getMainControl(), 3);
         formData.left = new FormAttachment(ratioToolDownloadSpeed3, 15);
         ratioToolPercentDone1.setLayoutData(formData);
         FloatSwtParameter ratioToolPercentDoneValue = new FloatSwtParameter(ratioToolGroup, "RatioToolPercentDone", (String)null, (String)null, 0.0F, 100.0F, true, 2, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolUploadSpeedMax.getMainControl());
         formData.left = new FormAttachment(ratioToolPercentDone1, 3);
         ratioToolPercentDoneValue.setLayoutData(formData);
         this.ds_parameters.put("RatioToolPercentDone", ratioToolPercentDoneValue);
         Label ratioToolPercentDone2 = new Label(ratioToolGroup, 0);
         ratioToolPercentDone2.setText("%");
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolUploadSpeedMax.getMainControl(), 3);
         formData.left = new FormAttachment(ratioToolPercentDoneValue.getMainControl(), 1);
         ratioToolPercentDone2.setLayoutData(formData);
         final BooleanSwtParameter ratioToolStopUpload1 = new BooleanSwtParameter(ratioToolGroup, "RatioToolStopUpload", "ConfigView.label.ratoToolStopUpload1", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolDownloadSpeedMax.getMainControl(), 2);
         ratioToolStopUpload1.setLayoutData(formData);
         this.ds_parameters.put("RatioToolStopUpload", ratioToolStopUpload1);
         final FloatSwtParameter ratioToolStopUploadValue = new FloatSwtParameter(ratioToolGroup, "RatioToolStopUploadValue", (String)null, (String)null, 0.0F, 1.0E9F, true, 2, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolDownloadSpeedMax.getMainControl());
         formData.left = new FormAttachment(ratioToolStopUpload1.getMainControl());
         ratioToolStopUploadValue.setLayoutData(formData);
         this.ds_parameters.put("RatioToolStopUploadValue", ratioToolStopUploadValue);
         final Label ratioToolStopUpload2 = new Label(ratioToolGroup, 0);
         Messages.setLanguageText(ratioToolStopUpload2, "ConfigView.label.ratoToolStopUpload2");
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolDownloadSpeedMax.getMainControl(), 3);
         formData.left = new FormAttachment(ratioToolStopUploadValue.getMainControl(), 1);
         ratioToolStopUpload2.setLayoutData(formData);
         final BooleanSwtParameter ratioToolStopDownload1 = new BooleanSwtParameter(ratioToolGroup, "RatioToolStopDownload", "ConfigView.label.ratoToolStopUpload1", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolDownloadSpeedMax.getMainControl(), 2);
         formData.left = new FormAttachment(ratioToolStopUpload2, 15);
         ratioToolStopDownload1.setLayoutData(formData);
         this.ds_parameters.put("RatioToolStopDownload", ratioToolStopDownload1);
         final FloatSwtParameter ratioToolStopDownloadValue = new FloatSwtParameter(ratioToolGroup, "RatioToolStopDownloadValue", (String)null, (String)null, 0.0F, 1.0E9F, true, 2, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolDownloadSpeedMax.getMainControl());
         formData.left = new FormAttachment(ratioToolStopDownload1.getMainControl());
         ratioToolStopDownloadValue.setLayoutData(formData);
         this.ds_parameters.put("RatioToolStopDownloadValue", ratioToolStopDownloadValue);
         final Label ratioToolStopDownload2 = new Label(ratioToolGroup, 0);
         Messages.setLanguageText(ratioToolStopDownload2, "ConfigView.label.ratoToolStopDownload");
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolDownloadSpeedMax.getMainControl(), 3);
         formData.left = new FormAttachment(ratioToolStopDownloadValue.getMainControl(), 1);
         ratioToolStopDownload2.setLayoutData(formData);
         final BooleanSwtParameter ratioToolStopPeers = new BooleanSwtParameter(ratioToolGroup, "RatioToolStopPeers", "ConfigView.label.ratoToolStopPeers", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolStopDownloadValue.getMainControl(), 2);
         ratioToolStopPeers.setLayoutData(formData);
         this.ds_parameters.put("RatioToolStopPeers", ratioToolStopPeers);
         IntSwtParameter ratioToolStopPeersValue = new IntSwtParameter(ratioToolGroup, "RatioToolStopPeersValue", (String)null, (String)null, 0, 250, this.ds_intparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolStopDownloadValue.getMainControl());
         formData.left = new FormAttachment(ratioToolStopPeers.getMainControl(), 1);
         ratioToolStopPeersValue.setLayoutData(formData);
         this.ds_parameters.put("RatioToolStopPeersValue", ratioToolStopPeersValue);
         final BooleanSwtParameter ratioToolFakeIntelligent = new BooleanSwtParameter(ratioToolGroup, "RatioToolFakeIntelligent", "ConfigView.label.fakeuploadrmintelligent", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolStopPeersValue.getMainControl(), 1);
         ratioToolFakeIntelligent.setLayoutData(formData);
         this.ds_parameters.put("RatioToolFakeIntelligent", ratioToolFakeIntelligent);
         final BooleanSwtParameter ratioToolStartSlow1 = new BooleanSwtParameter(ratioToolGroup, "RatioToolStartSlow", "ConfigView.label.ratoToolStartSlow1", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolFakeIntelligent.getMainControl(), 2);
         ratioToolStartSlow1.setLayoutData(formData);
         this.ds_parameters.put("RatioToolStartSlow", ratioToolStartSlow1);
         final FloatSwtParameter ratioToolStartSlow1Value = new FloatSwtParameter(ratioToolGroup, "RatioToolStartSlowValue", (String)null, (String)null, 0.0F, 100.0F, true, 2, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolFakeIntelligent.getMainControl());
         formData.left = new FormAttachment(ratioToolStartSlow1.getMainControl());
         ratioToolStartSlow1Value.setLayoutData(formData);
         this.ds_parameters.put("RatioToolStartSlowValue", ratioToolStartSlow1Value);
         final Label ratioToolStartSlow2 = new Label(ratioToolGroup, 0);
         Messages.setLanguageText(ratioToolStartSlow2, "ConfigView.label.ratoToolStartSlow2");
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolFakeIntelligent.getMainControl(), 3);
         formData.left = new FormAttachment(ratioToolStartSlow1Value.getMainControl(), 1);
         ratioToolStartSlow2.setLayoutData(formData);
         final BooleanSwtParameter ratioToolAddStopped = new BooleanSwtParameter(ratioToolGroup, "RatioToolAddStopped", "ConfigView.label.ratoToolAddStopped", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolStartSlow1Value.getMainControl(), 2);
         ratioToolAddStopped.setLayoutData(formData);
         this.ds_parameters.put("RatioToolAddStopped", ratioToolAddStopped);
         final BooleanSwtParameter fakeOffFakeAddedToRealCtrl = new BooleanSwtParameter(this.cShu, "FakeOff FakeAddedToReal", "ConfigView.label.fakeofffakeaddedtoreal", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ratioToolGroup, 4);
         fakeOffFakeAddedToRealCtrl.setLayoutData(formData);
         this.ds_parameters.put("FakeOff FakeAddedToReal", fakeOffFakeAddedToRealCtrl);
         final BooleanSwtParameter enableFakeCtrl = new BooleanSwtParameter(this.cShu, "Enable Fake", "ConfigView.label.dash", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(fakeOffFakeAddedToRealCtrl.getMainControl(), 1);
         enableFakeCtrl.setLayoutData(formData);
         this.ds_parameters.put("Enable Fake", enableFakeCtrl);
         Group gFake = new Group(this.cShu, 0);
         Messages.setLanguageText(gFake, "ConfigView.label.group.fake");
         formLayout = new FormLayout();
         formLayout.marginBottom = 6;
         formLayout.marginRight = 6;
         formLayout.marginLeft = 6;
         gFake.setLayout(formLayout);
         formData = new FormData();
         formData.top = new FormAttachment(fakeOffFakeAddedToRealCtrl.getMainControl(), 4);
         formData.left = new FormAttachment(enableFakeCtrl.getMainControl());
         gFake.setLayoutData(formData);
         Label ltxtPeer = new Label(gFake, 0);
         Messages.setLanguageText(ltxtPeer, "ConfigView.label.peerfake");
         formData = new FormData();
         formData.top = new FormAttachment(1, 7);
         ltxtPeer.setLayoutData(formData);
         IntSwtParameter peerFakeValue = new IntSwtParameter(gFake, "Peer Fake", (String)null, (String)null, 0, 250, this.ds_intparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(1, 5);
         formData.left = new FormAttachment(ltxtPeer);
         peerFakeValue.setLayoutData(formData);
         this.ds_parameters.put("Peer Fake", peerFakeValue);
         Label ltxt2Peer = new Label(gFake, 0);
         Messages.setLanguageText(ltxt2Peer, "ConfigView.label.peerfake2");
         formData = new FormData();
         formData.top = new FormAttachment(1, 7);
         formData.left = new FormAttachment(peerFakeValue.getMainControl());
         ltxt2Peer.setLayoutData(formData);
         IntSwtParameter peerSeedRatioValue = new IntSwtParameter(gFake, "PeerSeed Ratio", (String)null, (String)null, 0, 250, this.ds_intparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(1, 5);
         formData.left = new FormAttachment(ltxt2Peer);
         peerSeedRatioValue.setLayoutData(formData);
         this.ds_parameters.put("PeerSeed Ratio", peerSeedRatioValue);
         Label ltxt3Seed = new Label(gFake, 0);
         Messages.setLanguageText(ltxt3Seed, "ConfigView.label.seedfake3");
         formData = new FormData();
         formData.top = new FormAttachment(1, 7);
         formData.left = new FormAttachment(peerSeedRatioValue.getMainControl());
         ltxt3Seed.setLayoutData(formData);
         BooleanSwtParameter swarmPeerPoolCtrl = new BooleanSwtParameter(gFake, "SwarmPeerPool", "ConfigView.label.swarmpeerpool", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ltxtPeer, 1);
         formData.left = new FormAttachment(0, 18);
         swarmPeerPoolCtrl.setLayoutData(formData);
         this.ds_parameters.put("SwarmPeerPool", swarmPeerPoolCtrl);
         final BooleanSwtParameter safeFakeUploadCtrl = new BooleanSwtParameter(gFake, "Safe FakeUpload", "ConfigView.label.safefakeupload", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(swarmPeerPoolCtrl.getMainControl(), 5);
         safeFakeUploadCtrl.setLayoutData(formData);
         this.ds_parameters.put("Safe FakeUpload", safeFakeUploadCtrl);
         Label ltxtSafe = new Label(gFake, 0);
         Messages.setLanguageText(ltxtSafe, "ConfigView.label.safefakeupload1");
         formData = new FormData();
         formData.top = new FormAttachment(safeFakeUploadCtrl.getMainControl(), 1);
         formData.left = new FormAttachment(0, 18);
         ltxtSafe.setLayoutData(formData);
         FloatSwtParameter safeFakeUploadValue = new FloatSwtParameter(gFake, "Safe Fake UploadValue", (String)null, (String)null, 0.0F, 50.0F, true, 3, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(safeFakeUploadCtrl.getMainControl());
         formData.left = new FormAttachment(ltxtSafe);
         safeFakeUploadValue.setLayoutData(formData);
         this.ds_parameters.put("Safe Fake UploadValue", safeFakeUploadValue);
         Label ltxtSafe2 = new Label(gFake, 0);
         Messages.setLanguageText(ltxtSafe2, "ConfigView.label.safefakeupload2");
         formData = new FormData();
         formData.top = new FormAttachment(safeFakeUploadCtrl.getMainControl(), 1);
         formData.left = new FormAttachment(safeFakeUploadValue.getMainControl());
         ltxtSafe2.setLayoutData(formData);
         final BooleanSwtParameter downloadReducCtrl = new BooleanSwtParameter(gFake, "Download Reduc", "ConfigView.label.downloadreduc", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ltxtSafe, 5);
         downloadReducCtrl.setLayoutData(formData);
         this.ds_parameters.put("Download Reduc", downloadReducCtrl);
         final BooleanSwtParameter downloadReducAloneCtrl = new BooleanSwtParameter(gFake, "Download ReducAlone", (String)null, (String)null, this.ds_boolparam_adapter);
         Messages.setLanguageText(downloadReducAloneCtrl.getMainControl(), "ConfigView.label.downloadreducalone");
         formData = new FormData();
         formData.top = new FormAttachment(downloadReducCtrl.getMainControl(), 1);
         formData.left = new FormAttachment(0, 18);
         downloadReducAloneCtrl.setLayoutData(formData);
         this.ds_parameters.put("Download ReducAlone", downloadReducAloneCtrl);
         final BooleanSwtParameter downloadReducMixCtrl = new BooleanSwtParameter(gFake, "Download ReducMix", "ConfigView.label.downloadreducmix", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(downloadReducAloneCtrl.getMainControl(), 1);
         formData.left = new FormAttachment(0, 18);
         downloadReducMixCtrl.setLayoutData(formData);
         this.ds_parameters.put("Download ReducMix", downloadReducMixCtrl);
         Label ltxtDLReduc = new Label(gFake, 0);
         Messages.setLanguageText(ltxtDLReduc, "ConfigView.label.downloadreductxt");
         formData = new FormData();
         formData.top = new FormAttachment(downloadReducMixCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(0, 18);
         ltxtDLReduc.setLayoutData(formData);
         IntSwtParameter downloadReducValueCtrl = new IntSwtParameter(gFake, "DownloadReduc Value", (String)null, (String)null, 0, 1000, this.ds_intparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(downloadReducMixCtrl.getMainControl());
         formData.left = new FormAttachment(ltxtDLReduc);
         downloadReducValueCtrl.setLayoutData(formData);
         this.ds_parameters.put("DownloadReduc Value", downloadReducValueCtrl);
         Label lValueDLReducSpef = new Label(gFake, 0);
         Messages.setLanguageText(lValueDLReducSpef, "ConfigView.label.downloadreductxt1");
         formData = new FormData();
         formData.top = new FormAttachment(downloadReducMixCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(downloadReducValueCtrl.getMainControl());
         lValueDLReducSpef.setLayoutData(formData);
         final BooleanSwtParameter noReportCtrl = new BooleanSwtParameter(gFake, "No Report", (String)null, (String)null, this.ds_boolparam_adapter);
         Messages.setLanguageText(noReportCtrl.getMainControl(), "ConfigView.label.noreport");
         formData = new FormData();
         formData.top = new FormAttachment(downloadReducValueCtrl.getMainControl(), 10);
         noReportCtrl.setLayoutData(formData);
         this.ds_parameters.put("No Report", noReportCtrl);
         final BooleanSwtParameter noReportLeechCtrl = new BooleanSwtParameter(gFake, "No ReportLeech", "ConfigView.label.noreportleech", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(noReportCtrl.getMainControl(), 1);
         formData.left = new FormAttachment(0, 18);
         noReportLeechCtrl.setLayoutData(formData);
         this.ds_parameters.put("No ReportLeech", noReportLeechCtrl);
         final BooleanSwtParameter noReportSeedCtrl = new BooleanSwtParameter(gFake, "No ReportSeed", "ConfigView.label.noreportseed", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(noReportLeechCtrl.getMainControl(), 1);
         formData.left = new FormAttachment(0, 18);
         noReportSeedCtrl.setLayoutData(formData);
         this.ds_parameters.put("No ReportSeed", noReportSeedCtrl);
         final BooleanSwtParameter noReportCustom = new BooleanSwtParameter(gFake, "No ReportCustom", "ConfigView.label.noReportCustom", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(noReportSeedCtrl.getMainControl(), 1);
         formData.left = new FormAttachment(0, 18);
         noReportCustom.setLayoutData(formData);
         this.ds_parameters.put("No ReportCustom", noReportCustom);
         FloatSwtParameter noReportCustomValue = new FloatSwtParameter(gFake, "No ReportCustomValue", (String)null, (String)null, 1.0F, 99.0F, false, 2, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(noReportSeedCtrl.getMainControl());
         formData.left = new FormAttachment(noReportCustom.getMainControl());
         noReportCustomValue.setLayoutData(formData);
         this.ds_parameters.put("No ReportCustomValue", noReportCustomValue);
         Label noRepCusAdditional = new Label(gFake, 0);
         Messages.setLanguageText(noRepCusAdditional, "ConfigView.label.noReportCustomAdditional");
         formData = new FormData();
         formData.top = new FormAttachment(noReportSeedCtrl.getMainControl(), 2);
         formData.left = new FormAttachment(noReportCustomValue.getMainControl());
         noRepCusAdditional.setLayoutData(formData);
         final BooleanSwtParameter noReportAuto = new BooleanSwtParameter(gFake, "No ReportAuto", "ConfigView.label.noReportAuto", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(noReportCustom.getMainControl(), 1);
         formData.left = new FormAttachment(0, 18);
         noReportAuto.setLayoutData(formData);
         this.ds_parameters.put("No ReportAuto", noReportAuto);
         final BooleanSwtParameter fakeUploadRatioCtrl = new BooleanSwtParameter(gFake, "Fake UploadRM", (String)null, (String)null, this.ds_boolparam_adapter);
         Messages.setLanguageText(fakeUploadRatioCtrl.getMainControl(), "ConfigView.label.fakeuploadrm");
         formData = new FormData();
         formData.top = new FormAttachment(noReportAuto.getMainControl(), 10);
         fakeUploadRatioCtrl.setLayoutData(formData);
         this.ds_parameters.put("Fake UploadRM", fakeUploadRatioCtrl);
         Label ltxtRatio = new Label(gFake, 0);
         Messages.setLanguageText(ltxtRatio, "ConfigView.label.fakeuploadrmtxt1");
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadRatioCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(0, 18);
         ltxtRatio.setLayoutData(formData);
         FloatSwtParameter fakeUploadRatioValueCtrl = new FloatSwtParameter(gFake, "FakeUploadRM Value", (String)null, (String)null, 0.0F, 50.0F, true, 3, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadRatioCtrl.getMainControl());
         formData.left = new FormAttachment(ltxtRatio);
         fakeUploadRatioValueCtrl.setLayoutData(formData);
         this.ds_parameters.put("FakeUploadRM Value", fakeUploadRatioValueCtrl);
         Label lValueRMSpef = new Label(gFake, 0);
         Messages.setLanguageText(lValueRMSpef, "ConfigView.label.fakeuploadrmtxt");
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadRatioCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(fakeUploadRatioValueCtrl.getMainControl());
         lValueRMSpef.setLayoutData(formData);
         FloatSwtParameter fakeUploadRatioValueMaxCtrl = new FloatSwtParameter(gFake, "FakeUploadRM ValueMax", (String)null, (String)null, 0.0F, 50.0F, true, 3, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadRatioCtrl.getMainControl());
         formData.left = new FormAttachment(lValueRMSpef);
         fakeUploadRatioValueMaxCtrl.setLayoutData(formData);
         this.ds_parameters.put("FakeUploadRM ValueMax", fakeUploadRatioValueMaxCtrl);
         Label lValueRMMaxSpef = new Label(gFake, 0);
         Messages.setLanguageText(lValueRMMaxSpef, "ConfigView.label.fakeuploadrmmaxtxt");
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadRatioCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(fakeUploadRatioValueMaxCtrl.getMainControl());
         lValueRMMaxSpef.setLayoutData(formData);
         final BooleanSwtParameter fakeUploadRMIntelligentCtrl = new BooleanSwtParameter(gFake, "Fake UploadRMIntelligent", "ConfigView.label.fakeuploadrmintelligent", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadRatioValueCtrl.getMainControl(), 2);
         formData.left = new FormAttachment(0, 18);
         fakeUploadRMIntelligentCtrl.setLayoutData(formData);
         this.ds_parameters.put("Fake UploadRMIntelligent", fakeUploadRMIntelligentCtrl);
         final BooleanSwtParameter fakeUploadRatioContinueCtrl = new BooleanSwtParameter(gFake, "Fake UploadRMContinue", "ConfigView.label.fakeuploadrmcontinue", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadRMIntelligentCtrl.getMainControl(), 2);
         formData.left = new FormAttachment(0, 18);
         fakeUploadRatioContinueCtrl.setLayoutData(formData);
         this.ds_parameters.put("Fake UploadRMContinue", fakeUploadRatioContinueCtrl);
         final BooleanSwtParameter fakeUploadSpeedRatioCtrl = new BooleanSwtParameter(gFake, "Fake UploadSpeedRatio", (String)null, (String)null, this.ds_boolparam_adapter);
         Messages.setLanguageText(fakeUploadSpeedRatioCtrl.getMainControl(), "ConfigView.label.fakeuploadspeedratio");
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadRatioContinueCtrl.getMainControl(), 10);
         fakeUploadSpeedRatioCtrl.setLayoutData(formData);
         this.ds_parameters.put("Fake UploadSpeedRatio", fakeUploadSpeedRatioCtrl);
         final BooleanSwtParameter startFakeCtrl = new BooleanSwtParameter(gFake, "Start Fake", "ConfigView.label.startfake", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadSpeedRatioCtrl.getMainControl(), 3);
         formData.left = new FormAttachment(0, 18);
         startFakeCtrl.setLayoutData(formData);
         this.ds_parameters.put("Start Fake", startFakeCtrl);
         FloatSwtParameter startFakeValue = new FloatSwtParameter(gFake, "Start Fake PourcentValue", (String)null, (String)null, 0.0F, 100.0F, true, 3, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadSpeedRatioCtrl.getMainControl());
         formData.left = new FormAttachment(startFakeCtrl.getMainControl());
         startFakeValue.setLayoutData(formData);
         this.ds_parameters.put("Start Fake PourcentValue", startFakeValue);
         Label startLabel = new Label(gFake, 0);
         Messages.setLanguageText(startLabel, "ConfigView.label.startlabelpourcent");
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadSpeedRatioCtrl.getMainControl(), 3);
         formData.left = new FormAttachment(startFakeValue.getMainControl(), 1);
         startLabel.setLayoutData(formData);
         final BooleanSwtParameter fakeUploadSRIntelligentCtrl = new BooleanSwtParameter(gFake, "Fake UploadSRIntelligent", "ConfigView.label.fakeuploadsrintelligent", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(startFakeCtrl.getMainControl(), 2);
         formData.left = new FormAttachment(0, 18);
         fakeUploadSRIntelligentCtrl.setLayoutData(formData);
         this.ds_parameters.put("Fake UploadSRIntelligent", fakeUploadSRIntelligentCtrl);
         final BooleanSwtParameter fakeUploadAddCtrl = new BooleanSwtParameter(gFake, "Fake UploadAdd", "ConfigView.label.fakeuploadadd", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadSRIntelligentCtrl.getMainControl(), 2);
         formData.left = new FormAttachment(0, 18);
         fakeUploadAddCtrl.setLayoutData(formData);
         this.ds_parameters.put("Fake UploadAdd", fakeUploadAddCtrl);
         final BooleanSwtParameter showAsSeedCtrl = new BooleanSwtParameter(gFake, "Show As Seed", "ConfigView.label.showasseed", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadAddCtrl.getMainControl(), 2);
         formData.left = new FormAttachment(0, 18);
         showAsSeedCtrl.setLayoutData(formData);
         this.ds_parameters.put("Show As Seed", showAsSeedCtrl);
         final BooleanSwtParameter swarmSpeedCtrl = new BooleanSwtParameter(gFake, "Fake SwarmSpeed", "ConfigView.label.fakeswarmspeed", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(showAsSeedCtrl.getMainControl(), 2);
         formData.left = new FormAttachment(0, 18);
         swarmSpeedCtrl.setLayoutData(formData);
         this.ds_parameters.put("Fake SwarmSpeed", swarmSpeedCtrl);
         final BooleanSwtParameter stopIfSwarmDrops = new BooleanSwtParameter(gFake, "Fake StopIfSwarmDrops", "ConfigView.label.stopifswarmdrops", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(swarmSpeedCtrl.getMainControl(), 2);
         formData.left = new FormAttachment(0, 18);
         stopIfSwarmDrops.setLayoutData(formData);
         this.ds_parameters.put("Fake StopIfSwarmDrops", stopIfSwarmDrops);
         final BooleanSwtParameter isRatioCtrl = new BooleanSwtParameter(gFake, "Fake UploadIsRatio", "ConfigView.label.fakeuploadisratio", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(stopIfSwarmDrops.getMainControl(), 2);
         formData.left = new FormAttachment(0, 18);
         isRatioCtrl.setLayoutData(formData);
         this.ds_parameters.put("Fake UploadIsRatio", isRatioCtrl);
         Label ltxtSpeedRatio1 = new Label(gFake, 0);
         Messages.setLanguageText(ltxtSpeedRatio1, "ConfigView.label.fakeuploadspeedratiortxt1");
         formData = new FormData();
         formData.top = new FormAttachment(isRatioCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(0, 18);
         ltxtSpeedRatio1.setLayoutData(formData);
         FloatSwtParameter fakeUploadSpeedRatioRValueCtrl = new FloatSwtParameter(gFake, "FakeUploadSpeedRatio RValue", (String)null, (String)null, 0.0F, 50.0F, true, 3, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(isRatioCtrl.getMainControl());
         formData.left = new FormAttachment(ltxtSpeedRatio1);
         fakeUploadSpeedRatioRValueCtrl.setLayoutData(formData);
         this.ds_parameters.put("FakeUploadSpeedRatio RValue", fakeUploadSpeedRatioRValueCtrl);
         Label lValueSpeedRatioRSpef = new Label(gFake, 0);
         Messages.setLanguageText(lValueSpeedRatioRSpef, "ConfigView.label.fakeuploadspeedratiortxt");
         formData = new FormData();
         formData.top = new FormAttachment(isRatioCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(fakeUploadSpeedRatioRValueCtrl.getMainControl());
         lValueSpeedRatioRSpef.setLayoutData(formData);
         FloatSwtParameter fakeUploadSpeedRatioRValueMaxCtrl = new FloatSwtParameter(gFake, "FakeUploadSpeedRatio RValueMax", (String)null, (String)null, 0.0F, 50.0F, true, 3, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(isRatioCtrl.getMainControl());
         formData.left = new FormAttachment(lValueSpeedRatioRSpef);
         fakeUploadSpeedRatioRValueMaxCtrl.setLayoutData(formData);
         this.ds_parameters.put("FakeUploadSpeedRatio RValueMax", fakeUploadSpeedRatioRValueMaxCtrl);
         Label lValueSpeedRatioRMaxSpef = new Label(gFake, 0);
         Messages.setLanguageText(lValueSpeedRatioRMaxSpef, "ConfigView.label.fakeuploadspeedratiomaxrtxt");
         formData = new FormData();
         formData.top = new FormAttachment(isRatioCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(fakeUploadSpeedRatioRValueMaxCtrl.getMainControl());
         lValueSpeedRatioRMaxSpef.setLayoutData(formData);
         FloatSwtParameter fakeUploadSpeedRatioSValueRatioReachedCtrl = new FloatSwtParameter(gFake, "FakeUploadSpeedRatio SValue RatioReached", (String)null, (String)null, 0.0F, 1.0E7F, true, 3, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(isRatioCtrl.getMainControl());
         formData.left = new FormAttachment(lValueSpeedRatioRMaxSpef);
         fakeUploadSpeedRatioSValueRatioReachedCtrl.setLayoutData(formData);
         this.ds_parameters.put("FakeUploadSpeedRatio SValue RatioReached", fakeUploadSpeedRatioSValueRatioReachedCtrl);
         Label lValueSpeedRatioSRReachedSpef = new Label(gFake, 0);
         Messages.setLanguageText(lValueSpeedRatioSRReachedSpef, "ConfigView.label.fakeuploadspeedratiorrstxt");
         formData = new FormData();
         formData.top = new FormAttachment(isRatioCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(fakeUploadSpeedRatioSValueRatioReachedCtrl.getMainControl());
         lValueSpeedRatioSRReachedSpef.setLayoutData(formData);
         FloatSwtParameter fakeUploadSpeedRatioSValueMaxRatioReachedCtrl = new FloatSwtParameter(gFake, "FakeUploadSpeedRatio SValueMax RatioReached", (String)null, (String)null, 0.0F, 1.0E7F, true, 3, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(isRatioCtrl.getMainControl());
         formData.left = new FormAttachment(lValueSpeedRatioSRReachedSpef);
         fakeUploadSpeedRatioSValueMaxRatioReachedCtrl.setLayoutData(formData);
         this.ds_parameters.put("FakeUploadSpeedRatio SValueMax RatioReached", fakeUploadSpeedRatioSValueMaxRatioReachedCtrl);
         Label lValueSpeedRatioSRReachedMaxSpef = new Label(gFake, 0);
         Messages.setLanguageText(lValueSpeedRatioSRReachedMaxSpef, "ConfigView.label.fakeuploadspeedratiorrs2txt");
         formData = new FormData();
         formData.top = new FormAttachment(isRatioCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(fakeUploadSpeedRatioSValueMaxRatioReachedCtrl.getMainControl());
         lValueSpeedRatioSRReachedMaxSpef.setLayoutData(formData);
         Label ltxtSpeedRatio2 = new Label(gFake, 0);
         Messages.setLanguageText(ltxtSpeedRatio2, "ConfigView.label.fakeuploadspeedratiostxt1");
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadSpeedRatioRValueCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(0, 18);
         ltxtSpeedRatio2.setLayoutData(formData);
         FloatSwtParameter fakeUploadSpeedRatioSValueCtrl = new FloatSwtParameter(gFake, "FakeUploadSpeedRatio SValue", (String)null, (String)null, 0.0F, 1.0E7F, true, 3, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadSpeedRatioRValueCtrl.getMainControl());
         formData.left = new FormAttachment(ltxtSpeedRatio2);
         fakeUploadSpeedRatioSValueCtrl.setLayoutData(formData);
         this.ds_parameters.put("FakeUploadSpeedRatio SValue", fakeUploadSpeedRatioSValueCtrl);
         Label lValueSpeedRatioSSpef = new Label(gFake, 0);
         Messages.setLanguageText(lValueSpeedRatioSSpef, "ConfigView.label.fakeuploadspeedratiostxt");
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadSpeedRatioRValueCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(fakeUploadSpeedRatioSValueCtrl.getMainControl());
         lValueSpeedRatioSSpef.setLayoutData(formData);
         FloatSwtParameter fakeUploadSpeedRatioSValueMaxCtrl = new FloatSwtParameter(gFake, "FakeUploadSpeedRatio SValueMax", (String)null, (String)null, 0.0F, 1.0E7F, true, 3, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadSpeedRatioRValueCtrl.getMainControl());
         formData.left = new FormAttachment(lValueSpeedRatioSSpef);
         fakeUploadSpeedRatioSValueMaxCtrl.setLayoutData(formData);
         this.ds_parameters.put("FakeUploadSpeedRatio SValueMax", fakeUploadSpeedRatioSValueMaxCtrl);
         Label lValueSpeedRatioSMaxSpef = new Label(gFake, 0);
         Messages.setLanguageText(lValueSpeedRatioSMaxSpef, "ConfigView.label.fakeuploadspeedratiomaxstxt");
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadSpeedRatioRValueCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(fakeUploadSpeedRatioSValueMaxCtrl.getMainControl());
         lValueSpeedRatioSMaxSpef.setLayoutData(formData);
         final BooleanSwtParameter fakeUploadMultiplierCtrl = new BooleanSwtParameter(gFake, "Fake UploadMultiplier", (String)null, (String)null, this.ds_boolparam_adapter);
         Messages.setLanguageText(fakeUploadMultiplierCtrl.getMainControl(), "ConfigView.label.fakeuploadmultiplier");
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadSpeedRatioSValueMaxCtrl.getMainControl(), 10);
         fakeUploadMultiplierCtrl.setLayoutData(formData);
         this.ds_parameters.put("Fake UploadMultiplier", fakeUploadMultiplierCtrl);
         Label ltxtMultiplier = new Label(gFake, 0);
         Messages.setLanguageText(ltxtMultiplier, "ConfigView.label.fakeuploadmultipliertxt1");
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadMultiplierCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(0, 18);
         ltxtMultiplier.setLayoutData(formData);
         FloatSwtParameter fakeUploadMultiplierValueCtrl = new FloatSwtParameter(gFake, "FakeUploadMultiplier Value", (String)null, (String)null, 0.0F, 50.0F, true, 4, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadMultiplierCtrl.getMainControl());
         formData.left = new FormAttachment(ltxtMultiplier);
         fakeUploadMultiplierValueCtrl.setLayoutData(formData);
         this.ds_parameters.put("FakeUploadMultiplier Value", fakeUploadMultiplierValueCtrl);
         Label lValueMultiplierSpef = new Label(gFake, 0);
         Messages.setLanguageText(lValueMultiplierSpef, "ConfigView.label.fakeuploadmultipliertxt");
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadMultiplierCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(fakeUploadMultiplierValueCtrl.getMainControl());
         lValueMultiplierSpef.setLayoutData(formData);
         FloatSwtParameter fakeUploadMultiplierValueMaxCtrl = new FloatSwtParameter(gFake, "FakeUploadMultiplier ValueMax", (String)null, (String)null, 0.0F, 50.0F, true, 4, this.ds_floatparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadMultiplierCtrl.getMainControl());
         formData.left = new FormAttachment(lValueMultiplierSpef);
         fakeUploadMultiplierValueMaxCtrl.setLayoutData(formData);
         this.ds_parameters.put("FakeUploadMultiplier ValueMax", fakeUploadMultiplierValueMaxCtrl);
         Label lValueMultiplierMaxSpef = new Label(gFake, 0);
         Messages.setLanguageText(lValueMultiplierMaxSpef, "ConfigView.label.fakeuploadmultipliermaxtxt");
         formData = new FormData();
         formData.top = new FormAttachment(fakeUploadMultiplierCtrl.getMainControl(), 5);
         formData.left = new FormAttachment(fakeUploadMultiplierValueMaxCtrl.getMainControl());
         lValueMultiplierMaxSpef.setLayoutData(formData);
         final BooleanSwtParameter showAsSeedCtrl2 = new BooleanSwtParameter(gFake, "Show As Seed", "ConfigView.label.showasseed", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(lValueMultiplierMaxSpef, 3);
         formData.left = new FormAttachment(0, 18);
         showAsSeedCtrl2.setLayoutData(formData);
         this.ds_parameters.put("Show As Seed-DUP", showAsSeedCtrl2);
         final BooleanSwtParameter ghostleech = new BooleanSwtParameter(gFake, "GhostLeech", (String)null, (String)null, this.ds_boolparam_adapter);
         Messages.setLanguageText(ghostleech.getMainControl(), "ConfigView.label.ghostleechtitle");
         formData = new FormData();
         formData.top = new FormAttachment(showAsSeedCtrl2.getMainControl(), 10);
         ghostleech.setLayoutData(formData);
         this.ds_parameters.put("GhostLeech", ghostleech);
         final BooleanSwtParameter ghostLeechStart = new BooleanSwtParameter(gFake, "GhostLeechStart", "ConfigView.label.ghostleechstart", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ghostleech.getMainControl(), 3);
         formData.left = new FormAttachment(0, 18);
         ghostLeechStart.setLayoutData(formData);
         this.ds_parameters.put("GhostLeechStart", ghostLeechStart);
         final BooleanSwtParameter ghostLeechStartStop = new BooleanSwtParameter(gFake, "GhostLeechStartStop", "ConfigView.label.ghostleechstartstop", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ghostLeechStart.getMainControl(), 3);
         formData.left = new FormAttachment(0, 18);
         ghostLeechStartStop.setLayoutData(formData);
         this.ds_parameters.put("GhostLeechStartStop", ghostLeechStartStop);
         final BooleanSwtParameter ghostLeechStopTime = new BooleanSwtParameter(gFake, "GhostLeechStopTime", "ConfigView.label.ghostleechstoptime", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ghostLeechStartStop.getMainControl(), 3);
         formData.left = new FormAttachment(0, 18);
         ghostLeechStopTime.setLayoutData(formData);
         this.ds_parameters.put("GhostLeechStopTime", ghostLeechStopTime);
         IntSwtParameter ghostLeechStopTimeValue = new IntSwtParameter(gFake, "GhostLeechStopTimeValue", (String)null, (String)null, 20, 1500, this.ds_intparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ghostLeechStartStop.getMainControl(), 2);
         formData.left = new FormAttachment(ghostLeechStopTime.getMainControl());
         ghostLeechStopTimeValue.setLayoutData(formData);
         this.ds_parameters.put("GhostLeechStopTimeValue", ghostLeechStopTimeValue);
         Label ghostLeechStopTimeTxt = new Label(gFake, 0);
         Messages.setLanguageText(ghostLeechStopTimeTxt, "ConfigView.label.ghostleechstoptime2");
         formData = new FormData();
         formData.top = new FormAttachment(ghostLeechStartStop.getMainControl(), 4);
         formData.left = new FormAttachment(ghostLeechStopTimeValue.getMainControl(), 2);
         ghostLeechStopTimeTxt.setLayoutData(formData);
         final BooleanSwtParameter ghostLeechNoReport = new BooleanSwtParameter(gFake, "GhostLeechNoReport", "ConfigView.label.ghostleechnoreport", (String)null, this.ds_boolparam_adapter);
         formData = new FormData();
         formData.top = new FormAttachment(ghostLeechStopTimeTxt, 3);
         formData.left = new FormAttachment(0, 36);
         ghostLeechNoReport.setLayoutData(formData);
         this.ds_parameters.put("GhostLeechNoReport", ghostLeechNoReport);
         Label ltxtDonation = new Label(this.cShu, 0);
         Messages.setLanguageText(ltxtDonation, "ConfigView.label.donationshu");
         formData = new FormData();
         formData.top = new FormAttachment(gFake, 10);
         formData.left = new FormAttachment(0, 10);
         ltxtDonation.setLayoutData(formData);
         Label tmp = new Label(this.cShu, 0);
         formData = new FormData();
         formData.top = new FormAttachment(ltxtDonation);
         tmp.setLayoutData(formData);
         downloadReducValueCtrl.addChangeListener((p) -> {
            int psval = (Integer)downloadReducValueCtrl.getValue();
            if (psval < 0) {
               downloadReducValueCtrl.setValue(0);
            }

            if (psval > 1000) {
               downloadReducValueCtrl.setValue(1000);
            }

         });
         peerFakeValue.addChangeListener((p) -> {
            int psval = (Integer)peerFakeValue.getValue();
            if (psval < 0) {
               peerFakeValue.setValue(0);
            }

            if (psval > 250) {
               peerFakeValue.setValue(250);
            }

         });
         peerSeedRatioValue.addChangeListener((p) -> {
            int psval = (Integer)peerSeedRatioValue.getValue();
            if (psval < 0) {
               peerSeedRatioValue.setValue(0);
            }

            if (psval > 250) {
               peerSeedRatioValue.setValue(250);
            }

         });
         final Control[] tabSafeFake = new Control[]{safeFakeUploadValue.getMainControl(), ltxtSafe, ltxtSafe2};
         final SwtParameter[] tabSafeFakeParam = new SwtParameter[]{safeFakeUploadValue};
         safeFakeUploadCtrl.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(tabSafeFakeParam));
         safeFakeUploadCtrl.setAdditionalActionPerformer(new ChangeSelectionControlsActionPerformer(tabSafeFake));
         final Control[] tabDLReducCtrl = new Control[]{downloadReducAloneCtrl.getMainControl(), downloadReducMixCtrl.getMainControl(), ltxtDLReduc, lValueDLReducSpef, downloadReducValueCtrl.getMainControl()};
         final SwtParameter[] tabDLReducParam = new SwtParameter[]{downloadReducAloneCtrl, downloadReducMixCtrl, downloadReducValueCtrl};
         downloadReducCtrl.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(tabDLReducParam));
         downloadReducCtrl.setAdditionalActionPerformer(new ChangeSelectionControlsActionPerformer(tabDLReducCtrl));
         final Control[] tabNoRepCtrl = new Control[]{noReportLeechCtrl.getMainControl(), noReportSeedCtrl.getMainControl(), noReportCustom.getMainControl(), noReportCustomValue.getMainControl(), noRepCusAdditional, noReportAuto.getMainControl()};
         final SwtParameter[] tabNoRepParam = new SwtParameter[]{noReportLeechCtrl, noReportSeedCtrl, noReportCustom, noReportCustomValue, noReportAuto};
         noReportCtrl.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(tabNoRepParam));
         noReportCtrl.setAdditionalActionPerformer(new ChangeSelectionControlsActionPerformer(tabNoRepCtrl));
         final Control[] tabRmCtrl = new Control[]{ltxtRatio, lValueRMSpef, fakeUploadRatioValueCtrl.getMainControl(), lValueRMMaxSpef, fakeUploadRatioValueMaxCtrl.getMainControl(), fakeUploadRMIntelligentCtrl.getMainControl(), fakeUploadRatioContinueCtrl.getMainControl()};
         final SwtParameter[] tabRmParam = new SwtParameter[]{fakeUploadRatioValueCtrl, fakeUploadRatioValueMaxCtrl, fakeUploadRMIntelligentCtrl, fakeUploadRatioContinueCtrl};
         fakeUploadRatioCtrl.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(tabRmParam));
         fakeUploadRatioCtrl.setAdditionalActionPerformer(new ChangeSelectionControlsActionPerformer(tabRmCtrl));
         final Control[] tabIsRatioCtrl = new Control[]{ltxtSpeedRatio1, lValueSpeedRatioRSpef, fakeUploadSpeedRatioRValueCtrl.getMainControl(), lValueSpeedRatioRMaxSpef, fakeUploadSpeedRatioRValueMaxCtrl.getMainControl(), fakeUploadSpeedRatioSValueRatioReachedCtrl.getMainControl(), lValueSpeedRatioSRReachedSpef, fakeUploadSpeedRatioSValueMaxRatioReachedCtrl.getMainControl(), lValueSpeedRatioSRReachedMaxSpef};
         final SwtParameter[] tabIsRatioParam = new SwtParameter[]{fakeUploadSpeedRatioRValueCtrl, fakeUploadSpeedRatioRValueMaxCtrl, fakeUploadSpeedRatioSValueRatioReachedCtrl, fakeUploadSpeedRatioSValueMaxRatioReachedCtrl};
         final Control[] tabIsRatioSwarmCtrl = new Control[]{ltxtSpeedRatio1, lValueSpeedRatioRSpef, fakeUploadSpeedRatioRValueCtrl.getMainControl(), lValueSpeedRatioRMaxSpef, fakeUploadSpeedRatioRValueMaxCtrl.getMainControl(), fakeUploadSpeedRatioSValueRatioReachedCtrl.getMainControl(), lValueSpeedRatioSRReachedSpef};
         final SwtParameter[] tabIsRatioSwarmParam = new SwtParameter[]{fakeUploadSpeedRatioRValueCtrl, fakeUploadSpeedRatioRValueMaxCtrl, fakeUploadSpeedRatioSValueRatioReachedCtrl};
         isRatioCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  for(SwtParameter param : tabIsRatioParam) {
                     param.setEnabled(isRatioCtrl.isSelected());
                  }

                  for(int i = 0; i < tabIsRatioCtrl.length; ++i) {
                     tabIsRatioCtrl[i].setEnabled(isRatioCtrl.isSelected());
                  }

                  if (isRatioCtrl.isSelected() && swarmSpeedCtrl.isSelected()) {
                     for(SwtParameter param : tabIsRatioSwarmParam) {
                        param.setEnabled(false);
                     }

                     for(int i = 0; i < tabIsRatioSwarmCtrl.length; ++i) {
                        tabIsRatioSwarmCtrl[i].setEnabled(false);
                     }
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         final Control[] tabMultiCtrl = new Control[]{ltxtMultiplier, lValueMultiplierSpef, fakeUploadMultiplierValueCtrl.getMainControl(), lValueMultiplierMaxSpef, fakeUploadMultiplierValueMaxCtrl.getMainControl(), showAsSeedCtrl2.getMainControl()};
         final SwtParameter[] tabMultiParam = new SwtParameter[]{fakeUploadMultiplierValueCtrl, fakeUploadMultiplierValueMaxCtrl, showAsSeedCtrl2};
         fakeUploadMultiplierCtrl.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(tabMultiParam));
         fakeUploadMultiplierCtrl.setAdditionalActionPerformer(new ChangeSelectionControlsActionPerformer(tabMultiCtrl));
         final Control[] tabSwarmCtrl = new Control[]{ltxtSpeedRatio2, lValueSpeedRatioSSpef, fakeUploadSpeedRatioSValueCtrl.getMainControl()};
         final SwtParameter[] tabSwarmParam = new SwtParameter[]{fakeUploadSpeedRatioSValueCtrl};
         final Control[] tabSpeedRatioCtrl = new Control[]{startLabel, startFakeCtrl.getMainControl(), startFakeValue.getMainControl(), fakeUploadAddCtrl.getMainControl(), fakeUploadSRIntelligentCtrl.getMainControl(), showAsSeedCtrl.getMainControl(), swarmSpeedCtrl.getMainControl(), stopIfSwarmDrops.getMainControl(), isRatioCtrl.getMainControl(), ltxtSpeedRatio2, lValueSpeedRatioSSpef, fakeUploadSpeedRatioSValueCtrl.getMainControl(), lValueSpeedRatioSMaxSpef, fakeUploadSpeedRatioSValueMaxCtrl.getMainControl(), fakeUploadSpeedRatioSValueMaxRatioReachedCtrl.getMainControl(), lValueSpeedRatioSRReachedMaxSpef, lValueSpeedRatioSRReachedSpef, fakeUploadSpeedRatioSValueRatioReachedCtrl.getMainControl(), fakeUploadSpeedRatioRValueMaxCtrl.getMainControl(), lValueSpeedRatioRMaxSpef, fakeUploadSpeedRatioRValueCtrl.getMainControl(), ltxtSpeedRatio1, lValueSpeedRatioRSpef};
         final SwtParameter[] tabSpeedRatioParam = new SwtParameter[]{startFakeCtrl, startFakeValue, fakeUploadAddCtrl, fakeUploadSRIntelligentCtrl, showAsSeedCtrl, swarmSpeedCtrl, stopIfSwarmDrops, isRatioCtrl, fakeUploadSpeedRatioSValueCtrl, fakeUploadSpeedRatioSValueMaxCtrl, fakeUploadSpeedRatioSValueMaxRatioReachedCtrl, fakeUploadSpeedRatioSValueRatioReachedCtrl, fakeUploadSpeedRatioRValueMaxCtrl, fakeUploadSpeedRatioRValueCtrl};
         Control[] tabFakeCtrl = new Control[]{swarmPeerPoolCtrl.getMainControl(), ltxtPeer, peerFakeValue.getMainControl(), ltxt2Peer, peerSeedRatioValue.getMainControl(), ltxt3Seed, safeFakeUploadCtrl.getMainControl(), downloadReducCtrl.getMainControl(), noReportCtrl.getMainControl(), fakeUploadRatioCtrl.getMainControl(), fakeUploadSpeedRatioCtrl.getMainControl(), fakeUploadMultiplierCtrl.getMainControl(), ghostleech.getMainControl()};
         SwtParameter[] tabFakeParam = new SwtParameter[]{swarmPeerPoolCtrl, peerFakeValue, peerSeedRatioValue, safeFakeUploadCtrl, downloadReducCtrl, noReportCtrl, fakeUploadRatioCtrl, fakeUploadSpeedRatioCtrl, fakeUploadMultiplierCtrl, ghostleech};
         enableFakeCtrl.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(tabFakeParam));
         enableFakeCtrl.setAdditionalActionPerformer(new ChangeSelectionControlsActionPerformer(tabFakeCtrl));
         final Control[] tabGhostLeech = new Control[]{ghostLeechNoReport.getMainControl(), ghostLeechStart.getMainControl(), ghostLeechStartStop.getMainControl(), ghostLeechStopTime.getMainControl(), ghostLeechStopTimeTxt, ghostLeechStopTimeValue.getMainControl()};
         final SwtParameter[] tabGhostLeechParam = new SwtParameter[]{ghostLeechNoReport, ghostLeechStart, ghostLeechStartStop, ghostLeechStopTime, ghostLeechStopTimeValue};
         ghostleech.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(tabGhostLeechParam));
         ghostleech.setAdditionalActionPerformer(new ChangeSelectionControlsActionPerformer(tabGhostLeech));
         final IAdditionalActionPerformer<Boolean> ratioToolUploadSpeed1Performer = new IAdditionalActionPerformer<Boolean>() {
            public void performAction() {
               Utils.execSWTThread(() -> {
                  boolean b = true;
                  if (!ratioToolUploadSpeed1.isSelected()) {
                     b = false;
                     ratioToolStopUpload1.setSelected(b);
                  }

                  ratioToolStopUpload1.setEnabled(b);
                  ratioToolStopUpload2.setEnabled(b);
                  ratioToolStopUploadValue.setEnabled(b);
               });
            }

            public void valueChanged(Boolean newValue) {
            }
         };
         final IAdditionalActionPerformer<Boolean> ratioToolDownloadSpeed1Performer = new IAdditionalActionPerformer<Boolean>() {
            public void performAction() {
               Utils.execSWTThread(() -> {
                  boolean b = true;
                  if (!ratioToolDownloadSpeed1.isSelected()) {
                     b = false;
                     ratioToolStopDownload1.setSelected(b);
                     ratioToolFakeIntelligent.setSelected(b);
                     ratioToolStartSlow1.setSelected(b);
                  }

                  ratioToolStopDownload1.setEnabled(b);
                  ratioToolStopDownload2.setEnabled(b);
                  ratioToolStopDownloadValue.setEnabled(b);
                  ratioToolFakeIntelligent.setEnabled(b);
                  ratioToolStartSlow1.setEnabled(b);
                  ratioToolStartSlow2.setEnabled(b);
                  ratioToolStartSlow1Value.setEnabled(b);
               });
            }

            public void valueChanged(Boolean newValue) {
            }
         };
         final IAdditionalActionPerformer<Boolean> ratioToolStopUpload1Performer = new IAdditionalActionPerformer<Boolean>() {
            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (ratioToolStopUpload1.isSelected()) {
                     ratioToolStopDownload1.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
            }
         };
         final IAdditionalActionPerformer<Boolean> ratioToolStopDownload1Performer = new IAdditionalActionPerformer<Boolean>() {
            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (ratioToolStopDownload1.isSelected()) {
                     ratioToolStopUpload1.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
            }
         };
         ratioToolUploadSpeed1.setAdditionalActionPerformer(ratioToolUploadSpeed1Performer);
         ratioToolDownloadSpeed1.setAdditionalActionPerformer(ratioToolDownloadSpeed1Performer);
         ratioToolStopUpload1.setAdditionalActionPerformer(ratioToolStopUpload1Performer);
         ratioToolStopDownload1.setAdditionalActionPerformer(ratioToolStopDownload1Performer);
         final Control[] ratioToolControl = new Control[]{ratioToolUploadSpeed1.getMainControl(), ratioToolUploadSpeedMin.getMainControl(), ratioToolUploadSpeed2, ratioToolUploadSpeedMax.getMainControl(), ratioToolUploadSpeed3, ratioToolDownloadSpeed1.getMainControl(), ratioToolDownloadSpeedMin.getMainControl(), ratioToolDownloadSpeed2, ratioToolDownloadSpeedMax.getMainControl(), ratioToolDownloadSpeed3, ratioToolStopPeers.getMainControl(), ratioToolStopPeersValue.getMainControl(), ratioToolFakeIntelligent.getMainControl(), ratioToolAddStopped.getMainControl(), ratioToolPercentDone1, ratioToolPercentDoneValue.getMainControl(), ratioToolPercentDone2};
         ratioTool.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            public void performAction() {
               Utils.execSWTThread(() -> {
                  boolean b = ratioTool.isSelected();

                  for(Control c : ratioToolControl) {
                     c.setEnabled(b);
                  }

                  if (!b) {
                     ratioToolUploadSpeed1.setSelected(false);
                     ratioToolDownloadSpeed1.setSelected(false);
                     ratioToolStopPeers.setSelected(false);
                     ratioToolAddStopped.setSelected(false);
                  }

                  ratioToolUploadSpeed1Performer.performAction();
                  ratioToolDownloadSpeed1Performer.performAction();
                  ratioToolStopUpload1Performer.performAction();
                  ratioToolStopDownload1Performer.performAction();
               });
            }

            public void valueChanged(Boolean newValue) {
            }
         });
         Control[] tabNoUpADLCtrl = new Control[]{noUpAfterDLRemoveCtrl.getMainControl(), noUpAfterDLRemoveTorrentCtrl.getMainControl()};
         SwtParameter[] tabNoUpADLParam = new SwtParameter[]{noUpAfterDLRemoveCtrl, noUpAfterDLRemoveTorrentCtrl};
         noUpAfterDLCtrl.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(tabNoUpADLParam));
         noUpAfterDLCtrl.setAdditionalActionPerformer(new ChangeSelectionControlsActionPerformer(tabNoUpADLCtrl));
         noUpAfterDLCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (!this.checked) {
                     noUpAfterDLRemoveCtrl.setSelected(false);
                     noUpAfterDLRemoveTorrentCtrl.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         noUploadCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     boolean fakeUploadMultiplier = fakeUploadMultiplierCtrl.isSelected();
                     boolean downloadReducAlone = downloadReducAloneCtrl.isSelected();
                     if (fakeUploadMultiplier || downloadReducAlone) {
                        noUploadCtrl.setSelected(false);
                     }

                     boolean fakeUploadRMContinue = fakeUploadRatioContinueCtrl.isSelected();
                     if (fakeUploadRMContinue) {
                        fakeUploadRatioContinueCtrl.setSelected(false);
                     }
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         stopAfterXHoursCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               if (!this.checked) {
                  stopAfterXHoursCtrl.setSelected(false);
               }

            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         ratioTool.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     if (enableFakeCtrl.isSelected()) {
                        enableFakeCtrl.setSelected(false);
                     }

                     if (fakeOffFakeAddedToRealCtrl.isSelected()) {
                        fakeOffFakeAddedToRealCtrl.setSelected(false);
                     }
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         fakeUploadSpeedRatioCtrl.setAdditionalActionPerformer(new ChangeSelectionControlsActionPerformer(tabSpeedRatioCtrl));
         fakeUploadSpeedRatioCtrl.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(tabSpeedRatioParam));
         swarmSpeedCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (fakeUploadSpeedRatioCtrl.isSelected()) {
                     for(SwtParameter param : tabSwarmParam) {
                        param.setEnabled(!this.checked);
                     }

                     for(int i = 0; i < tabSwarmCtrl.length; ++i) {
                        tabSwarmCtrl[i].setEnabled(!this.checked);
                     }

                     if (isRatioCtrl.isSelected()) {
                        for(SwtParameter param : tabIsRatioSwarmParam) {
                           param.setEnabled(!this.checked);
                        }

                        for(int i = 0; i < tabIsRatioSwarmCtrl.length; ++i) {
                           tabIsRatioSwarmCtrl[i].setEnabled(!this.checked);
                        }
                     }
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         enableFakeCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (!this.checked) {
                     for(SwtParameter param : tabSafeFakeParam) {
                        param.setEnabled(false);
                     }

                     for(int i = 0; i < tabSafeFake.length; ++i) {
                        tabSafeFake[i].setEnabled(false);
                     }

                     for(SwtParameter param : tabDLReducParam) {
                        param.setEnabled(false);
                     }

                     for(int i = 0; i < tabDLReducCtrl.length; ++i) {
                        tabDLReducCtrl[i].setEnabled(false);
                     }

                     for(SwtParameter param : tabNoRepParam) {
                        param.setEnabled(false);
                     }

                     for(int i = 0; i < tabNoRepCtrl.length; ++i) {
                        tabNoRepCtrl[i].setEnabled(false);
                     }

                     for(SwtParameter param : tabRmParam) {
                        param.setEnabled(false);
                     }

                     for(int i = 0; i < tabRmCtrl.length; ++i) {
                        tabRmCtrl[i].setEnabled(false);
                     }

                     for(SwtParameter param : tabSpeedRatioParam) {
                        param.setEnabled(false);
                     }

                     for(int i = 0; i < tabSpeedRatioCtrl.length; ++i) {
                        tabSpeedRatioCtrl[i].setEnabled(false);
                     }

                     for(SwtParameter param : tabMultiParam) {
                        param.setEnabled(false);
                     }

                     for(int i = 0; i < tabMultiCtrl.length; ++i) {
                        tabMultiCtrl[i].setEnabled(false);
                     }

                     for(SwtParameter param : tabGhostLeechParam) {
                        param.setEnabled(false);
                     }

                     for(int i = 0; i < tabGhostLeech.length; ++i) {
                        tabGhostLeech[i].setEnabled(false);
                     }

                     safeFakeUploadCtrl.setSelected(false);
                     downloadReducCtrl.setSelected(false);
                     noReportCtrl.setSelected(false);
                     fakeUploadRatioCtrl.setSelected(false);
                     fakeUploadSpeedRatioCtrl.setSelected(false);
                     fakeUploadMultiplierCtrl.setSelected(false);
                     ghostleech.setSelected(false);
                     showAsSeedCtrl.setSelected(false);
                     showAsSeedCtrl2.setSelected(false);
                  } else if (ratioTool.isSelected()) {
                     ratioTool.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         downloadReducCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     boolean noReport = noReportCtrl.isSelected();
                     boolean fakeUploadRatio = fakeUploadRatioCtrl.isSelected();
                     boolean fakeUploadSpeedRatio = fakeUploadSpeedRatioCtrl.isSelected();
                     boolean fakeUploadMulti = fakeUploadMultiplierCtrl.isSelected();
                     downloadReducAloneCtrl.setSelected(true);
                     boolean ghostLeech = ghostleech.isSelected();
                     if (noReport) {
                        for(SwtParameter param : tabNoRepParam) {
                           param.setEnabled(false);
                        }

                        for(int i = 0; i < tabNoRepCtrl.length; ++i) {
                           tabNoRepCtrl[i].setEnabled(false);
                        }

                        downloadReducMixCtrl.setSelected(false);
                        noReportCtrl.setSelected(false);
                        noReportSeedCtrl.setSelected(false);
                        noReportLeechCtrl.setSelected(false);
                        noReportCustom.setSelected(false);
                        noReportAuto.setSelected(false);
                     }

                     if (fakeUploadMulti || fakeUploadRatio || fakeUploadSpeedRatio) {
                        downloadReducAloneCtrl.setSelected(false);
                        downloadReducMixCtrl.setSelected(true);
                     }

                     if (ghostLeech) {
                        for(SwtParameter param : tabGhostLeechParam) {
                           param.setEnabled(false);
                        }

                        for(int i = 0; i < tabGhostLeech.length; ++i) {
                           tabGhostLeech[i].setEnabled(false);
                        }

                        downloadReducMixCtrl.setSelected(false);
                        ghostleech.setSelected(false);
                        ghostLeechStart.setSelected(false);
                        ghostLeechStartStop.setSelected(false);
                        ghostLeechStopTime.setSelected(false);
                        ghostLeechNoReport.setSelected(false);
                     }
                  } else {
                     downloadReducAloneCtrl.setSelected(false);
                     downloadReducMixCtrl.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         downloadReducAloneCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     boolean noupload = noUploadCtrl.isSelected();
                     if (noupload) {
                        noUploadCtrl.setSelected(false);
                     }

                     downloadReducMixCtrl.setSelected(false);
                     downloadReducAloneCtrl.setSelected(true);
                     noReportCtrl.setSelected(false);
                     fakeUploadRatioCtrl.setSelected(false);
                     fakeUploadSpeedRatioCtrl.setSelected(false);
                     fakeUploadMultiplierCtrl.setSelected(false);
                     ghostleech.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         downloadReducMixCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     downloadReducAloneCtrl.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         noReportCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     boolean downred = downloadReducCtrl.isSelected();
                     if (downred) {
                        for(SwtParameter param : tabDLReducParam) {
                           param.setEnabled(false);
                        }

                        for(int i = 0; i < tabDLReducCtrl.length; ++i) {
                           tabDLReducCtrl[i].setEnabled(false);
                        }

                        downloadReducCtrl.setSelected(false);
                        downloadReducAloneCtrl.setSelected(false);
                        downloadReducMixCtrl.setSelected(false);
                     }

                     fakeUploadRatioCtrl.setSelected(false);
                     fakeUploadSpeedRatioCtrl.setSelected(false);
                     fakeUploadMultiplierCtrl.setSelected(false);
                     ghostleech.setSelected(false);
                  } else {
                     noReportLeechCtrl.setSelected(false);
                     noReportSeedCtrl.setSelected(false);
                     noReportCustom.setSelected(false);
                     noReportAuto.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         noReportLeechCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     noReportSeedCtrl.setSelected(false);
                     noReportCustom.setSelected(false);
                     noReportAuto.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         noReportSeedCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     noReportLeechCtrl.setSelected(false);
                     noReportCustom.setSelected(false);
                     noReportAuto.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         noReportCustom.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     noReportLeechCtrl.setSelected(false);
                     noReportSeedCtrl.setSelected(false);
                     noReportAuto.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         noReportAuto.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     noReportLeechCtrl.setSelected(false);
                     noReportSeedCtrl.setSelected(false);
                     noReportCustom.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         fakeUploadRatioCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     boolean downloadReducAlone = downloadReducAloneCtrl.isSelected();
                     if (downloadReducAlone) {
                        for(SwtParameter param : tabDLReducParam) {
                           param.setEnabled(false);
                        }

                        for(int i = 0; i < tabDLReducCtrl.length; ++i) {
                           tabDLReducCtrl[i].setEnabled(false);
                        }

                        downloadReducCtrl.setSelected(false);
                        downloadReducAloneCtrl.setSelected(false);
                     }

                     noReportCtrl.setSelected(false);
                     fakeUploadSpeedRatioCtrl.setSelected(false);
                     fakeUploadMultiplierCtrl.setSelected(false);
                     ghostleech.setSelected(false);
                  } else {
                     fakeUploadRMIntelligentCtrl.setSelected(false);
                     fakeUploadRatioContinueCtrl.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         fakeUploadRatioContinueCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     boolean noupload = noUploadCtrl.isSelected();
                     if (noupload) {
                        noUploadCtrl.setSelected(false);
                     }
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         startFakeCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     boolean showasseed = showAsSeedCtrl.isSelected();
                     if (showasseed) {
                        showAsSeedCtrl.setSelected(false);
                        showAsSeedCtrl2.setSelected(false);
                     }
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         showAsSeedCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  showAsSeedCtrl2.setSelected(this.checked);
                  if (this.checked) {
                     boolean downreducmix = downloadReducMixCtrl.isSelected();
                     if (downreducmix) {
                        downloadReducMixCtrl.setSelected(false);
                        downloadReducCtrl.setSelected(false);
                     }

                     boolean startFake = startFakeCtrl.isSelected();
                     if (startFake) {
                        startFakeCtrl.setSelected(false);
                     }
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         showAsSeedCtrl2.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  showAsSeedCtrl.setSelected(this.checked);
                  if (this.checked) {
                     boolean downreducmix = downloadReducMixCtrl.isSelected();
                     if (downreducmix) {
                        downloadReducMixCtrl.setSelected(false);
                        downloadReducCtrl.setSelected(false);
                     }
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         fakeUploadSpeedRatioCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     boolean downloadReducAlone = downloadReducAloneCtrl.isSelected();
                     if (downloadReducAlone) {
                        for(SwtParameter param : tabDLReducParam) {
                           param.setEnabled(false);
                        }

                        for(int i = 0; i < tabDLReducCtrl.length; ++i) {
                           tabDLReducCtrl[i].setEnabled(false);
                        }

                        downloadReducCtrl.setSelected(false);
                        downloadReducAloneCtrl.setSelected(false);
                     }

                     noReportCtrl.setSelected(false);
                     fakeUploadRatioCtrl.setSelected(false);
                     fakeUploadMultiplierCtrl.setSelected(false);
                     ghostleech.setSelected(false);
                  } else {
                     fakeUploadAddCtrl.setSelected(false);
                     isRatioCtrl.setSelected(false);
                     swarmSpeedCtrl.setSelected(false);
                     stopIfSwarmDrops.setSelected(false);
                     fakeUploadSRIntelligentCtrl.setSelected(false);
                     if (!fakeUploadMultiplierCtrl.isSelected()) {
                        showAsSeedCtrl.setSelected(false);
                        showAsSeedCtrl2.setSelected(false);
                     }
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         fakeUploadMultiplierCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (!this.checked && !fakeUploadSpeedRatioCtrl.isSelected()) {
                     showAsSeedCtrl.setSelected(false);
                     showAsSeedCtrl2.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         fakeUploadAddCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     boolean noupload = noUploadCtrl.isSelected();
                     if (noupload) {
                        noUploadCtrl.setSelected(false);
                     }
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         fakeUploadMultiplierCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     boolean noupload = noUploadCtrl.isSelected();
                     if (noupload) {
                        noUploadCtrl.setSelected(false);
                     }

                     boolean downloadReducAlone = downloadReducAloneCtrl.isSelected();
                     if (downloadReducAlone) {
                        for(SwtParameter param : tabDLReducParam) {
                           param.setEnabled(false);
                        }

                        for(int i = 0; i < tabDLReducCtrl.length; ++i) {
                           tabDLReducCtrl[i].setEnabled(false);
                        }

                        downloadReducCtrl.setSelected(false);
                        downloadReducAloneCtrl.setSelected(false);
                     }

                     noReportCtrl.setSelected(false);
                     fakeUploadRatioCtrl.setSelected(false);
                     fakeUploadSpeedRatioCtrl.setSelected(false);
                     ghostleech.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         ghostleech.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     boolean downred = downloadReducCtrl.isSelected();
                     if (downred) {
                        for(SwtParameter param : tabDLReducParam) {
                           param.setEnabled(false);
                        }

                        for(int i = 0; i < tabDLReducCtrl.length; ++i) {
                           tabDLReducCtrl[i].setEnabled(false);
                        }

                        downloadReducCtrl.setSelected(false);
                        downloadReducAloneCtrl.setSelected(false);
                        downloadReducMixCtrl.setSelected(false);
                     }

                     fakeUploadRatioCtrl.setSelected(false);
                     fakeUploadSpeedRatioCtrl.setSelected(false);
                     fakeUploadMultiplierCtrl.setSelected(false);
                     noReportCtrl.setSelected(false);
                  } else {
                     ghostLeechStart.setSelected(false);
                     ghostLeechStartStop.setSelected(false);
                     ghostLeechStopTime.setSelected(false);
                     ghostLeechNoReport.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         ghostLeechStart.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     ghostLeechStartStop.setSelected(false);
                     ghostLeechStopTime.setSelected(false);
                     ghostLeechNoReport.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         ghostLeechStartStop.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     ghostLeechStart.setSelected(false);
                     ghostLeechStopTime.setSelected(false);
                     ghostLeechNoReport.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         ghostLeechStopTime.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     ghostLeechStart.setSelected(false);
                     ghostLeechStartStop.setSelected(false);
                  } else {
                     ghostLeechNoReport.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         ghostLeechNoReport.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked) {
                     ghostLeechStart.setSelected(false);
                     ghostLeechStartStop.setSelected(false);
                     ghostLeechStopTime.setSelected(true);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         fakeOffFakeAddedToRealCtrl.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
            boolean checked;

            public void performAction() {
               Utils.execSWTThread(() -> {
                  if (this.checked && ratioTool.isSelected()) {
                     ratioTool.setSelected(false);
                  }

               });
            }

            public void valueChanged(Boolean newValue) {
               this.checked = newValue;
            }
         });
         this.cShu.layout();
         Rectangle r = this.scrolled_comp.getClientArea();
         this.scrolled_comp.setMinSize(this.cShu.computeSize(r.width, -1));
      } else {
         ViewUtils.setViewRequiresOneDownload(this.cShu);
      }
   }

   public Composite getComposite() {
      return this.cShu;
   }

   public void delete() {
      Utils.disposeComposite(this.cShu);
   }

   public String getData() {
      return "ShuView.title.short";
   }

   public String getFullTitle() {
      return MessageText.getString("ShuView.title.full");
   }

   public void parameterChanged(final DownloadManagerOptionsHandler manager) {
      Utils.execSWTThread(new Runnable() {
         public void run() {
            for(Map.Entry entry : ShuView.this.ds_parameters.entrySet()) {
               String key = (String)entry.getKey();
               key = key.replace("-DUP", "");
               Object param = entry.getValue();
               if (param instanceof IntSwtParameter) {
                  IntSwtParameter int_param = (IntSwtParameter)param;
                  int value3 = manager.getIntParameter(key);
                  int_param.setValue(value3);
               } else if (param instanceof BooleanSwtParameter) {
                  BooleanSwtParameter bool_param = (BooleanSwtParameter)param;
                  boolean value = manager.getBooleanParameter(key);
                  bool_param.setSelected(value);
               } else if (param instanceof FloatSwtParameter) {
                  FloatSwtParameter float_param = (FloatSwtParameter)param;
                  float value2 = (float)manager.getIntParameter(key) / 1000.0F;
                  float_param.setValue(value2);
               } else {
                  Debug.out("Unknown parameter type: " + String.valueOf(param.getClass()));
               }
            }

         }
      }, true);
   }

   @Override
   public boolean eventOccurred(UISWTViewEvent event) {
      switch (event.getType()) {
         case UISWTViewEvent.TYPE_CREATE:
            this.swtView = (UISWTView) event.getData();
            this.swtView.setTitle(this.getFullTitle());
            break;

         case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
            this.dataSourceChanged(event.getData());
            break;

         case UISWTViewEvent.TYPE_INITIALIZE:
            this.initialize((Composite) event.getData());
            break;

         case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
            Messages.updateLanguageForControl(this.getComposite());
            this.swtView.setTitle(this.getFullTitle());
            break;

         case UISWTViewEvent.TYPE_DESTROY:
            this.delete();
            break;
      }

      return true;
   }

   private static class DMWrapper implements DownloadManagerOptionsHandler, DownloadManagerStateAttributeListener {
      private DownloadManager dm;
      private CopyOnWriteList<ParameterChangeListener> listeners;

      private DMWrapper(DownloadManager _dm) {
         this.listeners = new CopyOnWriteList<>();
         this.dm = _dm;
      }

      public String getName() {
         return this.dm.getDisplayName();
      }

      public void setIntParameter(String name, int value) {
         this.dm.getDownloadState().setIntParameter(name, value);
      }

      public int getIntParameter(String name) {
         return this.dm.getDownloadState().getIntParameter(name);
      }

      public void setBooleanParameter(String name, boolean value) {
         this.dm.getDownloadState().setBooleanParameter(name, value);
      }

      public boolean getBooleanParameter(String name) {
         return this.dm.getDownloadState().getBooleanParameter(name);
      }

      public void setParameterDefault(String key) {
         this.dm.getDownloadState().setParameterDefault(key);
      }

      public int getUploadRateLimitBytesPerSecond() {
         return this.dm.getStats().getUploadRateLimitBytesPerSecond();
      }

      public void setUploadRateLimitBytesPerSecond(int limit) {
         this.dm.getStats().setUploadRateLimitBytesPerSecond(limit);
      }

      public int getDownloadRateLimitBytesPerSecond() {
         return this.dm.getStats().getDownloadRateLimitBytesPerSecond();
      }

      public void setDownloadRateLimitBytesPerSecond(int limit) {
         this.dm.getStats().setDownloadRateLimitBytesPerSecond(limit);
      }

      public DownloadManager getDownloadManager() {
         return this.dm;
      }

      public void attributeEventOccurred(DownloadManager dm, String attribute_name, int event_type) {
         for(DownloadManagerOptionsHandler.ParameterChangeListener l : this.listeners) {
            try {
               l.parameterChanged(this);
            } catch (Throwable e) {
               Debug.out(e);
            }
         }

      }

      public void addListener(DownloadManagerOptionsHandler.ParameterChangeListener listener) {
         this.listeners.add(listener);
         this.dm.getDownloadState().addListener(this, "parameters", 1);
      }

      public void removeListener(DownloadManagerOptionsHandler.ParameterChangeListener listener) {
         this.listeners.remove(listener);
         this.dm.getDownloadState().removeListener(this, "parameters", 1);
      }
   }

   protected class downloadStateIntParameterAdapter implements IntSwtParameter.ValueProcessor {
      public Integer getValue(IntSwtParameter p) {
         int result = 0;
         String key = p.getParamID();

         for(int i = 0; i < ShuView.this.managers.length; ++i) {
            int val = ShuView.this.managers[i].getIntParameter(key);
            if (i == 0) {
               result = val;
            } else if (result != val) {
               return 0;
            }
         }

         return result;
      }

      public boolean setValue(IntSwtParameter p, Integer value) {
         boolean changed = false;
         String key = p.getParamID();

         for(int i = 0; i < ShuView.this.managers.length; ++i) {
            DownloadManagerOptionsHandler manager = ShuView.this.managers[i];
            if (value != manager.getIntParameter(key)) {
               manager.setIntParameter(key, value);
               changed = true;
            }
         }

         return changed;
      }
   }

   protected class downloadStateFloatParameterAdapter implements FloatSwtParameter.ValueProcessor {
      public Float getValue(FloatSwtParameter p) {
         int result = 0;
         String key = p.getParamID();

         for(int i = 0; i < ShuView.this.managers.length; ++i) {
            int val = ShuView.this.managers[i].getIntParameter(key);
            if (i == 0) {
               result = val;
            } else if (result != val) {
               return 0.0F;
            }
         }

         return (float)result / 1000.0F;
      }

      public boolean setValue(FloatSwtParameter p, Float _value) {
         boolean changed = ShuView.this.managers.length == 0;
         int value = (int)(_value * 1000.0F);
         String key = p.getParamID();

         for(int i = 0; i < ShuView.this.managers.length; ++i) {
            DownloadManagerOptionsHandler manager = ShuView.this.managers[i];
            if (value != manager.getIntParameter(key)) {
               manager.setIntParameter(key, value);
               changed = true;
            }
         }

         return changed;
      }
   }

   protected class downloadStateBooleanParameterAdapter implements BooleanSwtParameter.ValueProcessor {
      public Boolean getValue(BooleanSwtParameter p) {
         boolean result = false;
         String key = p.getParamID();

         for(int i = 0; i < ShuView.this.managers.length; ++i) {
            boolean val = ShuView.this.managers[i].getBooleanParameter(key);
            if (i == 0) {
               result = val;
            } else if (result != val) {
               return false;
            }
         }

         return result;
      }

      public boolean setValue(BooleanSwtParameter p, Boolean value) {
         boolean changed = ShuView.this.managers.length == 0;
         String key = p.getParamID();

         for(int i = 0; i < ShuView.this.managers.length; ++i) {
            DownloadManagerOptionsHandler manager = ShuView.this.managers[i];
            if (value != manager.getBooleanParameter(key)) {
               manager.setBooleanParameter(key, value);
               changed = true;
            }
         }

         return changed;
      }
   }
}
