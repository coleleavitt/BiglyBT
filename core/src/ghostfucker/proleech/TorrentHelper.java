package ghostfucker.proleech;

import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.core.download.DownloadManager;
import com.biglybt.core.logging.LogAlert;
import com.biglybt.core.logging.Logger;
import com.biglybt.core.peer.PEPeer;
import com.biglybt.core.torrent.TOTorrent;
import com.biglybt.core.torrent.TOTorrentException;
import com.biglybt.core.util.BEncoder;
import com.biglybt.core.util.LightHashMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import ghostfucker.proleech.Peer;

public class TorrentHelper {

	public static void exportTorrents(DownloadManager[] dms) {
		for (DownloadManager dm : dms) {
			processTorrent(dm);
		}
	}

	public static void exportTorrent(DownloadManager dm) {
		if (COConfigurationManager.getBooleanParameter("plAutoCap")) {
			processTorrent(dm);
		}
	}

	private static void processTorrent(DownloadManager dm) {
		File dir = checkAndGetPath();
		if (dir == null) {
			return;
		}

		String name = new String(dm.getTorrent().getName());
		File file = new File(dir, name + ".torrent");

		if (!COConfigurationManager.getBooleanParameter("plOverrideTorrent") && file.exists()) {
			return;
		}

		Map map = serializeAndCleanTorrent(dm);
		if (map == null) {
			return;
		}

		Set<Peer> peers = extractPeers(dm);
		if (peers == null) {
			return;
		}

		boolean checkPeerCount = COConfigurationManager.getBooleanParameter("plAutoCapPeersCnt");
		if (checkPeerCount) {
			int minPeers = COConfigurationManager.getIntParameter("plAutoCapPeersCntVal");
			if (peers.size() < minPeers) {
				return;
			}
		}

		byte[] encoded = encodeTorrent(dm, buildProleechTorrent(dm, map, peers));
		if (encoded != null) {
			writeTorrent(dm, file, encoded);
		}
	}

	private static File checkAndGetPath() {
		boolean valid = false;
		File dir = null;

		try {
			dir = new File(COConfigurationManager.getStringParameter("plSaveDir"));
			valid = dir.exists();
			if (!valid) {
				valid = dir.mkdirs();
			}
		} catch (Exception ignored) {
		}

		if (!valid) {
			Logger.log(new LogAlert(false, LogAlert.AT_ERROR, "[ProLeech]: Torrent directory not valid."));
		}

		return valid ? dir : null;
	}

	@SuppressWarnings("unchecked")
	private static Map serializeAndCleanTorrent(DownloadManager dm) {
		TOTorrent torrent = dm.getTorrent();

		Map map;
		try {
			map = torrent.serialiseToMap();
		} catch (TOTorrentException e) {
			Logger.log(new LogAlert(false, LogAlert.AT_ERROR,
					"[ProLeech]: Cannot serialize '" + new String(torrent.getName()) + "'.", e));
			return null;
		}

		// Keep only the "info" key
		for (Object key : map.keySet().toArray()) {
			if (!key.toString().equals("info")) {
				map.remove(key);
			}
		}

		return map;
	}

	@SuppressWarnings("unchecked")
	private static Set<Peer> extractPeers(DownloadManager dm) {
		HashSet<Peer> peers = new HashSet<>();

		try {
			ArrayList trackerPeers = (ArrayList) dm.getTrackerClient()
					.getTrackerResponseCache().get("tracker_peers");
			if (trackerPeers != null) {
				for (Object o : trackerPeers) {
					LightHashMap entry = (LightHashMap) o;
					String ip = new String((byte[]) entry.get("ip"));
					int port = ((Long) entry.get("port")).intValue();
					peers.add(new Peer(ip, port));
				}
			}
		} catch (Exception ignored) {
			// Tracker client may not be available
		}

		try {
			if (dm.getPeerManager() != null) {
				for (PEPeer peer : dm.getPeerManager().getPeers()) {
					peers.add(new Peer(peer.getIp(), peer.getPort()));
				}
			}
		} catch (Exception ignored) {
			// Peer manager may not be available
		}

		if (peers.isEmpty()) {
			String name = new String(dm.getTorrent().getName());
			Logger.log(new LogAlert(false, LogAlert.AT_ERROR,
					"[ProLeech]: No peers available for '" + name + "'."));
			return null;
		}

		return peers;
	}

	@SuppressWarnings("unchecked")
	private static Map buildProleechTorrent(DownloadManager dm, Map map, Set<Peer> peers) {
		StringBuilder sb = new StringBuilder();

		for (Peer peer : peers) {
			sb.append(peer.getIp()).append(":").append(peer.getPort()).append(";");
		}

		String peersStr = sb.deleteCharAt(sb.length() - 1).toString();
		HashMap<String, Object> peersMap = new HashMap<>();
		peersMap.put("peers", peersStr);
		peersMap.put("tracker", dm.getTorrent().getAnnounceURL().getHost());
		map.put("proleech", peersMap);
		return map;
	}

	private static byte[] encodeTorrent(DownloadManager dm, Map map) {
		try {
			return BEncoder.encode(map);
		} catch (IOException e) {
			Logger.log(new LogAlert(false, LogAlert.AT_ERROR,
					"[ProLeech]: Cannot bencode '" + new String(dm.getTorrent().getName()) + "'.", e));
			return null;
		}
	}

	private static void writeTorrent(DownloadManager dm, File file, byte[] data) {
		FileOutputStream fos = null;
		String name = new String(dm.getTorrent().getName());

		try {
			fos = new FileOutputStream(file);
			fos.write(data);
		} catch (Exception e) {
			Logger.log(new LogAlert(false, LogAlert.AT_ERROR,
					"[ProLeech]: Cannot write '" + name + "'.", e));
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (Exception ignored) {
			}
		}
	}
}
