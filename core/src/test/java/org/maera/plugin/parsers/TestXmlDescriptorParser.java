package org.maera.plugin.parsers;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.*;
import org.maera.plugin.classloader.PluginClassLoader;
import org.maera.plugin.impl.DefaultDynamicPlugin;
import org.maera.plugin.mock.MockAnimalModuleDescriptor;
import org.maera.plugin.util.ClassLoaderUtils;

import java.io.*;
import java.net.URL;

@SuppressWarnings({"deprecation"})
//suppress deprecation warnings because we still need to test deprecated methods.
public class TestXmlDescriptorParser extends TestCase {
    private static final String MISSING_INFO_TEST_FILE = "test-missing-plugin-info.xml";
    private static final String DUMMY_PLUGIN_FILE = "pooh-test-plugin.jar";

    public TestXmlDescriptorParser(String name) {
        super(name);
    }

    // CONF-12680 Test for missing plugin-info
    public void testMissingPluginInfo() {
        // mock up some supporting objects
        PluginClassLoader classLoader = new PluginClassLoader(new File(getTestFile("ap-plugins") + "/" + DUMMY_PLUGIN_FILE));
        Mock mockFactory = new Mock(ModuleDescriptorFactory.class);
        mockFactory.expect("getModuleDescriptorClass", "unknown-plugin");

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin((PluginArtifact) new Mock(PluginArtifact.class).proxy(), classLoader);

        try {
            XmlDescriptorParser parser = new XmlDescriptorParser(new FileInputStream(getTestFile(MISSING_INFO_TEST_FILE)));
            parser.configurePlugin((ModuleDescriptorFactory) mockFactory.proxy(), testPlugin);

            PluginInformation info = testPlugin.getPluginInformation();
            assertNotNull("Info should not be null", info);
        }
        catch (PluginParseException e) {
            e.printStackTrace();
            fail("Plugin information parsing should not fail.");
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            // This shouldn't happen
            fail("Error setting up test");
        }
    }

    public void testPluginsApplicationVersionMinMax() {
        XmlDescriptorParser parser = parse(null,
                "<atlassian-plugin key='foo'>",
                "  <plugin-info>",
                "    <application-version min='3' max='4' />",
                "  </plugin-info>",
                "</atlassian-plugin>");
        assertEquals(3, (int) parser.getPluginInformation().getMinVersion());
        assertEquals(4, (int) parser.getPluginInformation().getMaxVersion());
    }

    public void testPluginsApplicationVersionMinMaxWithOnlyMin() {
        XmlDescriptorParser parser = parse(null,
                "<atlassian-plugin key='foo'>",
                "  <plugin-info>",
                "    <application-version min='3' />",
                "  </plugin-info>",
                "</atlassian-plugin>");
        assertEquals(3, (int) parser.getPluginInformation().getMinVersion());
        assertEquals(0, (int) parser.getPluginInformation().getMaxVersion());
    }

    public void testPluginsApplicationVersionMinMaxWithOnlyMax() {
        XmlDescriptorParser parser = parse(null,
                "<atlassian-plugin key='foo'>",
                "  <plugin-info>",
                "    <application-version max='3' />",
                "  </plugin-info>",
                "</atlassian-plugin>");
        assertEquals(3, (int) parser.getPluginInformation().getMaxVersion());
        assertEquals(0, (int) parser.getPluginInformation().getMinVersion());
    }

    // Also CONF-12680 test for missing "essential metadata"

    public void testPluginsVersion() {
        String xml = "<atlassian-plugin key=\"foo\" pluginsVersion=\"2\" />";
        XmlDescriptorParser parser = new XmlDescriptorParser(new ByteArrayInputStream(xml.getBytes()));
        assertEquals(2, parser.getPluginsVersion());
    }

    public void testPluginsVersionAfterConfigure() {
        XmlDescriptorParser parser = new XmlDescriptorParser(new ByteArrayInputStream("<atlassian-plugin key=\"foo\" plugins-version=\"2\" />".getBytes()));
        // mock up some supporting objects
        PluginClassLoader classLoader = new PluginClassLoader(new File(getTestFile("ap-plugins") + "/" + DUMMY_PLUGIN_FILE));
        Mock mockFactory = new Mock(ModuleDescriptorFactory.class);
        mockFactory.expect("getModuleDescriptorClass", "unknown-plugin");

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin((PluginArtifact) new Mock(PluginArtifact.class).proxy(), classLoader);
        parser.configurePlugin((ModuleDescriptorFactory) mockFactory.proxy(), testPlugin);
        assertEquals(2, testPlugin.getPluginsVersion());
    }

    public void testPluginWithModules() {
        XmlDescriptorParser parser = parse(null,
                "<atlassian-plugin key='foo'>",
                "  <animal key='bear' />",
                "</atlassian-plugin>");
        // mock up some supporting objects
        PluginClassLoader classLoader = new PluginClassLoader(new File(getTestFile("ap-plugins") + "/" + DUMMY_PLUGIN_FILE));
        Mock mockFactory = new Mock(ModuleDescriptorFactory.class);
        mockFactory.expectAndReturn("getModuleDescriptorClass", C.args(C.eq("animal")), MockAnimalModuleDescriptor.class);

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin((PluginArtifact) new Mock(PluginArtifact.class).proxy(), classLoader);
        parser.configurePlugin((ModuleDescriptorFactory) mockFactory.proxy(), testPlugin);
        assertNotNull(testPlugin.getModuleDescriptor("bear"));
    }

    public void testPluginWithModulesNoApplicationKey() {
        XmlDescriptorParser parser = parse(null,
                "<atlassian-plugin key='foo'>",
                "  <animal key='bear' application='foo'/>",
                "</atlassian-plugin>");
        // mock up some supporting objects
        PluginClassLoader classLoader = new PluginClassLoader(new File(getTestFile("ap-plugins") + "/" + DUMMY_PLUGIN_FILE));
        Mock mockFactory = new Mock(ModuleDescriptorFactory.class);
        mockFactory.expectAndReturn("getModuleDescriptorClass", C.args(C.eq("animal")), MockAnimalModuleDescriptor.class);

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin((PluginArtifact) new Mock(PluginArtifact.class).proxy(), classLoader);
        parser.configurePlugin((ModuleDescriptorFactory) mockFactory.proxy(), testPlugin);
        assertNull(testPlugin.getModuleDescriptor("bear"));
    }

    public void testPluginWithSomeNonApplicationModules() {
        XmlDescriptorParser parser = parse("myapp",
                "<atlassian-plugin key='foo'>",
                "  <animal key='bear' application='myapp'/>",
                "  <animal key='bear2' application='otherapp'/>",
                "</atlassian-plugin>");
        // mock up some supporting objects
        PluginClassLoader classLoader = new PluginClassLoader(new File(getTestFile("ap-plugins") + "/" + DUMMY_PLUGIN_FILE));
        Mock mockFactory = new Mock(ModuleDescriptorFactory.class);
        mockFactory.expectAndReturn("getModuleDescriptorClass", C.args(C.eq("animal")), MockAnimalModuleDescriptor.class);

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin((PluginArtifact) new Mock(PluginArtifact.class).proxy(), classLoader);
        parser.configurePlugin((ModuleDescriptorFactory) mockFactory.proxy(), testPlugin);
        assertNotNull(testPlugin.getModuleDescriptor("bear"));
        assertNull(testPlugin.getModuleDescriptor("bear2"));
    }

    public void testPluginWithSystemAttribute() {
        XmlDescriptorParser parser = parse(null,
                "<atlassian-plugin key='foo' system='true'>",
                "</atlassian-plugin>");

        // mock up some supporting objects
        PluginClassLoader classLoader = new PluginClassLoader(new File(getTestFile("ap-plugins") + "/" + DUMMY_PLUGIN_FILE));
        Mock mockFactory = new Mock(ModuleDescriptorFactory.class);
        mockFactory.expectAndReturn("getModuleDescriptorClass", C.args(C.eq("animal")), MockAnimalModuleDescriptor.class);

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin((PluginArtifact) new Mock(PluginArtifact.class).proxy(), classLoader);
        parser.configurePlugin((ModuleDescriptorFactory) mockFactory.proxy(), testPlugin);
        // PLUG-415 Plugins2 plugins now need to be able to be declared as system.
        assertEquals("This plugin should be a system plugin - bundled plugins2 plugins are system plugins.", true, testPlugin.isSystemPlugin());
    }

    public void testPluginWithoutSystemAttribute() {
        XmlDescriptorParser parser = parse(null,
                "<atlassian-plugin key='foo' >",
                "</atlassian-plugin>");

        // mock up some supporting objects
        PluginClassLoader classLoader = new PluginClassLoader(new File(getTestFile("ap-plugins") + "/" + DUMMY_PLUGIN_FILE));
        Mock mockFactory = new Mock(ModuleDescriptorFactory.class);
        mockFactory.expectAndReturn("getModuleDescriptorClass", C.args(C.eq("animal")), MockAnimalModuleDescriptor.class);

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin((PluginArtifact) new Mock(PluginArtifact.class).proxy(), classLoader);
        parser.configurePlugin((ModuleDescriptorFactory) mockFactory.proxy(), testPlugin);
        assertEquals("This plugin should not be a system plugin.", false, testPlugin.isSystemPlugin());
    }

    public void testPluginsVersionWithDash() {
        String xml = "<atlassian-plugin key=\"foo\" plugins-version=\"2\" />";
        XmlDescriptorParser parser = new XmlDescriptorParser(new ByteArrayInputStream(xml.getBytes()));
        assertEquals(2, parser.getPluginsVersion());
    }

    public void testPluginsVersionMissing() {
        String xml = "<atlassian-plugin key=\"foo\" />";
        XmlDescriptorParser parser = new XmlDescriptorParser(new ByteArrayInputStream(xml.getBytes()));
        assertEquals(1, parser.getPluginsVersion());
    }

    public void testPluginsResourcesAvailableToModuleDescriptors() {
        XmlDescriptorParser parser = parse(null,
                "<atlassian-plugin key='foo'>",
                "  <resource type='velocity' name='edit'>Show an input box here.</resource>",
                "  <animal key='bear' />",
                "</atlassian-plugin>");
        // mock up some supporting objects
        PluginClassLoader classLoader = new PluginClassLoader(new File(getTestFile("ap-plugins") + "/" + DUMMY_PLUGIN_FILE));
        Mock mockFactory = new Mock(ModuleDescriptorFactory.class);
        MockAnimalModuleDescriptor descriptor = new MockAnimalModuleDescriptor("velocity", "edit");
        mockFactory.expectAndReturn("getModuleDescriptor", C.args(C.eq("animal")), descriptor);

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin((PluginArtifact) new Mock(PluginArtifact.class).proxy(), classLoader);
        parser.configurePlugin((ModuleDescriptorFactory) mockFactory.proxy(), testPlugin);
        assertNotNull(testPlugin.getModuleDescriptor("bear"));

        mockFactory.verify();
    }


    private String getTestFile(String filename) {
        final URL url = ClassLoaderUtils.getResource(filename, this.getClass());
        return url.getFile();
    }

    private static XmlDescriptorParser parse(String applicationKey, String... lines) {
        StringBuffer sb = new StringBuffer();
        for (String line : lines) {
            sb.append(line.replace('\'', '"')).append('\n');
        }
        InputStream in = new ByteArrayInputStream(sb.toString().getBytes());
        return new XmlDescriptorParser(in, applicationKey);
    }
}
