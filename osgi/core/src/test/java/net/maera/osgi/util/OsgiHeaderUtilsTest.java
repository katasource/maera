package net.maera.osgi.util;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.jar.Manifest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 0.1
 */
public class OsgiHeaderUtilsTest {

    /*@Test
    public void testFindReferredPackages() throws IOException {
        String foundPackages = OsgiHeaderUtils.findReferredPackages(new ArrayList<HostComponentRegistration>() {{
            add(new StubHostComponentRegistration(OsgiHeaderUtil.class));
        }});
        assertTrue(foundPackages.contains(HostComponentRegistration.class.getPackage().getName()));
    }

    @Test
    public void testFindReferredPackagesWithVersion() throws IOException {
        String foundPackages = OsgiHeaderUtils.findReferredPackages(new ArrayList<HostComponentRegistration>() {{

            add(new StubHostComponentRegistration(OsgiHeaderUtil.class));
        }}, Collections.singletonMap(HostComponentRegistration.class.getPackage().getName(), "1.0"));

        assertTrue(foundPackages.contains(HostComponentRegistration.class.getPackage().getName() + ";version=1.0"));
    }*/

    @Test
    public void testGetPluginKeyBundle() {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(Constants.BUNDLE_VERSION, "1.0");
        headers.put(Constants.BUNDLE_SYMBOLICNAME, "foo");

        Bundle bundle = mock(Bundle.class);
        when(bundle.getSymbolicName()).thenReturn("foo");
        when(bundle.getHeaders()).thenReturn(headers);

        assertEquals("foo-1.0", OsgiHeaderUtils.getPluginKey(bundle));

        headers.put(OsgiHeaderUtils.MAERA_PLUGIN_KEY, "bar");
        assertEquals("bar", OsgiHeaderUtils.getPluginKey(bundle));
    }

    @Test
    public void testGetPluginKeyManifest() {
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue(Constants.BUNDLE_VERSION, "1.0");
        mf.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, "foo");

        assertEquals("foo-1.0", OsgiHeaderUtils.getPluginKey(mf));

        mf.getMainAttributes().putValue(OsgiHeaderUtils.MAERA_PLUGIN_KEY, "bar");
        assertEquals("bar", OsgiHeaderUtils.getPluginKey(mf));
    }

}
