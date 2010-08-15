package org.maera.plugin.util;

/**
 * String utility methods designed for memory / cpu efficiency
 */
public class EfficientStringUtils {
    /**
     * Test to see if a given string ends with some suffixes. Avoids the cost
     * of concatenating the suffixes together
     *
     * @param src      the source string to be tested
     * @param suffixes the set of suffixes
     * @return true if src ends with the suffixes concatenated together
     */
    public static boolean endsWith(final String src, final String... suffixes) {
        int pos = src.length();

        for (int i = suffixes.length - 1; i >= 0; i--) {
            final String suffix = suffixes[i];
            pos -= suffix.length();
            if (!src.startsWith(suffix, pos)) {
                return false;
            }
        }
        return true;
    }
}
