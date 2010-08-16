package org.maera.plugin.impl;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.maera.plugin.util.VersionStringComparator;

import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.*;

public class AbstractPluginTest {

    @Test
    public void testCompareTo() {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("bar");

        // foo should be after bar
        assertTrue(p1.compareTo(p2) > 0);
        assertTrue(p2.compareTo(p1) < 0);
    }

    @Test
    public void testCompareToOnVersion() {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        p1.getPluginInformation().setVersion("3.4.1");
        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("foo");
        p2.getPluginInformation().setVersion("3.1.4");

        // v3.4.1 should be after v3.1.4
        assertTrue(p1.compareTo(p2) > 0);
        assertTrue(p2.compareTo(p1) < 0);
    }

    @Test
    public void testCompareToWhenEqual() {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        p1.getPluginInformation().setVersion("3.1.4");
        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("foo");
        p2.getPluginInformation().setVersion("3.1.4");

        // Plugins are "equal" in order
        assertTrue(p1.compareTo(p2) == 0);
        assertTrue(p2.compareTo(p1) == 0);
    }

    @Test
    public void testCompareToWithBothNullKeys() {
        final AbstractPlugin p1 = createAbstractPlugin();
        final AbstractPlugin p2 = createAbstractPlugin();

        assertNull(p1.getKey());
        assertTrue(p1.compareTo(p2) == 0);
        assertTrue(p2.compareTo(p1) == 0);
    }

    @Test
    public void testCompareToWithNullKey() {
        final AbstractPlugin p1 = createAbstractPlugin();
        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("foo");

        // null should be before "foo"
        assertNull(p1.getKey());
        assertTrue(p1.compareTo(p2) < 0);
        assertTrue(p2.compareTo(p1) > 0);
    }

    @Test
    public void testCompareToWithNullPluginInformation() {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        p1.setPluginInformation(null);
        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("foo");

        // p2 has default version (== "0.0")
        assertEquals("0.0", p2.getPluginInformation().getVersion());
        // p1 has null PluginInformation, but the compareTo() will "clean up" this to use version "0", considered equal to "0.0"
        assertTrue(p1.compareTo(p2) == 0);
        assertTrue(p2.compareTo(p1) == 0);
    }

    @Test
    public void testCompareWithBothVersionsInvalid() throws Exception {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        p1.getPluginInformation().setVersion("@$%^#");
        assertFalse(VersionStringComparator.isValidVersionString(p1.getPluginInformation().getVersion()));

        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("foo");
        p2.getPluginInformation().setVersion("!!");

        // The plugins should sort equally
        assertEquals(0, p1.compareTo(p2));
        assertEquals(0, p2.compareTo(p1));
    }

    @Test
    public void testCompareWithInvalidVersion() throws Exception {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        final String invalidVersion = "@$%^#";
        assertFalse(VersionStringComparator.isValidVersionString(invalidVersion));
        p1.getPluginInformation().setVersion(invalidVersion);
        final AbstractPlugin p2 = createAbstractPlugin();
        p2.setKey("foo");
        p2.getPluginInformation().setVersion("3.2");

        // The valid version should be after the invalid version
        assertEquals(-1, p1.compareTo(p2));
        assertEquals(1, p2.compareTo(p1));
    }

    @Test
    public void testGetNameReturnsBlankIfI18nNameKeySpecified() {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        p1.setI18nNameKey("i18n");
        assertTrue(StringUtils.isBlank(p1.getName()));
    }

    @Test
    public void testGetNameReturnsKeyIfBlank() {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("foo");
        assertEquals("foo", p1.getName());
    }

    @Test
    public void testGetNameReturnsSetName() {
        final AbstractPlugin p1 = createAbstractPlugin();
        p1.setKey("key");
        p1.setI18nNameKey("i18n");
        p1.setName("name");
        assertEquals("name", p1.getName());
    }

    private AbstractPlugin createAbstractPlugin() {
        return new AbstractPlugin() {

            @Override
            public boolean isBundledPlugin() {
                return false;
            }

            @Override
            public boolean isUninstallable() {
                return false;
            }

            @Override
            public boolean isDeleteable() {
                return false;
            }

            @Override
            public boolean isDynamicallyLoaded() {
                return false;
            }

            @Override
            public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException {
                return null;
            }

            @Override
            public ClassLoader getClassLoader() {
                return null;
            }

            @Override
            public URL getResource(final String path) {
                return null;
            }

            @Override
            public InputStream getResourceAsStream(final String name) {
                return null;
            }
        };
    }
}
