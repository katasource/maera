package org.maera.plugin.util;

import org.apache.commons.lang.StringUtils;

import java.util.Comparator;

/**
 * Compares dotted version strings of varying length. Makes a best effort with
 * other delimiters and non-numeric versions.
 * <p/>
 * For dotted decimals, comparison is as you'd expect: 0.1 is before 0.2 is before
 * 1.0 is before 2.0. This works for any number of dots.
 * <p/>
 * More complicated version numbers are compared by splitting the version strings
 * into components using the {@link #DELIMITER_PATTERN} and comparing each
 * component in order. The first difference found when comparing components
 * left-to-right is returned.
 * <p/>
 * Two numeric components (containing only digits) are compared as integers. A
 * numeric component comes after any non-numeric one. Two non-numeric components
 * are ordered by {@link String#compareToIgnoreCase(String)}.
 */
public class VersionStringComparator implements Comparator<String> {
    public static final String DELIMITER_PATTERN = "[\\.,-]";
    public static final String COMPONENT_PATTERN = "[\\d\\w]+";
    public static final String VALID_VERSION_PATTERN = COMPONENT_PATTERN + "(" + DELIMITER_PATTERN + COMPONENT_PATTERN + ")*";

    public static boolean isValidVersionString(final String version) {
        return (version != null) && version.matches(VALID_VERSION_PATTERN);
    }

    /**
     * Compares two version strings. If either argument is not a String,
     * this method returns 0.
     *
     * @throws IllegalArgumentException if either argument is a String,
     * but does not match {@link #VALID_VERSION_PATTERN}.
     * @see #isValidVersionString(String)
     */
    //    public int compare(Object o1, Object o2)
    //    {
    //        if (!(o1 instanceof String)) return 0;
    //        if (!(o2 instanceof String)) return 0;
    //
    //        return compare((String) o1, (String) o2);
    //    }

    /**
     * Compares two version strings using the algorithm described above.
     *
     * @return <tt>-1</tt> if version1 is before version2, <tt>1</tt> if version2 is before
     *         version1, or <tt>0</tt> if the versions are equal.
     * @throws IllegalArgumentException if either argument does not match {@link #VALID_VERSION_PATTERN}.
     * @see #isValidVersionString(String)
     */
    public int compare(final String version1, final String version2) {
        // Get the version numbers, remove all whitespaces
        String thisVersion = "0";
        if (StringUtils.isNotEmpty(version1)) {
            thisVersion = version1.replaceAll(" ", "");
        }
        String compareVersion = "0";
        if (StringUtils.isNotEmpty(version2)) {
            compareVersion = version2.replaceAll(" ", "");
        }

        if (!thisVersion.matches(VALID_VERSION_PATTERN) || !compareVersion.matches(VALID_VERSION_PATTERN)) {
            throw new IllegalArgumentException("Version number '" + thisVersion + "' cannot be compared to '" + compareVersion + "'");
        }

        // Split the version numbers
        final String[] v1 = thisVersion.split(DELIMITER_PATTERN);
        final String[] v2 = compareVersion.split(DELIMITER_PATTERN);

        final Comparator<String> componentComparator = new VersionStringComponentComparator();

        // Compare each place, until we find a difference and then return. If empty, assume zero.
        for (int i = 0; i < (v1.length > v2.length ? v1.length : v2.length); i++) {
            final String component1 = i >= v1.length ? "0" : v1[i];
            final String component2 = i >= v2.length ? "0" : v2[i];

            if (componentComparator.compare(component1, component2) != 0) {
                return componentComparator.compare(component1, component2);
            }
        }

        return 0;
    }

    private class VersionStringComponentComparator implements Comparator<String> {
        public static final int FIRST_GREATER = 1;
        public static final int SECOND_GREATER = -1;

        //        public int compare(Object o1, Object o2)
        //        {
        //            if (!(o1 instanceof String)) return 0;
        //            if (!(o2 instanceof String)) return 0;
        //
        //            return compare((String) o1, (String) o2);
        //        }

        public int compare(final String component1, final String component2) {
            if (component1.equalsIgnoreCase(component2)) {
                return 0;
            }

            if (isInteger(component1) && isInteger(component2)) {
                // both numbers -- parse and compare
                if (Integer.parseInt(component1) > Integer.parseInt(component2)) {
                    return FIRST_GREATER;
                }
                if (Integer.parseInt(component2) > Integer.parseInt(component1)) {
                    return SECOND_GREATER;
                }
                return 0;
            }

            // 2.3-alpha < 2.3.0
            if ("0".equals(component1)) {
                return FIRST_GREATER;
            }
            if ("0".equals(component2)) {
                return SECOND_GREATER;
            }

            // 2.3a < 2.3
            if (isInteger(component1) && component2.startsWith(component1)) {
                return FIRST_GREATER;
            }
            if (isInteger(component2) && component1.startsWith(component2)) {
                return SECOND_GREATER;
            }

            // 2.3a < 2.3b
            return component1.compareToIgnoreCase(component2);
        }

        private boolean isInteger(final String string) {
            return string.matches("\\d+");
        }
    }
}
