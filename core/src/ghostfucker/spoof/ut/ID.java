package ghostfucker.spoof.ut;

/**
 * Wrapper for uTorrent-style peer ID generation.
 * Delegates to Generator for the actual 20-byte ID creation.
 */
public final class ID {

	private ID() {
	}

	/**
	 * Create a 20-byte uTorrent-compatible peer ID from a 10-byte prefix.
	 *
	 * @param prefix URL-encoded prefix string (10 bytes when decoded)
	 * @return 20-byte peer ID array, or null if prefix is invalid
	 */
	public static final byte[] create(String prefix) {
		return Generator.createId(prefix);
	}
}
