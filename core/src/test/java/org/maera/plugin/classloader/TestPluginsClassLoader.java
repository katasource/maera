package org.maera.plugin.classloader;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.MockPlugin;
import org.maera.plugin.MockPluginAccessor;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginAccessor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 *
 */
public class TestPluginsClassLoader extends TestCase {
    private static final String TEST_RESOURCE = "log4j.properties";

    private PluginsClassLoader pluginsClassLoader;
    private Mock mockPluginAccessor;
    private Mock mockPlugin;
    private static final String PLUGIN_KEY = "aPluginKey";
    private static final String TEST_CLASS = "java.lang.String";

    protected void setUp() throws Exception {
        mockPluginAccessor = new Mock(PluginAccessor.class);
        pluginsClassLoader = new PluginsClassLoader((PluginAccessor) mockPluginAccessor.proxy());

        mockPlugin = new Mock(Plugin.class);
    }

    protected void tearDown() throws Exception {
        mockPluginAccessor.verify();
        mockPlugin.verify();

        mockPluginAccessor = null;
        pluginsClassLoader = null;
    }

    public void testFindResourceWhenIndexed() {
        final StubClassLoader stubClassLoader = new StubClassLoader();
        loadPluginResource(stubClassLoader);
        assertTrue(stubClassLoader.getFindResourceNames().contains(TEST_RESOURCE));
        stubClassLoader.clear();

        mockPlugin.expectAndReturn("getKey", PLUGIN_KEY);
        mockPluginAccessor.matchAndReturn("isPluginEnabled", C.args(C.eq(PLUGIN_KEY)), Boolean.TRUE);
        mockPlugin.expectAndReturn("getClassLoader", stubClassLoader);
        pluginsClassLoader.findResource(TEST_RESOURCE);

        assertTrue(stubClassLoader.getFindResourceNames().contains(TEST_RESOURCE));
    }

    public void testFindResourceWhenNotIndexed() {
        final StubClassLoader stubClassLoader = new StubClassLoader();
        loadPluginResource(stubClassLoader);

        assertTrue(stubClassLoader.getFindResourceNames().contains(TEST_RESOURCE));
    }

    public void testFindResourceWhenIndexedAndPluginDisabled() {
        final StubClassLoader stubClassLoader = new StubClassLoader();
        loadPluginResource(stubClassLoader);
        assertTrue(stubClassLoader.getFindResourceNames().contains(TEST_RESOURCE));
        stubClassLoader.clear();

        mockPluginAccessor.expectAndReturn("getEnabledPlugins", Collections.emptyList());
        mockPlugin.expectAndReturn("getKey", PLUGIN_KEY);
        mockPluginAccessor.matchAndReturn("isPluginEnabled", C.args(C.eq(PLUGIN_KEY)), Boolean.FALSE);
        pluginsClassLoader.findResource(TEST_RESOURCE);

        assertFalse(stubClassLoader.getFindResourceNames().contains(TEST_RESOURCE));
    }

    public void testFindClassWhenIndexed() throws Exception {
        final StubClassLoader stubClassLoader = new StubClassLoader();
        loadPluginClass(stubClassLoader);
        assertTrue(stubClassLoader.getFindClassNames().contains(TEST_CLASS));
        stubClassLoader.clear();

        mockPlugin.expectAndReturn("getKey", PLUGIN_KEY);
        mockPluginAccessor.matchAndReturn("isPluginEnabled", C.args(C.eq(PLUGIN_KEY)), Boolean.TRUE);
        mockPlugin.expectAndReturn("getClassLoader", stubClassLoader);
        pluginsClassLoader.findClass(TEST_CLASS);

        assertTrue(stubClassLoader.getFindClassNames().contains(TEST_CLASS));
    }

    public void testFindClassWhenNotIndexed() throws Exception {
        final StubClassLoader stubClassLoader = new StubClassLoader();
        loadPluginClass(stubClassLoader);

        assertTrue(stubClassLoader.getFindClassNames().contains(TEST_CLASS));
    }

    public void testFindClassWhenIndexedAndPluginDisabled() throws Exception {
        final StubClassLoader stubClassLoader = new StubClassLoader();
        loadPluginClass(stubClassLoader);
        assertTrue(stubClassLoader.getFindClassNames().contains(TEST_CLASS));
        stubClassLoader.clear();

        mockPluginAccessor.expectAndReturn("getEnabledPlugins", Collections.emptyList());
        mockPlugin.expectAndReturn("getKey", PLUGIN_KEY);
        mockPluginAccessor.matchAndReturn("isPluginEnabled", C.args(C.eq(PLUGIN_KEY)), Boolean.FALSE);
        try {
            pluginsClassLoader.findClass(TEST_CLASS);
            fail("Plugin is disabled so its ClassLoader should throw ClassNotFoundException");
        }
        catch (ClassNotFoundException e) {
            // good
        }
    }

    public void testGetPluginForClass() throws Exception {
        final MockPluginAccessor mockPluginAccessor = new MockPluginAccessor();
        PluginsClassLoader pluginsClassLoader = new PluginsClassLoader(mockPluginAccessor);
        // Set up plugin A
        MockClassLoader mockClassLoaderA = new MockClassLoader();
        mockClassLoaderA.register("com.acme.Ant", String.class);
        mockClassLoaderA.register("com.acme.Clash", String.class);
        MockPlugin pluginA = new MockPlugin("A", mockClassLoaderA);
        mockPluginAccessor.addPlugin(pluginA);
        // Set up plugin B
        MockClassLoader mockClassLoaderB = new MockClassLoader();
        mockClassLoaderB.register("com.acme.Bat", String.class);
        mockClassLoaderB.register("com.acme.Clash", String.class);
        MockPlugin pluginB = new MockPlugin("B", mockClassLoaderB);
        mockPluginAccessor.addPlugin(pluginB);

        // With both plugins disabled, we should get Clash from no-one
        assertEquals(null, pluginsClassLoader.getPluginForClass("com.acme.Ant"));
        assertEquals(null, pluginsClassLoader.getPluginForClass("com.acme.Bat"));
        assertEquals(null, pluginsClassLoader.getPluginForClass("com.acme.Clash"));
        assertEquals(null, pluginsClassLoader.getPluginForClass("java.lang.String"));

        // Enable PluginB and it should give us Bat and Clash from pluginB
        pluginB.enable();
        pluginsClassLoader.notifyPluginOrModuleEnabled();
        assertEquals(null, pluginsClassLoader.getPluginForClass("com.acme.Ant"));
        assertEquals(pluginB, pluginsClassLoader.getPluginForClass("com.acme.Bat"));
        assertEquals(pluginB, pluginsClassLoader.getPluginForClass("com.acme.Clash"));
        assertEquals(null, pluginsClassLoader.getPluginForClass("java.lang.String"));

        // Enable PluginA and it should give us Clash from pluginB (because it is cached).
        pluginA.enable();
        pluginsClassLoader.notifyPluginOrModuleEnabled();
        assertEquals(pluginA, pluginsClassLoader.getPluginForClass("com.acme.Ant"));
        assertEquals(pluginB, pluginsClassLoader.getPluginForClass("com.acme.Bat"));
        assertEquals(pluginB, pluginsClassLoader.getPluginForClass("com.acme.Clash"));
        assertEquals(null, pluginsClassLoader.getPluginForClass("java.lang.String"));

        // flush the cache and we get Clash from plugin A instead (because it is earlier in the list).
        pluginsClassLoader.notifyUninstallPlugin(pluginB);
        assertEquals(pluginA, pluginsClassLoader.getPluginForClass("com.acme.Ant"));
        assertEquals(pluginB, pluginsClassLoader.getPluginForClass("com.acme.Bat"));
        assertEquals(pluginA, pluginsClassLoader.getPluginForClass("com.acme.Clash"));
        assertEquals(null, pluginsClassLoader.getPluginForClass("java.lang.String"));
    }

    private void loadPluginResource(ClassLoader stubClassLoader) {
        mockPluginAccessor.expectAndReturn("getEnabledPlugins", Collections.singleton(mockPlugin.proxy()));
        mockPlugin.expectAndReturn("getClassLoader", stubClassLoader);
        pluginsClassLoader.findResource(TEST_RESOURCE);
    }

    private void loadPluginClass(ClassLoader stubClassLoader) throws ClassNotFoundException {
        mockPluginAccessor.expectAndReturn("getEnabledPlugins", Collections.singleton(mockPlugin.proxy()));
        mockPlugin.expectAndReturn("getClassLoader", stubClassLoader);
        pluginsClassLoader.findClass(TEST_CLASS);
    }

    private static final class StubClassLoader extends AbstractClassLoader {
        private final Collection<String> findResourceNames = new LinkedList<String>();

        public Collection getFindClassNames() {
            return findClassNames;
        }

        private final Collection<String> findClassNames = new LinkedList<String>();

        public StubClassLoader() {
            super(null); // no parent classloader needed for tests
        }

        protected URL findResource(String name) {
            findResourceNames.add(name);
            try {
                return new URL("file://" + name);
            }
            catch (MalformedURLException e) {
                // ignore
                return null;
            }
        }

        /**
         * override the default behavior to bypass the system class loader
         * for tests
         */
        public Class loadClass(String name) throws ClassNotFoundException {
            return findClass(name);
        }

        protected Class findClass(String className) throws ClassNotFoundException {
            findClassNames.add(className);
            return String.class;
        }

        public Collection getFindResourceNames() {
            return findResourceNames;
        }

        public void clear() {
            findResourceNames.clear();
            findClassNames.clear();
        }
    }
}
