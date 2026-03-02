package ghostfucker.spoof.ut;

import java.util.Random;

/**
 * uTorrent-style peer ID generator.
 * Generates 20-byte IDs: 10-byte prefix + 8 random bytes + 2 SHA1 verification bytes.
 */
public class Generator {

	public static byte[] createId(String prefix) {
		if (prefix == null) {
			return null;
		}

		int length = getByteLength(prefix);
		if (length != -1 && length == 10) {
			byte[] id = new byte[20];
			int pos = 0;

			for (int i = 0; pos < 10; ++pos) {
				char ch = prefix.charAt(i);
				if (ch == '%') {
					ch = (char) Integer.parseInt(prefix.substring(i + 1, i + 3), 16);
					i += 2;
				}

				id[pos] = (byte) ch;
				++i;
			}

			Random r = new Random();

			while (pos < 18) {
				int rnd = r.nextInt();

				for (int n = Math.min(18 - pos, 4); n-- > 0; rnd >>= 8) {
					id[pos++] = (byte) rnd;
				}
			}

			byte[] sha = new byte[18];
			System.arraycopy(id, 0, sha, 0, 18);
			SHA sha1 = new SHA();
			sha = sha1.digest(sha);
			if (sha == null) {
				return null;
			}

			// Last 2 bytes are SHA1 verification bytes
			id[pos++] = sha[0];
			id[pos] = sha[1];
			return id;
		}

		return null;
	}

	private static int getByteLength(String s) {
		int length = 0;

		int pos;
		for (pos = 0; pos < s.length(); ++pos) {
			if (s.charAt(pos) == '%') {
				pos += 2;
			}

			++length;
		}

		return pos > s.length() ? -1 : length;
	}
}
