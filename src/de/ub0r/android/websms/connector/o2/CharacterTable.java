package de.ub0r.android.websms.connector.o2;

import java.util.HashMap;
import java.util.Map;

/**
 * Character table.
 * 
 * @author Thomas Pilarski <Thomas.Pilarski@gmail.com>, flx
 */
final class CharacterTable {

	/** Mapping. */
	private static final Map<String, String> MAP = new HashMap<String, String>(
			512);

	/** Valid characters. */
	private static String validCharacters = null;

	static {
		MAP.put("\r", "");
		// MAP.put("´", "'");
		MAP.put("\u00F7", "%"); // ÷
		MAP.put("\u0130", "I"); // İ
		MAP.put("\u0131", "i"); // ı
		MAP.put("\u015E", "S"); // Ş
		MAP.put("\u015F", "s"); // ş
		MAP.put("\u00C7", "C"); // Ç
		MAP.put("\u00E7", "c"); // ç
		MAP.put("\u011E", "G"); // Ğ
		MAP.put("\u011F", "g"); // ğ

		MAP.put("\u0104", "A"); // Ą
		MAP.put("\u0106", "C"); // Ć
		MAP.put("\u0118", "E"); // Ę
		MAP.put("\u0141", "L"); // Ł
		MAP.put("\u0143", "N"); // Ń
		MAP.put("\u00D3", "O"); // Ó
		MAP.put("\u015A", "S"); // Ś
		MAP.put("\u0179", "Z"); // Ź
		MAP.put("\u017B", "Z"); // Ż

		String[] arr = MAP.keySet().toArray(new String[0]);
		StringBuffer strb = new StringBuffer(MAP.size());
		for (String vc : arr) {
			strb.append(vc);
		}
		validCharacters = strb.toString();
	}

	/** Default constructor. */
	private CharacterTable() {

	}

	/**
	 * Encode {@link String}.
	 * 
	 * @param str
	 *            {@link String}
	 * @return encoded {@link String}
	 */
	public static String encodeString(final String str) {
		final int l = str.length();
		StringBuffer strb = new StringBuffer(l);
		for (int i = 0; i < l; i++) {
			String s = str.substring(i, i + 1);
			String chr = MAP.get(s);
			if (chr == null) {
				strb.append(s);
			} else {
				strb.append(chr);
			}
		}
		return strb.toString();
	}
}