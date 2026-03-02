/*
 * Copyright (C) Bigly Software, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.biglybt.ui.swt.views.tableitems.mytorrents;

import org.eclipse.swt.graphics.Color;

import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.core.config.ParameterListener;
import com.biglybt.core.download.DownloadManager;
import com.biglybt.core.util.Constants;
import com.biglybt.core.util.DisplayFormatters;
import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.mainwindow.Colors;
import com.biglybt.ui.swt.views.table.CoreTableColumnSWT;
import com.biglybt.pif.download.Download;
import com.biglybt.pif.ui.tables.*;

/**
 * Share Ratio Fake column - displays the spoofed/fake share ratio
 * reported to the tracker, as opposed to the real share ratio.
 *
 * Uses DownloadManagerStats.getShareRatioFake() which returns
 * the ratio in thousandths (e.g. 1500 = 1.500).
 */
public class ShareRatioFakeItem
       extends CoreTableColumnSWT
       implements TableCellRefreshListener, ParameterListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	private final static String CONFIG_ID = "StartStopManager_iFirstPriority_ShareRatio";
	public static final String COLUMN_ID = "shareRatioFake";
	private int iMinShareRatio;
	private boolean changeFG = true;

	@Override
	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_SHARING, CAT_SWARM });
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	/** Default Constructor */
	public ShareRatioFakeItem(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_TRAIL, 73, sTableID);
		setType(TableColumn.TYPE_TEXT);
		setRefreshInterval(INTERVAL_LIVE);

		setPosition(POSITION_LAST);

		iMinShareRatio = COConfigurationManager.getIntParameter(CONFIG_ID);
		COConfigurationManager.addWeakParameterListener(this, false, CONFIG_ID);
	}

	@Override
	public void refresh(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();

		int sr = (dm == null || dm.getStats() == null) ? 0 : dm.getStats().getShareRatioFake();

		if (sr == Integer.MAX_VALUE) {
			sr = Integer.MAX_VALUE - 1;
		}
		if (sr == -1) {
			sr = Integer.MAX_VALUE;
		}

		if (!cell.setSortValue(sr) && cell.isValid())
			return;

		String shareRatio = "";

		if (sr == Integer.MAX_VALUE) {
			shareRatio = Constants.INFINITY_STRING;
		} else {
			shareRatio = DisplayFormatters.formatDecimal((double) sr / 1000, 3);
		}

		if (cell.setText(shareRatio) && changeFG) {
			Color color = sr < iMinShareRatio ? Colors.colorWarning : null;
			cell.setForeground(Utils.colorToIntArray(color));
		}
	}

	@Override
	public void parameterChanged(String parameterName) {
		iMinShareRatio = COConfigurationManager.getIntParameter(CONFIG_ID);
		invalidateCells();
	}

	public boolean isChangeFG() {
		return changeFG;
	}

	public void setChangeFG(boolean changeFG) {
		this.changeFG = changeFG;
	}
}
