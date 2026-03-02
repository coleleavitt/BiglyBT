package ghostfucker.proleech;

public class Peer {
	private String ip;
	private int port;

	public Peer(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	public String getIp() {
		return this.ip;
	}

	public int getPort() {
		return this.port;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + (this.ip == null ? 0 : this.ip.hashCode());
		result = 31 * result + this.port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		Peer other = (Peer) obj;
		if (this.ip == null) {
			if (other.ip != null) {
				return false;
			}
		} else if (!this.ip.equals(other.ip)) {
			return false;
		}
		return this.port == other.port;
	}

	@Override
	public String toString() {
		return this.ip + ":" + this.port;
	}
}
