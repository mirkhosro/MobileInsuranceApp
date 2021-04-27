public final class Utils {
	/**
	 * Formats the number and returns a string containing the number with
	 * thousand separators.
	 * 
	 * @param n
	 * @return
	 */
	public static String formatNumber(long n) {
		StringBuffer sb = new StringBuffer();
		final char arabicZero = latin2arabic('0');
		
		while (n > 1000) {
			long r = n % 1000;
			sb.insert(0, toArabicNumerals(r));
			if (r < 100)
				sb.insert(0, arabicZero);
			if (r < 10)
				sb.insert(0, arabicZero);
			sb.insert(0, ',');
			n /= 1000;
		}
		sb.insert(0, toArabicNumerals(n));
		return sb.toString();
	}
	/**
	 * Converts a string containing Latin numerals to a string contating Arabic numerals
	 * @param latinNumber
	 * @return
	 */
	public static String toArabicNumerals(String latinNumber) {
		char arabicChars[] = new char[latinNumber.length()];
		for (int i = 0; i < arabicChars.length; i++) {
			arabicChars[i] = latin2arabic(latinNumber.charAt(i));			
		}
		
		return new String(arabicChars);
	}
	
	public static String toArabicNumerals(long latinNumber) {
		return toArabicNumerals(latinNumber + "");
		
	}
	
	private static char latin2arabic(char c) {
		return (char)(c + 0x660 - 0x30);
	}
}
