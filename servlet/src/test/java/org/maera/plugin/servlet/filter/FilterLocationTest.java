package org.maera.plugin.servlet.filter;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FilterLocationTest {

    @Test
    public void testParse() {
        assertEquals(FilterLocation.AFTER_ENCODING, FilterLocation.parse("after-encoding"));
        assertEquals(FilterLocation.AFTER_ENCODING, FilterLocation.parse("after_encoding"));
        assertEquals(FilterLocation.AFTER_ENCODING, FilterLocation.parse("After-Encoding"));
        try {
            FilterLocation.parse(null);
            fail();
        } catch (IllegalArgumentException ignored) {

        }
        try {
            FilterLocation.parse("asf");
            fail();
        } catch (IllegalArgumentException ignored) {

        }
    }

    @Test
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
