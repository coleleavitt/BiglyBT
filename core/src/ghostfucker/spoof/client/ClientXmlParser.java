package ghostfucker.spoof.client;

import java.io.File;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Manual XML parser for .client profile files.
 * Replaces JAXB unmarshalling since Jakarta XML Binding is not available.
 */
public class ClientXmlParser {

	/**
	 * Parse a full Client from a .client XML file.
	 */
	public static Client parseClient(File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(file);
		doc.getDocumentElement().normalize();

		Element root = doc.getDocumentElement(); // <client>
		Client c = new Client();

		// Protocol
		Element protocolEl = getFirstElement(root, "protocol");
		if (protocolEl != null) {
			c.protocol = new Client.Protocol();
			c.protocol.type = attr(protocolEl, "type");
			c.protocol.reservedBytes = textContent(protocolEl, "reservedBytes");

			Element clientNameEl = getFirstElement(protocolEl, "clientName");
			if (clientNameEl != null) {
				c.protocol.clientName = new Client.Protocol.ClientName();
				c.protocol.clientName.ltep = attr(clientNameEl, "ltep");
				c.protocol.clientName.azmp = attr(clientNameEl, "azmp");
				c.protocol.clientName.isEncodingDisabled = boolAttr(clientNameEl, "isEncodingDisabled");
			}

			Element clientVersionEl = getFirstElement(protocolEl, "clientVersion");
			if (clientVersionEl != null) {
				c.protocol.clientVersion = new Client.Protocol.ClientVersion();
				c.protocol.clientVersion.azmp = attr(clientVersionEl, "azmp");
			}
		}

		// PeerId
		Element peerIdEl = getFirstElement(root, "peerId");
		if (peerIdEl != null) {
			c.peerId = new Client.PeerId();
			c.peerId.type = attr(peerIdEl, "type");
			c.peerId.preFix = attr(peerIdEl, "preFix");
			c.peerId.length = intAttr(peerIdEl, "length");
			c.peerId.exceptions = attr(peerIdEl, "exceptions");
			c.peerId.isLowerCase = boolAttr(peerIdEl, "isLowerCase");
			c.peerId.isGlobal = boolAttr(peerIdEl, "isGlobal");
			c.peerId.charSet = textContent(peerIdEl, "charSet");
		}

		// Key
		Element keyEl = getFirstElement(root, "key");
		if (keyEl != null) {
			c.key = new Client.Key();
			c.key.type = attr(keyEl, "type");
			c.key.length = intAttr(keyEl, "length");
			c.key.exceptions = attr(keyEl, "exceptions");
			c.key.isLowerCase = boolAttr(keyEl, "isLowerCase");
			c.key.isGlobal = boolAttr(keyEl, "isGlobal");
			c.key.charSet = textContent(keyEl, "charSet");
		}

		// Announce
		Element announceEl = getFirstElement(root, "announce");
		if (announceEl != null) {
			c.announce = new Client.Announce();
			String proto = attr(announceEl, "protocol");
			if (proto != null && !proto.isEmpty()) {
				c.announce.protocol = proto;
			}

			Element announceInfoHash = getFirstElement(announceEl, "infoHash");
			if (announceInfoHash != null) {
				c.announce.infoHash = new Client.InfoHash();
				c.announce.infoHash.isLowerCase = boolAttr(announceInfoHash, "isLowerCase");
				c.announce.infoHash.exceptions = attr(announceInfoHash, "exceptions");
			}

			Element queryEl = getFirstElement(announceEl, "query");
			if (queryEl != null) {
				c.announce.query = new Client.Announce.Query();
				c.announce.query.staticNumwant = intAttr(queryEl, "staticNumwant");
				c.announce.query.ipType = intAttrDefault(queryEl, "ipType", -1);
				c.announce.query.ipMode = intAttrDefault(queryEl, "ipMode", -1);
				c.announce.query.ipFallBack = intAttr(queryEl, "ipFallBack");
				c.announce.query.fakeCorrupt = longAttrDefault(queryEl, "fakeCorrupt", -1);
				c.announce.query.param = new ArrayList<>();
				NodeList params = queryEl.getElementsByTagName("param");
				for (int i = 0; i < params.getLength(); i++) {
					c.announce.query.param.add(params.item(i).getTextContent());
				}
			}

			Element announceHeader = getFirstElement(announceEl, "header");
			if (announceHeader != null) {
				c.announce.header = parseHeader(announceHeader);
			}
		}

		// Scrape
		Element scrapeEl = getFirstElement(root, "scrape");
		if (scrapeEl != null) {
			c.scrape = new Client.Scrape();
			c.scrape.isDisabled = boolAttr(scrapeEl, "isDisabled");
			c.scrape.protocol = attr(scrapeEl, "protocol");

			Element scrapeInfoHash = getFirstElement(scrapeEl, "infoHash");
			if (scrapeInfoHash != null) {
				c.scrape.infoHash = new Client.InfoHash();
				c.scrape.infoHash.isLowerCase = boolAttr(scrapeInfoHash, "isLowerCase");
				c.scrape.infoHash.exceptions = attr(scrapeInfoHash, "exceptions");
			}

			Element scrapeHeader = getFirstElement(scrapeEl, "header");
			if (scrapeHeader != null) {
				c.scrape.header = parseHeader(scrapeHeader);
			}
		}

		return c;
	}

	/**
	 * Parse a SimpleClient from a .client XML file (only peerId section).
	 */
	public static SimpleClient parseSimpleClient(File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(file);
		doc.getDocumentElement().normalize();

		Element root = doc.getDocumentElement();
		SimpleClient sc = new SimpleClient();

		Element peerIdEl = getFirstElement(root, "peerId");
		if (peerIdEl != null) {
			sc.peerId = new SimpleClient.PeerId();
			sc.peerId.type = attr(peerIdEl, "type");
			sc.peerId.preFix = attr(peerIdEl, "preFix");
			sc.peerId.length = intAttr(peerIdEl, "length");
			sc.peerId.isGlobal = boolAttr(peerIdEl, "isGlobal");
		}

		return sc;
	}

	// --- Helper methods ---

	private static Client.Header parseHeader(Element headerEl) {
		Client.Header header = new Client.Header();
		header.showDefaultPort = boolAttr(headerEl, "showDefaultPort");
		header.field = new ArrayList<>();
		NodeList fields = headerEl.getElementsByTagName("field");
		for (int i = 0; i < fields.getLength(); i++) {
			header.field.add(fields.item(i).getTextContent());
		}
		return header;
	}

	private static Element getFirstElement(Element parent, String tagName) {
		NodeList list = parent.getElementsByTagName(tagName);
		if (list.getLength() > 0) {
			// Only get direct children, not nested
			for (int i = 0; i < list.getLength(); i++) {
				if (list.item(i).getParentNode() == parent) {
					return (Element) list.item(i);
				}
			}
			// Fallback: return first match
			return (Element) list.item(0);
		}
		return null;
	}

	private static String attr(Element el, String name) {
		if (el.hasAttribute(name)) {
			String val = el.getAttribute(name);
			return val.isEmpty() ? null : val;
		}
		return null;
	}

	private static boolean boolAttr(Element el, String name) {
		String val = el.getAttribute(name);
		return "true".equalsIgnoreCase(val);
	}

	private static int intAttr(Element el, String name) {
		String val = el.getAttribute(name);
		if (val == null || val.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int intAttrDefault(Element el, String name, int defaultVal) {
		String val = el.getAttribute(name);
		if (val == null || val.isEmpty()) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static long longAttrDefault(Element el, String name, long defaultVal) {
		String val = el.getAttribute(name);
		if (val == null || val.isEmpty()) {
			return defaultVal;
		}
		try {
			return Long.parseLong(val);
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static String textContent(Element parent, String tagName) {
		Element el = getFirstElement(parent, tagName);
		if (el != null) {
			String text = el.getTextContent();
			if (text != null) {
				text = text.trim();
				return text.isEmpty() ? null : text;
			}
		}
		return null;
	}
}
