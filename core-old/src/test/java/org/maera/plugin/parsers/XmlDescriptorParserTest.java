package org.maera.plugin.parsers;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.junit.Test;
import org.maera.plugin.*;
import org.maera.plugin.classloader.PluginClassLoader;
import org.maera.plugin.impl.DefaultDynamicPlugin;
import org.maera.plugin.mock.MockAnimalModuleDescriptor;
import org.maera.plugin.util.ClassLoaderUtils;

import java.io.*;
import java.net.URL;

import static org.junit.Assert.*;

@SuppressWarnings({"deprecation"})
public class XmlDescriptorParserTest {

    private static final String DUMMY_PLUGIN_FILE = "pooh-test-plugin.jar";
    private static final String MISSING_INFO_TEST_FILE = "test-missing-plugin-info.xml";

    @Test
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

    @Test
    public void testPluginWithModules() {
        XmlDescriptorParser parser = parse(null,
                "<maera-plugin key='foo'>",
                "  <animal key='bear' />",
                "</maera-plugin>");
        // mock up some supporting objects
        PluginClassLoader classLoader = new PluginClassLoader(new File(getTestFile("ap-plugins") + "/" + DUMMY_PLUGIN_FILE));
        Mock mockFactory = new Mock(ModuleDescriptorFactory.class);
        mockFactory.expectAndReturn("getModuleDescriptorClass", C.args(C.eq("animal")), MockAnimalModuleDescriptor.class);

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin((PluginArtifact) new Mock(PluginArtifact.class).proxy(), classLoader);
        parser.configurePlugin((ModuleDescriptorFactory) mockFactory.proxy(), testPlugin);
        assertNotNull(testPlugin.getModuleDescriptor("bear"));
    }

    @Test
    public void testPluginWithModulesNoApplicationKey() {
        XmlDescriptorParser parser = parse(null,
                "<maera-plugin key='foo'>",
                "  <animal key='bear' application='foo'/>",
                "</maera-plugin>");
        // mock up some supporting objects
        PluginClassLoader classLoader = new PluginClassLoader(new File(getTestFile("ap-plugins") + "/" + DUMMY_PLUGIN_FILE));
        Mock mockFactory = new Mock(ModuleDescriptorFactory.class);
        mockFactory.expectAndReturn("getModuleDescriptorClass", C.args(C.eq("animal")), MockAnimalModuleDescriptor.class);

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin((PluginArtifact) new Mock(PluginArtifact.class).proxy(), classLoader);
        parser.configurePlugin((ModuleDescriptorFactory) mockFactory.proxy(), testPlugin);
        assertNull(testPlugin.getModuleDescriptor("bear"));
    }

    @Test
    public void testPluginWithSomeNonApplicationModules() {
        XmlDescriptorParser parser = parse("myapp",
                "<maera-plugin key='foo'>",
                "  <animal key='bear' application='myapp'/>",
                "  <animal key='bear2' application='otherapp'/>",
                "</maera-plugin>");
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

    @Test
    public void testPluginWithSystemAttribute() {
        XmlDescriptorParser parser = parse(null,
                "<maera-plugin key='foo' system='true'>",
                "</maera-plugin>");

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

    @Test
    public void testPluginWithoutSystemAttribute() {
        XmlDescriptorParser parser = parse(null,
                "<maera-plugin key='foo' >",
                "</maera-plugin>");

        // mock up some supporting objects
        PluginClassLoader classLoader = new PluginClassLoader(new File(getTestFile("ap-plugins") + "/" + DUMMY_PLUGIN_FILE));
        Mock mockFactory = new Mock(ModuleDescriptorFactory.class);
        mockFactory.expectAndReturn("getModuleDescriptorClass", C.args(C.eq("animal")), MockAnimalModuleDescriptor.class);

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin((PluginArtifact) new Mock(PluginArtifact.class).proxy(), classLoader);
        parser.configurePlugin((ModuleDescriptorFactory) mockFactory.proxy(), testPlugin);
        assertEquals("This plugin should not be a system plugin.", false, testPlugin.isSystemPlugin());
    }

    @Test
    public void testPluginsApplicationVersionMinMax() {
        XmlDescriptorParser parser = parse(null,
                "<maera-plugin key='foo'>",
                "  <plugin-info>",
                "    <application-version min='3' max='4' />",
                "  </plugin-info>",
                "</maera-plugin>");
        assertEquals(3, (int) parser.getPluginInformation().getMinVersion());
        assertEquals(4, (int) parser.getPluginInformation().getMaxVersion());
    }

    @Test
    public void testPluginsApplicationVersionMinMaxWithOnlyMax() {
        XmlDescriptorParser parser = parse(null,
                "<maera-plugin key='foo'>",
                "  <plugin-info>",
                "    <application-version max='3' />",
                "  </plugin-info>",
                "</maera-plugin>");
        assertEquals(3, (int) parser.getPluginInformation().getMaxVersion());
        assertEquals(0, (int) parser.getPluginInformation().getMinVersion());
    }

    @Test
    public void testPluginsApplicationVersionMinMaxWithOnlyMin() {
        XmlDescriptorParser parser = parse(null,
                "<maera-plugin key='foo'>",
                "  <plugin-info>",
                "    <application-version min='3' />",
                "  </plugin-info>",
                "</maera-plugin>");
        assertEquals(3, (int) parser.getPluginInformation().getMinVersion());
        assertEquals(0, (int) parser.getPluginInformation().getMaxVersion());
    }

    @Test
    public void testPluginsResourcesAvailableToModuleDescriptors() {
        XmlDescriptorParser parser = parse(null,
                "<maera-plugin key='foo'>",
                "  <resource type='velocity' name='edit'>Show an input box here.</resource>",
                "  <animal key='bear' />",
                "</maera-plugin>");
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

    @Test
    public void testPluginsVersion() {
        String xml = "<maera-plugin key=\"foo\" pluginsVersion=\"2\" />";
        XmlDescriptorParser parser = new XmlDescriptorParser(new ByteArrayInputStream(xml.getBytes()));
        assertEquals(2, parser.getPluginsVersion());
    }

    @Test
    public void testPluginsVersionAfterConfigure() {
        XmlDescriptorParser parser = new XmlDescriptorParser(new ByteArrayInputStream("<maera-plugin key=\"foo\" plugins-version=\"2\" />".getBytes()));
        // mock up some supporting objects
        PluginClassLoader classLoader = new PluginClassLoader(new File(getTestFile("ap-plugins") + "/" + DUMMY_PLUGIN_FILE));
        Mock mockFactory = new Mock(ModuleDescriptorFactory.class);
        mockFactory.expect("getModuleDescriptorClass", "unknown-plugin");

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin((PluginArtifact) new Mock(PluginArtifact.class).proxy(), classLoader);
        parser.configurePlugin((ModuleDescriptorFactory) mockFactory.proxy(), testPlugin);
        assertEquals(2, testPlugin.getPluginsVersion());
    }

    @Test
    public void testPluginsVersionMissing() {
        String xml = "<maera-plugin key=\"foo\" />";
        XmlDescriptorParser parser = new XmlDescriptorParser(new ByteArrayInputStream(xml.getBytes()));
        assertEquals(1, parser.getPluginsVersion());
    }

    @Test
    public void testPluginsVersionWithDash() {
        String xml = "<maera-plugin key=\"foo\" plugins-version=\"2\" />";
        XmlDescriptorParser parser = new XmlDescriptorParser(new ByteArrayInputStream(xml.getBytes()));
        assertEquals(2, parser.getPluginsVersion());
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
