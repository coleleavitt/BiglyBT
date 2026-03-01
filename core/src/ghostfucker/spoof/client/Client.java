package ghostfucker.spoof.client;

import java.util.List;
import java.util.ArrayList;
import ghostfucker.utils.PropertyReader;

/**
 * XML data model for .client profile files.
 * Mirrors the schema.client structure for spoofed client configuration.
 */
public class Client {

	public Protocol protocol;
	public PeerId peerId;
	public Key key;
	public Announce announce;
	public Scrape scrape;

	public static class Protocol {
		public String type;
		public ClientName clientName;
		public ClientVersion clientVersion;
		public String reservedBytes;

		public static class ClientName {
			public String ltep;
			public String azmp;
			public boolean isEncodingDisabled;
		}

		public static class ClientVersion {
			public String azmp;
		}
	}

	public static class PeerId {
		public String type;
		public String preFix;
		public int length;
		public String exceptions;
		public boolean isLowerCase;
		public boolean isGlobal;
		public String charSet;
		// Transient - not from XML, set by Validator for custom IDs from file
		public transient ghostfucker.utils.PropertyReader propertyReader;
	}

	public static class Key {
		public String type;
		public int length;
		public String exceptions;
		public boolean isLowerCase;
		public boolean isGlobal;
		public String charSet;
	}

	public static class Announce {
		public String protocol = "HTTP/1.1";
		public InfoHash infoHash;
		public Query query;
		public Header header;

		public static class Query {
			public int staticNumwant = 0;
			public int ipType = -1;
			public int ipMode = -1;
			public int ipFallBack;
			public long fakeCorrupt = -1;
			public List<String> param = new ArrayList<>();
		}
	}

	public static class Scrape {
		public boolean isDisabled;
		public String protocol;
		public InfoHash infoHash;
		public Header header;
	}

	public static class InfoHash {
		public boolean isLowerCase;
		public String exceptions;
	}

	public static class Header {
		public boolean showDefaultPort;
		public List<String> field = new ArrayList<>();
	}
}
