package org.maera.plugin.osgi.factory;

import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.osgi.container.OsgiContainerManager;
import org.osgi.framework.Bundle;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OsgiPluginUninstalledHelperTest {
    private String key = "key";
    private OsgiContainerManager mgr;
    private OsgiPluginUninstalledHelper helper;

    @Before
    public void setUp() throws Exception {
        PluginArtifact pluginArtifact = mock(PluginArtifact.class);
        mgr = mock(OsgiContainerManager.class);
        helper = new OsgiPluginUninstalledHelper(key, mgr, pluginArtifact);
    }

    @Test
    public void testInstall() {
        Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put(OsgiPlugin.MAERA_PLUGIN_KEY, key);
        Bundle bundle = mock(Bundle.class);
        when(bundle.getHeaders()).thenReturn(dict);
        when(bundle.getSymbolicName()).thenReturn(key);
        when(mgr.installBundle(null)).thenReturn(bundle);
        assertEquals(bundle, helper.install());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstallDifferentSymbolicName() {
        Dictionary<String, String> dict = new Hashtable<String, String>();
        Bundle bundle = mock(Bundle.class);
        when(bundle.getHeaders()).thenReturn(dict);
        when(bundle.getSymbolicName()).thenReturn("bar");
        when(mgr.installBundle(null)).thenReturn(bundle);
        helper.install();
    }

    @Test
    public void testInstallDifferentSymbolicNameButAltassianKeyFound() {
        Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put(OsgiPlugin.MAERA_PLUGIN_KEY, key);
        Bundle bundle = mock(Bundle.class);
        when(bundle.getHeaders()).thenReturn(dict);
        when(bundle.getSymbolicName()).thenReturn("bar");
        when(mgr.installBundle(null)).thenReturn(bundle);
        helper.install();
    }
}
