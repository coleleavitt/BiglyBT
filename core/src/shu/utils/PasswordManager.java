package shu.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordManager {

	public static String getMD5Hash(String key) {
		byte[] uniqueKey = key.getBytes();
		byte[] hash = null;

		try {
			hash = MessageDigest.getInstance("MD5").digest(uniqueKey);
		} catch (NoSuchAlgorithmException var6) {
			throw new Error("no MD5 support in this VM");
		}

		StringBuilder hashString = new StringBuilder();

		for (int i = 0; i < hash.length; ++i) {
			String hex = Integer.toHexString(hash[i]);
			if (hex.length() == 1) {
				hashString.append('0');
				hashString.append(hex.charAt(hex.length() - 1));
			} else {
				hashString.append(hex.substring(hex.length() - 2));
			}
		}

		return hashString.toString();
	}

	public static boolean testMD5Password(String clearTextTestPassword, String encodedActualPassword)
			throws NoSuchAlgorithmException {
		String encodedTestPassword = getMD5Hash(clearTextTestPassword);
		return encodedTestPassword.equals(encodedActualPassword);
	}

	public static String getEncryptedPass(String authPass) {
		return authPass;
	}
}
