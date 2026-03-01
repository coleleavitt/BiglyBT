package ghostfucker.spoof.client;

/**
 * Simplified client model used by Configuration for listing available profiles.
 * Only contains the peerId section needed for validation and display.
 */
public class SimpleClient {

	public String name;
	public String version;
	public byte[] peerIdBytes;
	public byte[] reservedBytes;
	public String userAgent;
	public String key;
	public PeerId peerId;

	public boolean isForcedCid() {
		return peerId.type.equalsIgnoreCase("customId");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public byte[] getPeerIdBytes() {
		return peerIdBytes;
	}

	public void setPeerIdBytes(byte[] peerIdBytes) {
		this.peerIdBytes = peerIdBytes;
	}

	public byte[] getReservedBytes() {
		return reservedBytes;
	}

	public void setReservedBytes(byte[] reservedBytes) {
		this.reservedBytes = reservedBytes;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public PeerId getPeerIdObj() {
		return peerId;
	}

	public void setPeerIdObj(PeerId peerId) {
		this.peerId = peerId;
	}

	public static class PeerId {
		public String type;
		public String preFix;
		public int length;
		public boolean isGlobal;
	}
}
