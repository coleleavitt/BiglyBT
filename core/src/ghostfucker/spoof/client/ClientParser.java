package ghostfucker.spoof.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parses .client XML profile files into {@link Client} objects.
 * <p>
 * Uses standard javax.xml.parsers APIs with no external dependencies.
 * Handles BOM-prefixed files and UTF-8 encoding.
 * <p>
 * Schema reference: schema.client
 */
public class ClientParser {

	/**
	 * Parse a .client XML file into a Client object.
	 *
	 * @param file the .client XML file
	 * @return parsed Client
	 * @throws ClientParseException if the file cannot be parsed or required fields are missing
	 */
	public Client parse(File file) throws ClientParseException {
		try (FileInputStream fis = new FileInputStream(file)) {
			return parse(fis);
		} catch (IOException e) {
			throw new ClientParseException("Failed to read file: " + file.getAbsolutePath(), e);
		}
	}

	/**
	 * Parse a .client XML stream into a Client object.
	 *
	 * @param is the input stream containing .client XML data
	 * @return parsed Client
	 * @throws ClientParseException if the stream cannot be parsed or required fields are missing
	 */
	public Client parse(InputStream is) throws ClientParseException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// Security: disable external entities
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(is);
			doc.getDocumentElement().normalize();

			Element root = doc.getDocumentElement();
			if (!"client".equals(root.getTagName())) {
				throw new ClientParseException("Root element must be <client>, found: " + root.getTagName());
			}

			Client c = new Client();
			c.protocol = parseProtocol(root);
			c.peerId = parsePeerId(root);
			c.key = parseKey(root);
			c.announce = parseAnnounce(root);
			c.scrape = parseScrape(root);
			return c;
		} catch (ClientParseException e) {
			throw e;
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new ClientParseException("XML parsing failed", e);
		}
	}

	// ---- Section parsers ----

	private Client.Protocol parseProtocol(Element root) throws ClientParseException {
		Element el = requireChild(root, "protocol");
		Client.Protocol p = new Client.Protocol();
		p.type = requireAttr(el, "type", "protocol.type");
		p.reservedBytes = textContent(el, "reservedBytes");

		Element clientNameEl = firstDirectChild(el, "clientName");
		if (clientNameEl != null) {
			p.clientName = new Client.Protocol.ClientName();
			p.clientName.ltep = optAttr(clientNameEl, "ltep");
			p.clientName.azmp = optAttr(clientNameEl, "azmp");
			p.clientName.isEncodingDisabled = boolAttr(clientNameEl, "isEncodingDisabled");
		}

		Element clientVersionEl = firstDirectChild(el, "clientVersion");
		if (clientVersionEl != null) {
			p.clientVersion = new Client.Protocol.ClientVersion();
			p.clientVersion.azmp = optAttr(clientVersionEl, "azmp");
		}

		return p;
	}

	private Client.PeerId parsePeerId(Element root) throws ClientParseException {
		Element el = requireChild(root, "peerId");
		Client.PeerId pid = new Client.PeerId();
		pid.type = requireAttr(el, "type", "peerId.type");
		pid.preFix = requireAttr(el, "preFix", "peerId.preFix");
		pid.length = intAttr(el, "length", 0);
		pid.exceptions = optAttr(el, "exceptions");
		pid.isLowerCase = boolAttr(el, "isLowerCase");
		pid.isGlobal = boolAttr(el, "isGlobal");
		pid.charSet = textContent(el, "charSet");
		return pid;
	}

	private Client.Key parseKey(Element root) throws ClientParseException {
		Element el = requireChild(root, "key");
		Client.Key k = new Client.Key();
		k.type = requireAttr(el, "type", "key.type");
		k.length = intAttr(el, "length", 0);
		k.exceptions = optAttr(el, "exceptions");
		k.isLowerCase = boolAttr(el, "isLowerCase");
		k.isGlobal = boolAttr(el, "isGlobal");
		k.charSet = textContent(el, "charSet");
		return k;
	}

	private Client.Announce parseAnnounce(Element root) throws ClientParseException {
		Element el = requireChild(root, "announce");
		Client.Announce a = new Client.Announce();

		String proto = optAttr(el, "protocol");
		if (proto != null) {
			a.protocol = proto;
		}

		Element infoHashEl = firstDirectChild(el, "infoHash");
		if (infoHashEl != null) {
			a.infoHash = parseInfoHash(infoHashEl);
		}

		Element queryEl = firstDirectChild(el, "query");
		if (queryEl != null) {
			a.query = new Client.Announce.Query();
			a.query.staticNumwant = intAttr(queryEl, "staticNumwant", 0);
			a.query.ipType = intAttr(queryEl, "ipType", -1);
			a.query.ipMode = intAttr(queryEl, "ipMode", -1);
			a.query.ipFallBack = intAttr(queryEl, "ipFallBack", 0);
			a.query.fakeCorrupt = longAttr(queryEl, "fakeCorrupt", -1);
			a.query.param = new ArrayList<>();
			NodeList params = queryEl.getElementsByTagName("param");
			for (int i = 0; i < params.getLength(); i++) {
				a.query.param.add(params.item(i).getTextContent());
			}
		}

		Element headerEl = firstDirectChild(el, "header");
		if (headerEl != null) {
			a.header = parseHeader(headerEl);
		}

		return a;
	}

	private Client.Scrape parseScrape(Element root) {
		Element el = firstDirectChild(root, "scrape");
		if (el == null) {
			return null;
		}

		Client.Scrape s = new Client.Scrape();
		s.isDisabled = boolAttr(el, "isDisabled");
		s.protocol = optAttr(el, "protocol");

		Element infoHashEl = firstDirectChild(el, "infoHash");
		if (infoHashEl != null) {
			s.infoHash = parseInfoHash(infoHashEl);
		}

		Element headerEl = firstDirectChild(el, "header");
		if (headerEl != null) {
			s.header = parseHeader(headerEl);
		}

		return s;
	}

	private Client.InfoHash parseInfoHash(Element el) {
		Client.InfoHash ih = new Client.InfoHash();
		ih.isLowerCase = boolAttr(el, "isLowerCase");
		ih.exceptions = optAttr(el, "exceptions");
		return ih;
	}

	private Client.Header parseHeader(Element el) {
		Client.Header h = new Client.Header();
		h.showDefaultPort = boolAttr(el, "showDefaultPort");
		h.field = new ArrayList<>();
		NodeList fields = el.getElementsByTagName("field");
		for (int i = 0; i < fields.getLength(); i++) {
			h.field.add(fields.item(i).getTextContent());
		}
		return h;
	}

	// ---- DOM helpers ----

	/**
	 * Get the first direct child element with the given tag name.
	 * Avoids matching nested descendants (e.g. scrape/infoHash vs announce/infoHash).
	 */
	private static Element firstDirectChild(Element parent, String tagName) {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && tagName.equals(node.getNodeName())) {
				return (Element) node;
			}
		}
		return null;
	}

	/**
	 * Require a direct child element; throw if missing.
	 */
	private static Element requireChild(Element parent, String tagName) throws ClientParseException {
		Element el = firstDirectChild(parent, tagName);
		if (el == null) {
			throw new ClientParseException("Required element <" + tagName + "> not found in <" + parent.getTagName() + ">");
		}
		return el;
	}

	/**
	 * Get an attribute value, returning null if absent or empty.
	 */
	private static String optAttr(Element el, String name) {
		if (!el.hasAttribute(name)) {
			return null;
		}
		String val = el.getAttribute(name);
		return val.isEmpty() ? null : val;
	}

	/**
	 * Require a non-empty attribute; throw if missing or empty.
	 */
	private static String requireAttr(Element el, String name, String fieldPath) throws ClientParseException {
		String val = optAttr(el, name);
		if (val == null) {
			throw new ClientParseException("Required attribute '" + name + "' missing or empty on <" + el.getTagName() + "> (" + fieldPath + ")");
		}
		return val;
	}

	private static boolean boolAttr(Element el, String name) {
		return "true".equalsIgnoreCase(el.getAttribute(name));
	}

	private static int intAttr(Element el, String name, int defaultVal) {
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

	private static long longAttr(Element el, String name, long defaultVal) {
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

	/**
	 * Get trimmed text content of a direct child element, or null if absent/empty.
	 */
	private static String textContent(Element parent, String tagName) {
		Element el = firstDirectChild(parent, tagName);
		if (el != null) {
			String text = el.getTextContent();
			if (text != null) {
				text = text.trim();
				return text.isEmpty() ? null : text;
			}
		}
		return null;
	}

	/**
	 * Thrown when a .client file cannot be parsed or is structurally invalid.
	 */
	public static class ClientParseException extends Exception {
		private static final long serialVersionUID = 1L;

		public ClientParseException(String message) {
			super(message);
		}

		public ClientParseException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
