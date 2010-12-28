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

	/** The unknown character. */
	private static final String UNKNOWN_CHAR = "?";

	/** Valid characters. */
	private static String validCharacters = null;

	static {
		for (String s : new String[] { "0", "1", "2", "3", "4", "5", "6", "7",
				"8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
				"K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
				"W", "X", "Y", "Z", "a", "b", "c", "d", "e", "f", "g", "h",
				"i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
				"u", "v", "w", "x", "y", "z", // .
				"\u00C4", // Ä
				"\u00D6", // Ö
				"\u00DC", // Ü
				"\u00E4", // ä
				"\u00F6", // ö
				"\u00FC", // ü
				"\u00DF", // ß
		}) {
			MAP.put(s, s);
		}
		for (String s : new String[] {
				"\u00A7", // §
				"\u20AC", // €
				"\u00A3", // £
				"$", "^", "`", "!", "?", "#", "%", "&", "'", "\"", "[", "]",
				"(", ")", "{", "}", "*", "+", "-", "_", ",", ".", ":", ";",
				"<", "=", ">", "|", "/", "\\", "@", " ", "	", "\n", }) {
			MAP.put(s, s);
		}
		MAP.put("\r", "");
		MAP.put("\u00F7", "%"); // ÷

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
				strb.append(UNKNOWN_CHAR);
			} else {
				strb.append(chr);
			}
		}
		return strb.toString();
	}

	/**
	 * Get valid characters.
	 * 
	 * @return valid characters
	 */
	public static String getValidCharacters() {
		return CharacterTable.validCharacters;
	}
}