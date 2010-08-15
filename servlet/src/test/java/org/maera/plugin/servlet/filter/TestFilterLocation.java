package org.maera.plugin.servlet.filter;

import junit.framework.TestCase;

import java.util.Locale;

public class TestFilterLocation extends TestCase {
    public void testParse() {
        assertEquals(FilterLocation.AFTER_ENCODING, FilterLocation.parse("after-encoding"));
        assertEquals(FilterLocation.AFTER_ENCODING, FilterLocation.parse("after_encoding"));
        assertEquals(FilterLocation.AFTER_ENCODING, FilterLocation.parse("After-Encoding"));
        try {
            FilterLocation.parse(null);
            fail();
        } catch (IllegalArgumentException ex) {
            // test passed
        }
        try {
            FilterLocation.parse("asf");
            fail();
        } catch (IllegalArgumentException ex) {
            // test passed
        }
    }

    public void testParseWithTurkishCharacters() {
        Locale defLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "", ""));
            assertEquals(FilterLocation.BEFORE_LOGIN, FilterLocation.parse("before-log\u0069n"));
            assertEquals(FilterLocation.BEFORE_LOGIN, FilterLocation.parse("before-log\u0131n"));
            assertEquals(FilterLocation.BEFORE_LOGIN, FilterLocation.parse("before-login"));
        }
        finally {
            Locale.setDefault(defLocale);
        }
    }
}
