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
		MAP.put("£", "£");
		MAP.put("$", "$");
		MAP.put("¥", "¥");
		MAP.put("è", "è");
		MAP.put("é", "é");
		MAP.put("ù", "ù");
		MAP.put("ì", "ì");
		MAP.put("ò", "ò");
		MAP.put("Ç", "Ç");
		MAP.put("Ø", "Ø");
		MAP.put("ø", "ø");
		MAP.put("Å", "Å");
		MAP.put("å", "å");
		MAP.put("_", "_");
		MAP.put("Æ", "Æ");
		MAP.put("æ", "æ");
		MAP.put("ß", "ß");
		MAP.put("É", "É");
		MAP.put(" ", " ");
		MAP.put("!", "!");
		MAP.put("\"", "\"");
		MAP.put("#", "#");
		MAP.put("%", "%");
		MAP.put("&", "&");
		MAP.put("'", "'");
		MAP.put("(", "(");
		MAP.put(")", ")");
		MAP.put("*", "*");
		MAP.put("+", "+");
		MAP.put(",", ",");
		MAP.put("-", "-");
		MAP.put(".", ".");
		MAP.put("/", "/");
		MAP.put("0", "0");
		MAP.put("1", "1");
		MAP.put("2", "2");
		MAP.put("3", "3");
		MAP.put("4", "4");
		MAP.put("5", "5");
		MAP.put("6", "6");
		MAP.put("7", "7");
		MAP.put("8", "8");
		MAP.put("9", "9");
		MAP.put(":", ":");
		MAP.put(";", ";");
		MAP.put("<", "<");
		MAP.put("=", "=");
		MAP.put(">", ">");
		MAP.put("?", "?");
		MAP.put("¡", "¡");
		MAP.put("A", "A");
		MAP.put("B", "B");
		MAP.put("C", "C");
		MAP.put("D", "D");
		MAP.put("E", "E");
		MAP.put("F", "F");
		MAP.put("G", "G");
		MAP.put("H", "H");
		MAP.put("I", "I");
		MAP.put("J", "J");
		MAP.put("K", "K");
		MAP.put("L", "L");
		MAP.put("M", "M");
		MAP.put("N", "N");
		MAP.put("O", "O");
		MAP.put("P", "P");
		MAP.put("Q", "Q");
		MAP.put("R", "R");
		MAP.put("S", "S");
		MAP.put("T", "T");
		MAP.put("U", "U");
		MAP.put("V", "V");
		MAP.put("W", "W");
		MAP.put("X", "X");
		MAP.put("Y", "Y");
		MAP.put("Z", "Z");
		MAP.put("Ä", "Ä");
		MAP.put("Ö", "Ö");
		MAP.put("Ñ", "Ñ");
		MAP.put("Ü", "Ü");
		MAP.put("§", "§");
		MAP.put("¿", "¿");
		MAP.put("a", "a");
		MAP.put("b", "b");
		MAP.put("c", "c");
		MAP.put("d", "d");
		MAP.put("e", "e");
		MAP.put("f", "f");
		MAP.put("g", "g");
		MAP.put("h", "h");
		MAP.put("i", "i");
		MAP.put("j", "j");
		MAP.put("k", "k");
		MAP.put("l", "l");
		MAP.put("m", "m");
		MAP.put("n", "n");
		MAP.put("o", "oF");
		MAP.put("p", "p");
		MAP.put("q", "q");
		MAP.put("r", "r");
		MAP.put("s", "s");
		MAP.put("t", "t");
		MAP.put("u", "u");
		MAP.put("v", "v");
		MAP.put("w", "w");
		MAP.put("x", "x");
		MAP.put("y", "y");
		MAP.put("z", "z");
		MAP.put("ä", "ä");
		MAP.put("ö", "ö");
		MAP.put("ñ", "ñ");
		MAP.put("ü", "ü");
		MAP.put("à", "à");
		MAP.put("\n", "\n");
		MAP.put("\r", "");

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
		StringBuffer strb = new StringBuffer(str.length() * 3 + 4);

		final int length = str.length();
		for (int offset = 0; offset < length;) {
			final int codepoint = str.codePointAt(offset);
			offset += Character.charCount(codepoint);
			String chr = MAP.get(String.valueOf((char) codepoint));
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