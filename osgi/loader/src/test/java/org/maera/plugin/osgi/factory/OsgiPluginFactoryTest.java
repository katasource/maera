package org.maera.plugin.osgi.factory;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.*;
import org.maera.plugin.event.impl.DefaultPluginEventManager;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.osgi.container.OsgiContainerManager;
import org.maera.plugin.osgi.container.impl.DefaultOsgiPersistentCache;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.maera.plugin.test.PluginJarBuilder;
import org.maera.plugin.test.PluginTestUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OsgiPluginFactoryTest {

    private OsgiPluginFactory factory;
    private File jar;
    private Mock mockBundle;
    private Mock mockSystemBundle;
    private OsgiContainerManager osgiContainerManager;
    private File tmpDir;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        tmpDir = PluginTestUtils.createTempDirectory(OsgiPluginFactoryTest.class);
        osgiContainerManager = mock(OsgiContainerManager.class);
        mock(ModuleFactory.class);
        factory = new OsgiPluginFactory(PluginAccessor.Descriptor.FILENAME, (String) null, new DefaultOsgiPersistentCache(tmpDir), osgiContainerManager, new DefaultPluginEventManager());
        jar = new PluginJarBuilder("someplugin").addPluginInformation("plugin.key", "My Plugin", "1.0").build();

        mockBundle = new Mock(Bundle.class);
        final Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        dict.put(Constants.BUNDLE_VERSION, "1.0");
        mockBundle.matchAndReturn("getHeaders", dict);

        mockSystemBundle = new Mock(Bundle.class);
        final Dictionary<String, String> sysDict = new Hashtable<String, String>();
        sysDict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        sysDict.put(Constants.BUNDLE_VERSION, "1.0");
        mockSystemBundle.matchAndReturn("getHeaders", sysDict);
        mockSystemBundle.matchAndReturn("getLastModified", System.currentTimeMillis());
        mockSystemBundle.matchAndReturn("getSymbolicName", "system.bundle");

        Mock mockSysContext = new Mock(BundleContext.class);
        mockSystemBundle.matchAndReturn("getBundleContext", mockSysContext.proxy());

        mockSysContext.matchAndReturn("getServiceReference", C.ANY_ARGS, null);
        mockSysContext.matchAndReturn("getService", C.ANY_ARGS, new Mock(PackageAdmin.class).proxy());
    }

    @After
    public void tearDown() throws IOException {
        factory = null;
        FileUtils.cleanDirectory(tmpDir);
        jar.delete();
    }

    @Test
    public void testCanLoadNoXml() throws PluginParseException, IOException {
        final File plugin = new PluginJarBuilder("loadwithxml").build();
        final String key = factory.canCreate(new JarPluginArtifact(plugin));
        assertNull(key);
    }

    @Test
    public void testCanLoadWithXml() throws PluginParseException, IOException {
        final File plugin = new PluginJarBuilder("loadwithxml").addPluginInformation("foo.bar", "", "1.0").build();
        final String key = factory.canCreate(new JarPluginArtifact(plugin));
        assertEquals("foo.bar", key);
    }

    @Test
    public void testCreateOsgiPlugin() throws PluginParseException {
        mockBundle.expectAndReturn("getSymbolicName", "plugin.key");
        when(osgiContainerManager.getHostComponentRegistrations()).thenReturn(new ArrayList<HostComponentRegistration>());
        when(osgiContainerManager.getBundles()).thenReturn(new Bundle[]{(Bundle) mockSystemBundle.proxy()});
        final Plugin plugin = factory.create(new JarPluginArtifact(jar), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
        assertNotNull(plugin);
        assertTrue(plugin instanceof OsgiPlugin);
    }

    @Test
    public void testCreateOsgiPluginWithBadVersion() throws PluginParseException, IOException {
        jar = new PluginJarBuilder("someplugin").addPluginInformation("plugin.key", "My Plugin", "beta.1.0").build();
        mockBundle.expectAndReturn("getSymbolicName", "plugin.key");
        when(osgiContainerManager.getHostComponentRegistrations()).thenReturn(new ArrayList<HostComponentRegistration>());
        when(osgiContainerManager.getBundles()).thenReturn(new Bundle[]{(Bundle) mockSystemBundle.proxy()});
        try {
            factory.create(new JarPluginArtifact(jar), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
            fail("Should have complained about osgi version");
        }
        catch (PluginParseException ignored) {

        }
    }

    @Test
    public void testCreateOsgiPluginWithBadVersion2() throws PluginParseException, IOException {
        jar = new PluginJarBuilder("someplugin").addPluginInformation("plugin.key", "My Plugin", "3.2-rc1").build();
        mockBundle.expectAndReturn("getSymbolicName", "plugin.key");
        when(osgiContainerManager.getHostComponentRegistrations()).thenReturn(new ArrayList<HostComponentRegistration>());
        when(osgiContainerManager.getBundles()).thenReturn(new Bundle[]{(Bundle) mockSystemBundle.proxy()});
        Plugin plugin = factory.create(new JarPluginArtifact(jar), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
        assertTrue(plugin instanceof OsgiPlugin);
    }
}
