/*
 * Copyright (C) Bigly Software.  All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.biglybt.ui.swt.views.configsections;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.biglybt.pifimpl.local.ui.config.ParameterImpl;
import com.biglybt.ui.config.ConfigSectionImpl;
import com.biglybt.ui.swt.Messages;
import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.config.BaseSwtParameter;
import com.biglybt.ui.swt.config.BooleanSwtParameter;
import com.biglybt.ui.swt.config.ParameterChangeListener;
import com.biglybt.ui.swt.config.StringListSwtParameter;
import com.biglybt.ui.swt.config.StringSwtParameter;
import com.biglybt.ui.swt.config.SwtParameter;
import com.biglybt.ui.swt.config.actionperformer.IAdditionalActionPerformer;
import com.biglybt.ui.swt.mainwindow.Colors;
import com.biglybt.ui.swt.mainwindow.SWTThread;

import ghostfucker.spoof.Configuration;
import ghostfucker.spoof.PerfectSpoof;
import ghostfucker.spoof.client.SimpleClient;

/**
 * Configuration section for BiglyBT Extreme Mod ("Shu") settings.
 *
 * <p>This section currently implements:</p>
 * <ul>
 *   <li><b>Perfect Spoof</b> – client identity spoofing with dynamic
 *       client-name / version selection, custom peer-ID (CID) entry,
 *       and file-backed CID sourcing.</li>
 * </ul>
 *
 * <p>The Ratio Tool and Fake Upload sections are tracked as separate tasks
 * and are <em>not</em> present here yet.</p>
 *
 * <h3>COConfigurationManager keys consumed</h3>
 * <table>
 *   <tr><td>{@code PSisActive}</td>       <td>boolean – master toggle for Perfect Spoof</td></tr>
 *   <tr><td>{@code PSclientName}</td>     <td>string  – selected client name</td></tr>
 *   <tr><td>{@code PSclientVersion}</td>  <td>string  – selected client version</td></tr>
 *   <tr><td>{@code PSisCid}</td>          <td>boolean – enable custom peer-ID</td></tr>
 *   <tr><td>{@code PSisCidFromFile}</td>  <td>boolean – source CID from file</td></tr>
 *   <tr><td>{@code PScid}</td>            <td>string  – the peer-ID string</td></tr>
 * </table>
 */
public class ConfigSectionShu
		extends ConfigSectionImpl
		implements BaseConfigSectionSWT
{
	// -----------------------------------------------------------------------
	// Constants
	// -----------------------------------------------------------------------

	public static final String SECTION_ID = "shu";

	// -----------------------------------------------------------------------
	// State
	// -----------------------------------------------------------------------

	private Display display;

	// -----------------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------------

	public ConfigSectionShu() {
		super(SECTION_ID, "root");
	}

	// -----------------------------------------------------------------------
	// ConfigSectionImpl
	// -----------------------------------------------------------------------

	@Override
	public void build() {
		// Parameters are wired up inside configSectionCreate via SwtParameter
		// factories so nothing needs to be registered here.
	}

	// -----------------------------------------------------------------------
	// BaseConfigSectionSWT
	// -----------------------------------------------------------------------

	@Override
	public void configSectionCreate(Composite parent,
			Map<ParameterImpl, BaseSwtParameter> mapParamToSwtParam)
	{
		display = SWTThread.getInstance().getDisplay();

		// ------------------------------------------------------------------ //
		// Root composite                                                       //
		// ------------------------------------------------------------------ //

		Composite cShu = new Composite(parent, SWT.NONE);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		cShu.setLayoutData(gridData);

		FormLayout rootLayout = new FormLayout();
		try {
			rootLayout.spacing = 5;
		} catch (NoSuchFieldError ignored) {
			// Older SWT builds do not expose FormLayout#spacing.
		}
		cShu.setLayout(rootLayout);

		// ------------------------------------------------------------------ //
		// Header label                                                         //
		// ------------------------------------------------------------------ //

		Label headerLabel = new Label(cShu, SWT.NONE);
		headerLabel.setText("BiglyBT Extreme Mod by SB-Innovation");
		FormData fd = new FormData();
		headerLabel.setLayoutData(fd);

		// ------------------------------------------------------------------ //
		// Azureus / legacy spoof info labels                                  //
		// (retained from the original section for layout continuity)          //
		// ------------------------------------------------------------------ //

		Label azureusSpoof1 = new Label(cShu, SWT.NONE);
		Messages.setLanguageText(azureusSpoof1, "ConfigView.label.azureusspoof1");
		fd = new FormData();
		fd.top  = new FormAttachment(headerLabel, 8);
		fd.left = new FormAttachment(0, 3);
		azureusSpoof1.setLayoutData(fd);

		Label azureusSpoof2 = new Label(cShu, SWT.NONE);
		Messages.setLanguageText(azureusSpoof2, "ConfigView.label.azureusspoof2");
		fd = new FormData();
		fd.top  = new FormAttachment(azureusSpoof1, 1);
		fd.left = new FormAttachment(0, 8);
		azureusSpoof2.setLayoutData(fd);

		// ------------------------------------------------------------------ //
		// PSisActive – master toggle checkbox                                  //
		// ------------------------------------------------------------------ //

		final BooleanSwtParameter psIsActive = new BooleanSwtParameter(
				cShu, "PSisActive",
				"ConfigView.label.spooftitle",
				/* suffixLabelKey */ null,
				/* valueProcessor  */ null);

		fd = new FormData();
		fd.top  = new FormAttachment(azureusSpoof2);
		psIsActive.setLayoutData(fd);

		// ------------------------------------------------------------------ //
		// Perfect Spoof group                                                  //
		// ------------------------------------------------------------------ //

		Group perfectSpoof = new Group(cShu, SWT.NONE);
		Messages.setLanguageText(perfectSpoof, "ConfigView.label.spooftitle");

		FormLayout groupLayout = new FormLayout();
		groupLayout.marginBottom = 6;
		groupLayout.marginRight  = 6;
		groupLayout.marginLeft   = 6;
		groupLayout.marginTop    = 9;
		perfectSpoof.setLayout(groupLayout);

		fd = new FormData();
		fd.top  = new FormAttachment(azureusSpoof2, 3);
		fd.left = new FormAttachment(psIsActive.getMainControl());
		perfectSpoof.setLayoutData(fd);

		// ---- Client configuration (loaded from clientfiles/ directory) ----- //

		Configuration config = new Configuration();

		// ── Client name dropdown ──────────────────────────────────────────── //

		String[] spoofNames = config.getNames();

		StringListSwtParameter spoofNameList = new StringListSwtParameter(
				perfectSpoof, "PSclientName",
				/* labelKey */    null,
				/* suffixKey */   null,
				spoofNames, spoofNames,
				/* translucent */ true,
				/* processor */   null);

		fd = new FormData();
		fd.width = 165;
		spoofNameList.setLayoutData(fd);

		// ── Version selector ──────────────────────────────────────────────── //

		String[] initialVersions = config.getVersions((String) spoofNameList.getValue());

		StringListSwtParameter spoofVersionList = new StringListSwtParameter(
				perfectSpoof, "PSclientVersion",
				/* labelKey */    null,
				/* suffixKey */   null,
				initialVersions, initialVersions,
				/* translucent */ true,
				/* processor */   null);

		fd = new FormData();
		fd.left  = new FormAttachment(spoofNameList.getMainControl(), 6);
		fd.width = 165;
		spoofVersionList.setLayoutData(fd);

		// ── "Use custom peer-ID (CID)" checkbox ──────────────────────────── //

		final BooleanSwtParameter isCid = new BooleanSwtParameter(
				perfectSpoof, "PSisCid",
				"ConfigView.label.cid",
				/* suffixKey */  null,
				/* processor */  null);

		fd = new FormData();
		fd.top = new FormAttachment(spoofNameList.getMainControl(), 8);
		isCid.setLayoutData(fd);

		// ── "CID from file" checkbox ──────────────────────────────────────── //

		final BooleanSwtParameter isCidFromFile = new BooleanSwtParameter(
				perfectSpoof, "PSisCidFromFile",
				"ConfigView.label.cidFromFile",
				/* suffixKey */  null,
				/* processor */  null);

		fd = new FormData();
		fd.top  = new FormAttachment(isCid.getMainControl(), 3);
		fd.left = new FormAttachment(0, 18);
		isCidFromFile.setLayoutData(fd);

		// ── CID prefix field (read-only, populated from client file) ─────── //

		final StringSwtParameter cidPrefix = new StringSwtParameter(
				perfectSpoof, "tmpPreSelect",
				/* labelKey */  null,
				/* suffixKey */ null,
				/* processor */ null);

		fd = new FormData();
		fd.top   = new FormAttachment(isCidFromFile.getMainControl(), 4);
		fd.left  = new FormAttachment(0, 18);
		fd.width = 90;
		cidPrefix.setLayoutData(fd);
		cidPrefix.getMainControl().setEnabled(false); // always read-only; driven by version change

		// ── CID validation message (shown when peer-ID length is wrong) ───── //

		Label cidFormatMessage = new Label(perfectSpoof, SWT.NONE);
		cidFormatMessage.setText("Wrong Length");
		cidFormatMessage.setForeground(Colors.red);

		fd = new FormData();
		fd.top  = new FormAttachment(isCid.getMainControl(), 3);
		fd.left = new FormAttachment(cidPrefix.getMainControl(), 6);
		cidFormatMessage.setLayoutData(fd);
		cidFormatMessage.setVisible(false);

		// ── CID input field ───────────────────────────────────────────────── //

		final StringSwtParameter spoofCid = new StringSwtParameter(
				perfectSpoof, "PScid",
				/* labelKey */  null,
				/* suffixKey */ null,
				/* processor */ null);

		fd = new FormData();
		fd.top   = new FormAttachment(isCidFromFile.getMainControl(), 4);
		fd.left  = new FormAttachment(cidPrefix.getMainControl(), 6);
		fd.width = 250;
		spoofCid.setLayoutData(fd);

		// ── Restart-required hint ─────────────────────────────────────────── //

		Label restartMessage = new Label(perfectSpoof, SWT.NONE);
		Messages.setLanguageText(restartMessage, "ConfigView.label.restartmessage");

		fd = new FormData();
		fd.top = new FormAttachment(cidPrefix.getMainControl(), 12);
		restartMessage.setLayoutData(fd);

		// ------------------------------------------------------------------ //
		// Control / param arrays (used by the master toggle performer)        //
		// ------------------------------------------------------------------ //

		final Control[] perfectSpoofCtrl = {
				spoofNameList.getMainControl(),
				spoofVersionList.getMainControl(),
				isCid.getMainControl(),
				isCidFromFile.getMainControl(),
				cidPrefix.getMainControl(),
				spoofCid.getMainControl(),
				restartMessage,
				cidFormatMessage
		};

		final SwtParameter<?>[] perfectSpoofParam = {
				spoofNameList,
				spoofVersionList,
				isCid,
				isCidFromFile,
				cidPrefix,
				spoofCid
		};

		// ------------------------------------------------------------------ //
		// Listeners & action performers                                        //
		// ------------------------------------------------------------------ //

		// Validate CID length whenever the CID text changes.
		final ParameterChangeListener spoofCidListener = p ->
				Utils.execSWTThread(() ->
						cidFormatMessage.setVisible(
								!isValidCid(spoofCid) && isCid.isSelected() && !isCidFromFile.isSelected()));

		spoofCid.addChangeListener(spoofCidListener);

		// Also re-validate on raw SWT key-up so the message updates while typing.
		spoofCid.getMainControl().addListener(SWT.KeyUp, event ->
				Utils.execSWTThread(() ->
						spoofCid.setValue(((Text) spoofCid.getMainControl()).getText())));

		// "CID from file" toggle – enables/disables the CID text field.
		final IAdditionalActionPerformer<Boolean> isCidFromFileListener =
				new IAdditionalActionPerformer<Boolean>() {
					@Override
					public void performAction() {
						Utils.execSWTThread(() -> {
							spoofCid.setEnabled(!isCidFromFile.isSelected() && isCid.isSelected());
							spoofCidListener.parameterChanged(null);
						});
					}

					@Override
					public void valueChanged(Boolean newValue) {
					}
				};

		isCidFromFile.setAdditionalActionPerformer(isCidFromFileListener);

		// "Use CID" toggle – gates the CID-from-file checkbox and the text field.
		IAdditionalActionPerformer<Boolean> isCidListener =
				new IAdditionalActionPerformer<Boolean>() {
					@Override
					public void performAction() {
						Utils.execSWTThread(() -> {
							boolean isSelected = isCid.isSelected();

							Boolean isGlobal = (Boolean) isCidFromFile.getMainControl()
									.getData("isGlobal");
							if (isGlobal == null) {
								isGlobal = false;
							}

							isCidFromFile.setEnabled(isSelected && isGlobal);
							isCidFromFileListener.performAction();
						});
					}

					@Override
					public void valueChanged(Boolean newValue) {
					}
				};

		isCid.setAdditionalActionPerformer(isCidListener);

		// Client name change → refresh version list.
		// Note: StringListSwtParameter doesn't have setList(), so we update the underlying Combo directly
		spoofNameList.addChangeListener(p ->
				Utils.execSWTThread(() -> {
					String[] versions = config.getVersions((String) spoofNameList.getValue());
					Control ctrl = spoofVersionList.getMainControl();
					if (ctrl instanceof org.eclipse.swt.widgets.Combo) {
						org.eclipse.swt.widgets.Combo combo = (org.eclipse.swt.widgets.Combo) ctrl;
						combo.removeAll();
						for (String version : versions) {
							combo.add(version);
						}
						if (versions.length > 0) {
							combo.select(0);
							spoofVersionList.setValue(versions[0]);
						}
					}
				}));

		// Version change → update prefix, CID constraints, and toggle states.
		final ParameterChangeListener spoofVersionListener = p ->
				Utils.execSWTThread(() -> {
					SimpleClient sc = config.getClient(
							(String) spoofNameList.getValue(),
							(String) spoofVersionList.getValue());

					if (sc.isForcedCid()) {
						isCid.setSelected(true);
					}
					isCid.setEnabled(!sc.isForcedCid());

					if (!sc.peerId.isGlobal) {
						isCidFromFile.setSelected(true);
					}
					isCidFromFile.setEnabled(sc.peerId.isGlobal && isCid.isSelected());

					cidPrefix.setValue(sc.peerId.preFix);
					spoofCid.getMainControl().setData("length", sc.peerId.length);
					isCidFromFile.getMainControl().setData("isGlobal", sc.peerId.isGlobal);

					isCidListener.performAction();
				});

		spoofVersionList.addChangeListener(spoofVersionListener);

		// Fire once on startup so the UI reflects the saved configuration.
		spoofVersionListener.parameterChanged(null);

		// Master toggle (PSisActive) – enables/disables the entire group.
		psIsActive.setAdditionalActionPerformer(new IAdditionalActionPerformer<Boolean>() {
			@Override
			public void performAction() {
				Utils.execSWTThread(() -> {
					boolean isSelected = psIsActive.isSelected();

					for (Control c : perfectSpoofCtrl) {
						c.setEnabled(isSelected);
					}
					for (SwtParameter<?> param : perfectSpoofParam) {
						param.setEnabled(isSelected);
					}

					if (psIsActive.isSelected()) {
						// Prefix is always read-only; CID fields obey their own rules.
						cidPrefix.setEnabled(false);
						spoofCid.setEnabled(false);
						isCidFromFile.setEnabled(false);
						spoofVersionListener.parameterChanged(null);
					}
				});
			}

			@Override
			public void valueChanged(Boolean newValue) {
			}
		});

		// ------------------------------------------------------------------ //
		// Disable entire group when PerfectSpoof feature is not available     //
		// (no client files found on disk)                                     //
		// ------------------------------------------------------------------ //

		if (!PerfectSpoof.isAvailable) {
			for (Control c : perfectSpoofCtrl) {
				c.setEnabled(false);
			}
			for (SwtParameter<?> param : perfectSpoofParam) {
				param.setEnabled(false);
			}
			psIsActive.setSelected(false);
			psIsActive.setEnabled(false);
			perfectSpoof.setEnabled(false);
		}
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	/**
	 * Returns {@code true} when the current value of {@code cidParam} has the
	 * exact byte-length advertised by the selected client file.
	 *
	 * <p>The expected length is stored as an {@link Integer} in the SWT widget
	 * data under the key {@code "length"}.  If no length has been set yet the
	 * method returns {@code true} (no false-positive error message).</p>
	 *
	 * @param cidParam the CID text parameter to validate
	 * @return {@code true} if the value length matches the client's requirement
	 */
	private boolean isValidCid(StringSwtParameter cidParam) {
		Object rawLength = cidParam.getMainControl().getData("length");
		if (!(rawLength instanceof Integer)) {
			return true;
		}
		int required = (Integer) rawLength;
		if (required <= 0) {
			return true;
		}
		String value = (String) cidParam.getValue();
		return value != null && value.length() == required;
	}
}
