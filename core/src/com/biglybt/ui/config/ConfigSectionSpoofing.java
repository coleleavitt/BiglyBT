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

package com.biglybt.ui.config;

import java.util.ArrayList;
import java.util.List;

import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.pifimpl.local.ui.config.*;

import com.biglybt.pif.ui.config.ConfigSection;
import com.biglybt.pif.ui.config.Parameter;

import ghostfucker.spoof.Configuration;
import ghostfucker.spoof.PerfectSpoof;

/**
 * Configuration section for client spoofing and fake stats settings.
 * Appears under the Connection section in the configuration view.
 */
public class ConfigSectionSpoofing
	extends ConfigSectionImpl
{
	public static final String SECTION_ID = "spoofing";

	// Config keys matching existing PerfectSpoof usage
	private static final String CFG_PS_ACTIVE = "PSisActive";
	private static final String CFG_PS_CLIENT_NAME = "PSclientName";
	private static final String CFG_PS_CLIENT_VERSION = "PSclientVersion";

	// Config keys for fake stats
	private static final String CFG_FAKE_STATS_ENABLE = "PSfakeStatsEnable";
	private static final String CFG_FAKE_UPLOAD_MULTIPLIER = "PSfakeUploadMultiplier";
	private static final String CFG_FAKE_DOWNLOAD_REDUCTION = "PSfakeDownloadReduction";
	private static final String CFG_GHOST_LEECH_MODE = "PSghostLeechMode";

	public ConfigSectionSpoofing() {
		super(SECTION_ID, ConfigSection.SECTION_CONNECTION);
	}

	@Override
	public void build() {

		// ==================== CLIENT SPOOFING ====================

		List<Parameter> listSpoof = new ArrayList<>();

		add(new LabelParameterImpl(
				"ConfigView.section.spoofing.info"), listSpoof);

		// Enable spoofing checkbox
		BooleanParameterImpl paramActive = new BooleanParameterImpl(
				CFG_PS_ACTIVE,
				"ConfigView.section.spoofing.enable");
		add(paramActive, listSpoof);

		// Load available clients from clientfiles/
		Configuration spoofConfig = new Configuration();

		// Client selection dropdown
		String[] clientNames = spoofConfig.getNames();
		StringListParameterImpl paramClientName = new StringListParameterImpl(
				CFG_PS_CLIENT_NAME,
				"ConfigView.section.spoofing.client",
				clientNames, clientNames);
		add(paramClientName, listSpoof);

		// Version selection dropdown - populated based on selected client
		String selectedClient = COConfigurationManager.getStringParameter(
				CFG_PS_CLIENT_NAME, clientNames.length > 0 ? clientNames[0] : "");
		String[] versions = spoofConfig.getVersions(selectedClient);
		StringListParameterImpl paramClientVersion = new StringListParameterImpl(
				CFG_PS_CLIENT_VERSION,
				"ConfigView.section.spoofing.version",
				versions, versions);
		add(paramClientVersion, listSpoof);


		// When client selection changes, rebuild to refresh the version dropdown
		paramClientName.addListener(p -> {
			String newClient = paramClientName.getValue();
			String[] newVersions = spoofConfig.getVersions(newClient);
			if (newVersions.length > 0) {
				COConfigurationManager.setParameter(CFG_PS_CLIENT_VERSION, newVersions[0]);
			}
			requestRebuild();
		});

		// Wire enable/disable to client controls
		paramActive.addEnabledOnSelection(paramClientName, paramClientVersion);

		// Activate/deactivate spoofing when checkbox changes
		paramActive.addListener(p -> {
			PerfectSpoof.setActive(paramActive.getValue());
		});

		add("pgSpoofClient", new ParameterGroupImpl(
				"ConfigView.section.spoofing.group.client", listSpoof));

		// ==================== FAKE STATS ====================

		List<Parameter> listFakeStats = new ArrayList<>();

		add(new LabelParameterImpl(
				"ConfigView.section.spoofing.fakestats.info"), listFakeStats);

		// Enable fake stats checkbox
		BooleanParameterImpl paramFakeStatsEnable = new BooleanParameterImpl(
				CFG_FAKE_STATS_ENABLE,
				"ConfigView.section.spoofing.fakestats.enable");
		add(paramFakeStatsEnable, listFakeStats);

		// Fake upload multiplier (e.g., 1.5x, 2.0x, etc.)
		FloatParameterImpl paramUploadMultiplier = new FloatParameterImpl(
				CFG_FAKE_UPLOAD_MULTIPLIER,
				"ConfigView.section.spoofing.fakestats.upload.multiplier",
				1.0f, 10.0f, 1);
		paramUploadMultiplier.setSuffixLabelKey(
				"ConfigView.section.spoofing.fakestats.upload.multiplier.suffix");
		add(paramUploadMultiplier, listFakeStats);

		// Fake download reduction percentage (0-100%)
		IntParameterImpl paramDownloadReduction = new IntParameterImpl(
				CFG_FAKE_DOWNLOAD_REDUCTION,
				"ConfigView.section.spoofing.fakestats.download.reduction",
				0, 100);
		paramDownloadReduction.setSuffixLabelKey(
				"ConfigView.section.spoofing.fakestats.download.reduction.suffix");
		add(paramDownloadReduction, listFakeStats);

		// Wire enable/disable to fake stats controls
		paramFakeStatsEnable.addEnabledOnSelection(
				paramUploadMultiplier, paramDownloadReduction);

		add("pgFakeStats", new ParameterGroupImpl(
				"ConfigView.section.spoofing.group.fakestats", listFakeStats));

		// ==================== GHOST LEECH MODE ====================

		List<Parameter> listGhost = new ArrayList<>();

		add(new LabelParameterImpl(
				"ConfigView.section.spoofing.ghostleech.info"), listGhost);

		BooleanParameterImpl paramGhostLeech = new BooleanParameterImpl(
				CFG_GHOST_LEECH_MODE,
				"ConfigView.section.spoofing.ghostleech.enable");
		add(paramGhostLeech, listGhost);

		add("pgGhostLeech", new ParameterGroupImpl(
				"ConfigView.section.spoofing.group.ghostleech", listGhost));
	}
}
