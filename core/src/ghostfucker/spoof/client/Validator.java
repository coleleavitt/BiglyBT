package ghostfucker.spoof.client;

import com.biglybt.core.config.COConfigurationManager;
import ghostfucker.utils.PropertyReader;

import java.io.IOException;

/**
 * Validates .client profile configurations for correctness.
 * Supports both full Client validation and SimpleClient (lightweight) validation.
 */
public class Validator {

	private Client c;
	private SimpleClient sc;

	public Validator(Client c) {
		this.c = c;
	}

	public Validator(SimpleClient sc) {
		this.sc = sc;
	}

	public void run() throws ValidationException {
		if (this.sc == null) {
			this.validateClient();
		} else {
			this.validateSimpleClient();
		}
	}

	private void validateSimpleClient() throws ValidationException {
		validateObject(this.sc, "client");
		validateObject(this.sc.peerId, "peerId");
		validateString(this.sc.peerId.type, "peerId.type");
		validateString(this.sc.peerId.preFix, "peerId.preFix");
		this.sc.peerId.length -= PSClient.getByteLength(this.sc.peerId.preFix);
		validateInteger(this.sc.peerId.length, "peerId.length");
	}

	private void validateClient() throws ValidationException {
		validateObject(this.c, "client");
		this.validateProtocol();
		this.validatePeerId();
		this.validateKey();
		this.validateAnnounce();
		this.validateScrape();
		this.validateCustomId();
		this.ensureDefaultValues();
	}

	private void validateProtocol() throws ValidationException {
		validateObject(this.c.protocol, "protocol");
		validateString(this.c.protocol.type, "protocol.type");
		if (!this.c.protocol.type.equalsIgnoreCase("btlp")) {
			if (this.c.protocol.type.equalsIgnoreCase("ltep")) {
				validateObject(this.c.protocol.clientName, "protocol.clientName");
				validateString(this.c.protocol.clientName.ltep, "protocol.clientName.ltep");
			} else {
				if (!this.c.protocol.type.equalsIgnoreCase("azmp")) {
					throw new ValidationException("Invalid value: protocol.type");
				}

				validateObject(this.c.protocol.clientName, "protocol.clientName");
				validateString(this.c.protocol.clientName.ltep, "protocol.clientName.ltep");
				validateString(this.c.protocol.clientName.azmp, "protocol.clientName.azmp");
				validateObject(this.c.protocol.clientVersion, "protocol.clientVersion");
				validateString(this.c.protocol.clientVersion.azmp, "protocol.clientVersion.azmp");
			}
		}

		validateObject(this.c.protocol.reservedBytes, "protocol.reservedBytes");
		String[] reservedBits = this.c.protocol.reservedBytes.split("\\|");
		if (reservedBits.length == 8) {
			try {
				for (String rsvBit : reservedBits) {
					Integer.decode(rsvBit);
				}
				return;
			} catch (Exception ignored) {
			}
		}

		throw new ValidationException("Invalid value: protocol.reservedBytes");
	}

	private void validatePeerId() throws ValidationException {
		validateObject(this.c.peerId, "peerId");
		validateString(this.c.peerId.type, "peerId.type");
		validateString(this.c.peerId.preFix, "peerId.preFix");
		this.c.peerId.length -= PSClient.getByteLength(this.c.peerId.preFix);
		validateInteger(this.c.peerId.length, "peerId.length");
		if (!this.c.peerId.type.equalsIgnoreCase("custom") && !this.c.peerId.type.equalsIgnoreCase("ut")) {
			if (this.c.peerId.type.equalsIgnoreCase("hex")) {
				validateOptionalString(this.c.peerId.exceptions, "peerId.exceptions", 255);
			} else if (!this.c.peerId.type.equalsIgnoreCase("anum")) {
				throw new ValidationException("Invalid value: peerId.type");
			}

			validateOptionalString(this.c.peerId.charSet, "peerId.charSet", 255);
		}
	}

	private void validateKey() throws ValidationException {
		validateObject(this.c.key, "key");
		validateString(this.c.key.type, "key.type");
		validateInteger(this.c.key.length, "key.length");
		if (this.c.key.type.equalsIgnoreCase("hex")) {
			validateOptionalString(this.c.key.exceptions, "key.exceptions", 255);
		} else if (!this.c.key.type.equalsIgnoreCase("anum")) {
			throw new ValidationException("Invalid value: key.type");
		}

		validateOptionalString(this.c.key.charSet, "key.charSet", 255);
	}

	private void validateAnnounce() throws ValidationException {
		validateObject(this.c.announce, "announce");
		validateOptionalString(this.c.announce.protocol, "announce.protocol");
		this.validateInfoHash(this.c.announce.infoHash, "announce.infoHash");
		validateObject(this.c.announce.query, "announce.query");
		validateObject(this.c.announce.query.param, "announce.query.param");
		if (this.c.announce.query.param.size() == 0) {
			throw new ValidationException("Invalid value: announce.query.param");
		}

		for (String s : this.c.announce.query.param) {
			validateString(s, "announce.query.param");
		}

		if (this.c.announce.query.ipType <= 2 && this.c.announce.query.ipMode <= 1
				&& (this.c.announce.query.ipType == -1 || this.c.announce.query.ipMode != -1)) {
			this.validateHeader(this.c.announce.header, "announce.header");
		} else {
			throw new ValidationException("Invalid value: announce.query.(ipType|ipMode)");
		}
	}

	private void validateScrape() throws ValidationException {
		if (this.c.scrape != null) {
			validateOptionalString(this.c.scrape.protocol, "scrape.protocol");
			this.validateInfoHash(this.c.scrape.infoHash, "scrape.infoHash");
			if (this.c.scrape.header != null) {
				this.validateHeader(this.c.scrape.header, "scrape.header");
			}
		}
	}

	private void validateInfoHash(Client.InfoHash infoHash, String elementName) throws ValidationException {
		if (infoHash != null) {
			validateOptionalString(infoHash.exceptions, elementName + ".exceptions", 255);
		}
	}

	private void validateHeader(Client.Header header, String elementName) throws ValidationException {
		validateObject(header, elementName);
		validateObject(header.field, elementName + ".field");
		if (header.field.size() == 0) {
			throw new ValidationException("Invalid value: " + elementName + ".field");
		}

		String host = null;

		for (String s : header.field) {
			if (s.toUpperCase().contains("HOST")) {
				host = s;
			}

			String[] tmp = s.split(":", 2);
			if (tmp.length != 2) {
				throw new ValidationException("Invalid configuration: " + elementName + ".field");
			}

			validateString(tmp[0], elementName + ".field");
			validateString(tmp[1], elementName + ".field");
		}

		if (host == null || !host.toUpperCase().contains("{HOST}") || !host.toUpperCase().contains("{PORT}")) {
			throw new ValidationException("Invalid configuration: " + elementName + ".field");
		}
	}

	private void validateCustomId() throws ValidationException {
		if (COConfigurationManager.getBooleanParameter("PSisCid")) {
			try {
				if (COConfigurationManager.getBooleanParameter("PSisCidFromFile")) {
					PropertyReader propertyReader;

					try {
						propertyReader = new PropertyReader("clientfiles/CustomId.properties");
					} catch (IOException ioe) {
						throw new ValidationException("Cannot read file: CustomID.properties", ioe);
					}

					for (int i = 0; i < propertyReader.getSize(); ++i) {
						if (!PSClient.isValidCid(propertyReader.getValue(i), this.c.peerId.length)) {
							throw new ValidationException("Invalid custom id length (CustomId.properties - No. " + i + ")");
						}
					}

					this.c.peerId.propertyReader = propertyReader;
				} else if (!PSClient.isValidCid(COConfigurationManager.getStringParameter("PScid"), this.c.peerId.length)) {
					throw new ValidationException("Invalid custom id length");
				}
			} catch (ValidationException ve) {
				COConfigurationManager.setParameter("PSisCidFromFile", false);
				COConfigurationManager.setParameter("PSisCid", false);
				throw ve;
			}
		}
	}

	private void ensureDefaultValues() {
		if (this.c.announce.infoHash == null) {
			this.c.announce.infoHash = new Client.InfoHash();
		}

		if (this.c.scrape == null) {
			this.c.scrape = new Client.Scrape();
		}

		if (!this.c.scrape.isDisabled) {
			if (this.c.scrape.protocol == null) {
				this.c.scrape.protocol = this.c.announce.protocol;
			}

			if (this.c.scrape.infoHash == null) {
				this.c.scrape.infoHash = this.c.announce.infoHash;
			}

			if (this.c.scrape.header == null) {
				this.c.scrape.header = this.c.announce.header;
			}
		}
	}

	// --- Validation helpers ---

	private static void validateString(String str, String elementName) throws ValidationException {
		validateString(str, elementName, 42);
	}

	private static void validateString(String str, String elementName, int maxLength) throws ValidationException {
		validateObject(str, elementName);
		str = str.trim();
		if (str.length() <= 0 || str.length() >= maxLength) {
			throw new ValidationException("Invalid value: " + elementName);
		}
	}

	private static void validateOptionalString(String str, String elementName) throws ValidationException {
		validateOptionalString(str, elementName, str == null ? 0 : str.length());
	}

	private static void validateOptionalString(String str, String elementName, int maxLength) throws ValidationException {
		if (str != null) {
			validateString(str, elementName, maxLength);
		}
	}

	private static void validateInteger(int i, String elementName) throws ValidationException {
		validateInteger(i, elementName, 1, 42);
	}

	private static void validateInteger(int i, String elementName, int min, int max) throws ValidationException {
		if (i < min || i > max) {
			throw new ValidationException("Invalid value: " + elementName);
		}
	}

	private static void validateObject(Object obj, String elementName) throws ValidationException {
		if (obj == null) {
			throw new ValidationException("Required element not set: " + elementName);
		}
	}

	public static class ValidationException extends Exception {
		private static final long serialVersionUID = 1L;

		public ValidationException(String message) {
			super(message);
		}

		public ValidationException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
