package org.maera.plugin.osgi.util;

import junit.framework.TestCase;
import org.maera.plugin.osgi.factory.OsgiPlugin;
import org.maera.plugin.osgi.factory.transform.StubHostComponentRegistration;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.jar.Manifest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestOsgiHeaderUtil extends TestCase {

    public void testFindReferredPackages() throws IOException {
        String foundPackages = OsgiHeaderUtil.findReferredPackages(new ArrayList<HostComponentRegistration>() {{
            add(new StubHostComponentRegistration(OsgiHeaderUtil.class));
        }});

        assertTrue(foundPackages.contains(HostComponentRegistration.class.getPackage().getName()));

    }

    public void testFindReferredPackagesWithVersion() throws IOException {
        String foundPackages = OsgiHeaderUtil.findReferredPackages(new ArrayList<HostComponentRegistration>() {{
            add(new StubHostComponentRegistration(OsgiHeaderUtil.class));
        }}, Collections.singletonMap(HostComponentRegistration.class.getPackage().getName(), "1.0"));

        assertTrue(foundPackages.contains(HostComponentRegistration.class.getPackage().getName() + ";version=1.0"));

    }

    public void testGetPluginKeyBundle() {
        Dictionary headers = new Hashtable();
        headers.put(Constants.BUNDLE_VERSION, "1.0");
        headers.put(Constants.BUNDLE_SYMBOLICNAME, "foo");

        Bundle bundle = mock(Bundle.class);
        when(bundle.getSymbolicName()).thenReturn("foo");
        when(bundle.getHeaders()).thenReturn(headers);

        assertEquals("foo-1.0", OsgiHeaderUtil.getPluginKey(bundle));

        headers.put(OsgiPlugin.MAERA_PLUGIN_KEY, "bar");
        assertEquals("bar", OsgiHeaderUtil.getPluginKey(bundle));
    }

    public void testGetPluginKeyManifest() {
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue(Constants.BUNDLE_VERSION, "1.0");
        mf.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, "foo");

        assertEquals("foo-1.0", OsgiHeaderUtil.getPluginKey(mf));

        mf.getMainAttributes().putValue(OsgiPlugin.MAERA_PLUGIN_KEY, "bar");
        assertEquals("bar", OsgiHeaderUtil.getPluginKey(mf));
    }
}
