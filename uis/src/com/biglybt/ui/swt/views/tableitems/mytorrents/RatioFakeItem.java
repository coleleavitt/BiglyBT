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

import com.biglybt.core.download.DownloadManager;
import com.biglybt.core.download.DownloadManagerStats;
import com.biglybt.core.download.impl.DownloadManagerStatsImpl;
import com.biglybt.core.util.Constants;
import com.biglybt.core.util.DisplayFormatters;
import com.biglybt.pif.download.Download;
import com.biglybt.pif.ui.tables.TableCell;
import com.biglybt.pif.ui.tables.TableCellRefreshListener;
import com.biglybt.pif.ui.tables.TableColumn;
import com.biglybt.pif.ui.tables.TableColumnInfo;
import com.biglybt.ui.swt.views.table.CoreTableColumnSWT;

/** Fake share ratio column
 *
 * Calculates sentFake / receivedFake and formats as ratio (e.g., "2.50").
 */
public class RatioFakeItem
       extends CoreTableColumnSWT
       implements TableCellRefreshListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "ratioFake";

	@Override
	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_SHARING
		});
	}

	/** Default Constructor */
	public RatioFakeItem(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_TRAIL, 73, sTableID);
		setType(TableColumn.TYPE_TEXT);
		setRefreshInterval(INTERVAL_LIVE);
	}

	@Override
	public void refresh(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		long sentFake = 0;
		long receivedFake = 0;
		if (dm != null) {
			DownloadManagerStats stats = dm.getStats();
			if (stats instanceof DownloadManagerStatsImpl) {
				DownloadManagerStatsImpl statsImpl = (DownloadManagerStatsImpl) stats;
				sentFake = statsImpl.getTotalDataBytesSentFake();
				receivedFake = statsImpl.getTotalDataBytesReceivedFake();
			}
		}

		// Use millis-based ratio (like ShareRatioItem uses thousandths)
		int sr;
		if (receivedFake <= 0) {
			if (sentFake > 0) {
				sr = Integer.MAX_VALUE - 1;
			} else {
				sr = 0;
			}
		} else {
			sr = (int) ((1000 * sentFake) / receivedFake);
		}

		if (!cell.setSortValue(sr) && cell.isValid())
			return;

		String ratioText;
		if (sr == Integer.MAX_VALUE - 1) {
			ratioText = Constants.INFINITY_STRING;
		} else {
			ratioText = DisplayFormatters.formatDecimal((double) sr / 1000, 3);
		}

		cell.setText(ratioText);
	}
}
