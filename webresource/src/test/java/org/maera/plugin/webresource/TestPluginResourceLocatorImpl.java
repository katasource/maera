package org.maera.plugin.webresource;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.elements.ResourceDescriptor;
import org.maera.plugin.elements.ResourceLocation;
import org.maera.plugin.servlet.DownloadableClasspathResource;
import org.maera.plugin.servlet.DownloadableResource;
import org.maera.plugin.servlet.ForwardableResource;
import org.maera.plugin.servlet.ServletContextFactory;
import org.maera.plugin.util.PluginUtils;
import org.maera.plugin.webresource.transformer.WebResourceTransformer;
import org.maera.plugin.webresource.transformer.WebResourceTransformerModuleDescriptor;

import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestPluginResourceLocatorImpl extends TestCase {
    private PluginResourceLocatorImpl pluginResourceLocator;
    private Mock mockWebResourceIntegration;
    private Mock mockPluginAccessor;
    private Mock mockServletContextFactory;
    private Mock mockBatchingConfiguration;

    private static final String TEST_PLUGIN_KEY = "test.plugin";
    private static final String TEST_MODULE_KEY = "web-resources";
    private static final String TEST_MODULE_COMPLETE_KEY = TEST_PLUGIN_KEY + ":" + TEST_MODULE_KEY;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mockPluginAccessor = new Mock(PluginAccessor.class);

        mockWebResourceIntegration = new Mock(WebResourceIntegration.class);
        mockWebResourceIntegration.matchAndReturn("getPluginAccessor", mockPluginAccessor.proxy());
        mockServletContextFactory = new Mock(ServletContextFactory.class);
        mockBatchingConfiguration = new Mock(ResourceBatchingConfiguration.class);

        pluginResourceLocator = new PluginResourceLocatorImpl((WebResourceIntegration) mockWebResourceIntegration.proxy(), (ServletContextFactory) mockServletContextFactory
                .proxy(), (ResourceBatchingConfiguration) mockBatchingConfiguration.proxy());
    }

    @Override
    protected void tearDown() throws Exception {
        pluginResourceLocator = null;
        mockWebResourceIntegration = null;
        mockPluginAccessor = null;
        mockServletContextFactory = null;

        super.tearDown();
    }

    public void testIsBatchingOff() {
        try {
            System.clearProperty(PluginResourceLocatorImpl.PLUGIN_WEBRESOURCE_BATCHING_OFF);
            System.setProperty(PluginUtils.ATLASSIAN_DEV_MODE, "true");
            assertTrue(pluginResourceLocator.isBatchingOff());

            System.setProperty(PluginResourceLocatorImpl.PLUGIN_WEBRESOURCE_BATCHING_OFF, "true");
            System.clearProperty(PluginUtils.ATLASSIAN_DEV_MODE);
            assertTrue(pluginResourceLocator.isBatchingOff());

            System.clearProperty(PluginResourceLocatorImpl.PLUGIN_WEBRESOURCE_BATCHING_OFF);
            System.clearProperty(PluginUtils.ATLASSIAN_DEV_MODE);
            assertFalse(pluginResourceLocator.isBatchingOff());

            System.clearProperty(PluginResourceLocatorImpl.PLUGIN_WEBRESOURCE_BATCHING_OFF);
            System.setProperty(PluginUtils.ATLASSIAN_DEV_MODE, "false");
            assertFalse(pluginResourceLocator.isBatchingOff());

            System.setProperty(PluginResourceLocatorImpl.PLUGIN_WEBRESOURCE_BATCHING_OFF, "false");
            System.setProperty(PluginUtils.ATLASSIAN_DEV_MODE, "true");
            assertFalse(pluginResourceLocator.isBatchingOff());
        }
        finally {
            System.clearProperty(PluginResourceLocatorImpl.PLUGIN_WEBRESOURCE_BATCHING_OFF);
            System.clearProperty(PluginUtils.ATLASSIAN_DEV_MODE);
        }
    }

    public void testMatches() {
        assertTrue(pluginResourceLocator.matches("/download/superbatch/css/batch.css"));
        assertTrue(pluginResourceLocator.matches("/download/superbatch/css/images/background/blah.gif"));
        assertTrue(pluginResourceLocator.matches("/download/batch/plugin.key:module-key/plugin.key.js"));
        assertTrue(pluginResourceLocator.matches("/download/resources/plugin.key:module-key/foo.png"));
    }

    public void testNotMatches() {
        assertFalse(pluginResourceLocator.matches("/superbatch/batch.css"));
        assertFalse(pluginResourceLocator.matches("/download/blah.css"));
    }

    public void testGetAndParseUrl() {
        final String url = pluginResourceLocator.getResourceUrl("plugin.key:my-resources", "foo.css");
        assertTrue(pluginResourceLocator.matches(url));
    }

    public void testGetPluginResourcesWithoutBatching() throws Exception {
        final Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getPluginsVersion", 1);

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("master-ie.css", "master.css", "comments.css");

        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), TestUtils.createWebResourceModuleDescriptor(TEST_MODULE_COMPLETE_KEY,
                (Plugin) mockPlugin.proxy(), resourceDescriptors));

        System.setProperty(PluginResourceLocatorImpl.PLUGIN_WEBRESOURCE_BATCHING_OFF, "true");
        try {
            final List<PluginResource> resources = pluginResourceLocator.getPluginResources(TEST_MODULE_COMPLETE_KEY);
            assertEquals(3, resources.size());
            // ensure the resources still have their parameters
            for (final PluginResource resource : resources) {
                if (resource.getResourceName().contains("ie")) {
                    assertEquals("true", resource.getParams().get("ieonly"));
                } else {
                    assertNull(resource.getParams().get("ieonly"));
                }
            }
        }
        finally {
            System.setProperty(PluginResourceLocatorImpl.PLUGIN_WEBRESOURCE_BATCHING_OFF, "false");
        }
    }

    public void testGetPluginResourcesWithBatching() throws Exception {
        final Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getPluginsVersion", 1);

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("master-ie.css", "master.css", "comments.css");

        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), TestUtils.createWebResourceModuleDescriptor(TEST_MODULE_COMPLETE_KEY,
                (Plugin) mockPlugin.proxy(), resourceDescriptors));

        final List<PluginResource> resources = pluginResourceLocator.getPluginResources(TEST_MODULE_COMPLETE_KEY);
        assertEquals(2, resources.size());

        final BatchPluginResource ieBatch = (BatchPluginResource) resources.get(0);
        assertEquals(TEST_MODULE_COMPLETE_KEY, ieBatch.getModuleCompleteKey());
        assertEquals("css", ieBatch.getType());
        assertEquals(1, ieBatch.getParams().size());
        assertEquals("true", ieBatch.getParams().get("ieonly"));

        final BatchPluginResource batch = (BatchPluginResource) resources.get(1);
        assertEquals(TEST_MODULE_COMPLETE_KEY, batch.getModuleCompleteKey());
        assertEquals("css", batch.getType());
        assertEquals(0, batch.getParams().size());
    }

    public void testGetPluginResourcesWithBatchParameter() throws Exception {
        final Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getPluginsVersion", 1);

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("master.css", "comments.css");
        final Map<String, String> nonBatchParams = new TreeMap<String, String>();
        nonBatchParams.put("batch", "false");
        resourceDescriptors.add(TestUtils.createResourceDescriptor("nonbatch.css", nonBatchParams));

        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), TestUtils.createWebResourceModuleDescriptor(TEST_MODULE_COMPLETE_KEY,
                (Plugin) mockPlugin.proxy(), resourceDescriptors));

        final List<PluginResource> resources = pluginResourceLocator.getPluginResources(TEST_MODULE_COMPLETE_KEY);
        assertEquals(2, resources.size());

        final BatchPluginResource batch = (BatchPluginResource) resources.get(0);
        assertEquals(TEST_MODULE_COMPLETE_KEY, batch.getModuleCompleteKey());
        assertEquals("css", batch.getType());
        assertEquals(0, batch.getParams().size());

        final SinglePluginResource single = (SinglePluginResource) resources.get(1);
        assertEquals(TEST_MODULE_COMPLETE_KEY, single.getModuleCompleteKey());
    }

    public void testGetPluginResourcesWithForwarding() throws Exception {
        final Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getPluginsVersion", 1);

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("master.css", "comments.css");
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("source", "webContext");
        resourceDescriptors.add(TestUtils.createResourceDescriptor("forward.css", params));

        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), TestUtils.createWebResourceModuleDescriptor(TEST_MODULE_COMPLETE_KEY,
                (Plugin) mockPlugin.proxy(), resourceDescriptors));

        final List<PluginResource> resources = pluginResourceLocator.getPluginResources(TEST_MODULE_COMPLETE_KEY);
        assertEquals(2, resources.size());

        final BatchPluginResource batch = (BatchPluginResource) resources.get(0);
        assertEquals(TEST_MODULE_COMPLETE_KEY, batch.getModuleCompleteKey());
        assertEquals("css", batch.getType());
        assertEquals(0, batch.getParams().size());

        final SinglePluginResource single = (SinglePluginResource) resources.get(1);
        assertEquals(TEST_MODULE_COMPLETE_KEY, single.getModuleCompleteKey());
    }

    public void testGetForwardableResource() throws Exception {
        final String resourceName = "test.css";
        final String url = "/download/resources/" + TEST_MODULE_COMPLETE_KEY + "/" + resourceName;
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("source", "webContext");

        final Mock mockPlugin = new Mock(Plugin.class);
        final Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.expectAndReturn("getPluginKey", TEST_PLUGIN_KEY);
        mockModuleDescriptor.expectAndReturn("getResourceLocation", C.args(C.eq("download"), C.eq(resourceName)), new ResourceLocation("", resourceName, "download", "text/css",
                "", params));

        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), mockModuleDescriptor.proxy());
        mockPluginAccessor.expectAndReturn("getPlugin", C.args(C.eq(TEST_PLUGIN_KEY)), mockPlugin.proxy());

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.EMPTY_MAP);

        assertTrue(resource instanceof ForwardableResource);
    }

    public void testGetDownloadableClasspathResource() throws Exception {
        final String resourceName = "test.css";
        final String url = "/download/resources/" + TEST_MODULE_COMPLETE_KEY + "/" + resourceName;

        final Mock mockPlugin = new Mock(Plugin.class);
        final Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.expectAndReturn("getPluginKey", TEST_PLUGIN_KEY);
        mockModuleDescriptor.expectAndReturn("getResourceLocation", C.args(C.eq("download"), C.eq(resourceName)), new ResourceLocation("", resourceName, "download", "text/css",
                "", Collections.EMPTY_MAP));

        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), mockModuleDescriptor.proxy());
        mockPluginAccessor.expectAndReturn("getPlugin", C.args(C.eq(TEST_PLUGIN_KEY)), mockPlugin.proxy());

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.EMPTY_MAP);

        assertTrue(resource instanceof DownloadableClasspathResource);
    }

    public void testGetTransformedDownloadableClasspathResource() throws Exception {
        final String resourceName = "test.js";
        final String url = "/download/resources/" + TEST_MODULE_COMPLETE_KEY + "/" + resourceName;

        final DownloadableResource transformedResource = mock(DownloadableResource.class);
        WebResourceTransformation trans = new WebResourceTransformation(DocumentHelper.parseText(
                "<transformation extension=\"js\">\n" +
                        "<transformer key=\"foo\" />\n" +
                        "</transformation>").getRootElement());
        WebResourceTransformer transformer = new WebResourceTransformer() {
            public DownloadableResource transform(Element configElement, ResourceLocation location, String filePath, DownloadableResource nextResource) {
                return transformedResource;
            }
        };

        WebResourceTransformerModuleDescriptor transDescriptor = mock(WebResourceTransformerModuleDescriptor.class);
        when(transDescriptor.getKey()).thenReturn("foo");
        when(transDescriptor.getModule()).thenReturn(transformer);

        final Mock mockPlugin = new Mock(Plugin.class);
        WebResourceModuleDescriptor descriptor = mock(WebResourceModuleDescriptor.class);
        when(descriptor.getPluginKey()).thenReturn(TEST_PLUGIN_KEY);
        when(descriptor.getResourceLocation("download", resourceName)).thenReturn(new ResourceLocation("", resourceName, "download", "text/css",
                "", Collections.EMPTY_MAP));
        when(descriptor.getTransformations()).thenReturn(Arrays.asList(trans));

        mockPluginAccessor.expectAndReturn("getEnabledModuleDescriptorsByClass", C.args(C.eq(WebResourceTransformerModuleDescriptor.class)), Arrays.asList(transDescriptor));
        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), descriptor);
        mockPluginAccessor.expectAndReturn("getPlugin", C.args(C.eq(TEST_PLUGIN_KEY)), mockPlugin.proxy());

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.EMPTY_MAP);

        assertTrue(resource == transformedResource);
    }

    public void testGetUnmatchedTransformDownloadableClasspathResource() throws Exception {
        final String resourceName = "test.css";
        final String url = "/download/resources/" + TEST_MODULE_COMPLETE_KEY + "/" + resourceName;

        final DownloadableResource transformedResource = mock(DownloadableResource.class);
        WebResourceTransformation trans = new WebResourceTransformation(DocumentHelper.parseText(
                "<transformation extension=\"js\">\n" +
                        "<transformer key=\"foo\" />\n" +
                        "</transformation>").getRootElement());
        WebResourceTransformer transformer = new WebResourceTransformer() {
            public DownloadableResource transform(Element configElement, ResourceLocation location, String extraPath, DownloadableResource nextResource) {
                return transformedResource;
            }
        };

        WebResourceTransformerModuleDescriptor transDescriptor = mock(WebResourceTransformerModuleDescriptor.class);
        when(transDescriptor.getKey()).thenReturn("foo");
        when(transDescriptor.getModule()).thenReturn(transformer);

        final Mock mockPlugin = new Mock(Plugin.class);
        WebResourceModuleDescriptor descriptor = mock(WebResourceModuleDescriptor.class);
        when(descriptor.getPluginKey()).thenReturn(TEST_PLUGIN_KEY);
        when(descriptor.getResourceLocation("download", resourceName)).thenReturn(new ResourceLocation("", resourceName, "download", "text/css",
                "", Collections.EMPTY_MAP));
        when(descriptor.getTransformations()).thenReturn(Arrays.asList(trans));

        mockPluginAccessor.expectAndReturn("getEnabledModuleDescriptorsByClass", C.args(C.eq(WebResourceTransformerModuleDescriptor.class)), Arrays.asList(transDescriptor));
        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), descriptor);
        mockPluginAccessor.expectAndReturn("getPlugin", C.args(C.eq(TEST_PLUGIN_KEY)), mockPlugin.proxy());

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.EMPTY_MAP);

        assertTrue(resource instanceof DownloadableClasspathResource);
    }

    public void testGetMissingTransformerDownloadableClasspathResource() throws Exception {
        final String resourceName = "test.css";
        final String url = "/download/resources/" + TEST_MODULE_COMPLETE_KEY + "/" + resourceName;

        final DownloadableResource transformedResource = mock(DownloadableResource.class);
        WebResourceTransformation trans = new WebResourceTransformation(DocumentHelper.parseText(
                "<transformation extension=\"js\">\n" +
                        "<transformer key=\"foo\" />\n" +
                        "</transformation>").getRootElement());
        WebResourceTransformer transformer = new WebResourceTransformer() {
            public DownloadableResource transform(Element configElement, ResourceLocation location, String extraPath, DownloadableResource nextResource) {
                return transformedResource;
            }
        };

        WebResourceTransformerModuleDescriptor transDescriptor = mock(WebResourceTransformerModuleDescriptor.class);
        when(transDescriptor.getKey()).thenReturn("bar");
        when(transDescriptor.getModule()).thenReturn(transformer);

        final Mock mockPlugin = new Mock(Plugin.class);
        WebResourceModuleDescriptor descriptor = mock(WebResourceModuleDescriptor.class);
        when(descriptor.getPluginKey()).thenReturn(TEST_PLUGIN_KEY);
        when(descriptor.getResourceLocation("download", resourceName)).thenReturn(new ResourceLocation("", resourceName, "download", "text/css",
                "", Collections.EMPTY_MAP));
        when(descriptor.getTransformations()).thenReturn(Arrays.asList(trans));

        mockPluginAccessor.expectAndReturn("getEnabledModuleDescriptorsByClass", C.args(C.eq(WebResourceTransformerModuleDescriptor.class)), Arrays.asList(transDescriptor));
        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), descriptor);
        mockPluginAccessor.expectAndReturn("getPlugin", C.args(C.eq(TEST_PLUGIN_KEY)), mockPlugin.proxy());

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.EMPTY_MAP);

        assertTrue(resource instanceof DownloadableClasspathResource);
    }

    public void testGetDownloadableBatchResource() throws Exception {
        final String url = "/download/batch/" + TEST_MODULE_COMPLETE_KEY + "/all.css";
        final String ieResourceName = "master-ie.css";
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("ieonly", "true");

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors(ieResourceName, "master.css");

        final Mock mockPlugin = new Mock(Plugin.class);
        final Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.expectAndReturn("getPluginKey", TEST_PLUGIN_KEY);
        mockModuleDescriptor.expectAndReturn("getCompleteKey", TEST_MODULE_COMPLETE_KEY);
        mockModuleDescriptor.expectAndReturn("getResourceDescriptors", resourceDescriptors);
        mockModuleDescriptor.expectAndReturn("getResourceLocation", C.args(C.eq("download"), C.eq(ieResourceName)), new ResourceLocation("", ieResourceName, "download",
                "text/css", "", Collections.EMPTY_MAP));

        mockPluginAccessor.matchAndReturn("isPluginModuleEnabled", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), Boolean.TRUE);
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), mockModuleDescriptor.proxy());
        mockPluginAccessor.matchAndReturn("getPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), mockModuleDescriptor.proxy());
        mockPluginAccessor.matchAndReturn("getPlugin", C.args(C.eq(TEST_PLUGIN_KEY)), mockPlugin.proxy());

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, params);

        assertTrue(resource instanceof BatchPluginResource);
    }

    public void testGetDownloadableBatchResourceFallbacksToSingle() throws Exception {
        final String resourceName = "images/foo.png";
        final String url = "/download/batch/" + TEST_MODULE_COMPLETE_KEY + "/" + resourceName;

        final Mock mockPlugin = new Mock(Plugin.class);
        final Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.expectAndReturn("getPluginKey", TEST_PLUGIN_KEY);
        mockModuleDescriptor.expectAndReturn("getCompleteKey", TEST_MODULE_COMPLETE_KEY);
        mockModuleDescriptor.expectAndReturn("getResourceDescriptors", Collections.EMPTY_LIST);
        mockModuleDescriptor.expectAndReturn("getResourceLocation", C.args(C.eq("download"), C.eq(resourceName)), new ResourceLocation("", resourceName, "download", "text/css",
                "", Collections.EMPTY_MAP));

        mockPluginAccessor.matchAndReturn("isPluginModuleEnabled", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), Boolean.TRUE);
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), mockModuleDescriptor.proxy());
        mockPluginAccessor.matchAndReturn("getPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), mockModuleDescriptor.proxy());
        mockPluginAccessor.matchAndReturn("getPlugin", C.args(C.eq(TEST_PLUGIN_KEY)), mockPlugin.proxy());

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.EMPTY_MAP);

        assertTrue(resource instanceof DownloadableClasspathResource);
    }

    public void testGetDownloadableSuperBatchResource() throws Exception {
        final String url = "/download/superbatch/css/batch.css";

        final Plugin testPlugin = TestUtils.createTestPlugin(TEST_PLUGIN_KEY, "1");
        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("atlassian.css", "master.css");

        final WebResourceModuleDescriptor webModuleDescriptor = TestUtils.createWebResourceModuleDescriptor(TEST_MODULE_COMPLETE_KEY, testPlugin, resourceDescriptors);

        mockWebResourceIntegration.expectAndReturn("getSuperBatchVersion", "1.0");
        mockBatchingConfiguration.expectAndReturn("isSuperBatchingEnabled", true);
        mockBatchingConfiguration.matchAndReturn("getSuperBatchModuleCompleteKeys", Arrays.asList(TEST_MODULE_COMPLETE_KEY));

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), webModuleDescriptor);
        mockPluginAccessor.matchAndReturn("getPlugin", C.args(C.eq(TEST_PLUGIN_KEY)), testPlugin);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.<String, String>emptyMap());
        assertTrue(resource instanceof SuperBatchPluginResource);

        final SuperBatchPluginResource superBatchPluginResource = (SuperBatchPluginResource) resource;
        assertFalse(superBatchPluginResource.isEmpty());
    }

    public void testGetDownloadableSuperBatchSubResource() throws Exception {
        final String url = "/download/superbatch/css/images/foo.png";
        final String cssResourcesXml = "<resource name=\"css/\" type=\"download\" location=\"css/images/\" />";

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("atlassian.css", "master.css");
        resourceDescriptors.add(new ResourceDescriptor(DocumentHelper.parseText(cssResourcesXml).getRootElement()));

        final Plugin testPlugin = TestUtils.createTestPlugin(TEST_PLUGIN_KEY, "1");
        final WebResourceModuleDescriptor webModuleDescriptor = TestUtils.createWebResourceModuleDescriptor(TEST_MODULE_COMPLETE_KEY, testPlugin, resourceDescriptors);

        mockWebResourceIntegration.expectAndReturn("getSuperBatchVersion", "1.0");
        mockBatchingConfiguration.expectAndReturn("isSuperBatchingEnabled", true);
        mockBatchingConfiguration.matchAndReturn("getSuperBatchModuleCompleteKeys", Arrays.asList(TEST_MODULE_COMPLETE_KEY));

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), webModuleDescriptor);
        mockPluginAccessor.matchAndReturn("getPlugin", C.args(C.eq(TEST_PLUGIN_KEY)), testPlugin);

        final DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.<String, String>emptyMap());
        assertTrue(resource instanceof DownloadableClasspathResource);
    }

    public void testSplitLastPathPart() {
        final String[] parts = pluginResourceLocator.splitLastPathPart("http://localhost:8080/confluence/download/foo/bar/baz");
        assertEquals(2, parts.length);
        assertEquals("http://localhost:8080/confluence/download/foo/bar/", parts[0]);
        assertEquals("baz", parts[1]);

        final String[] anotherParts = pluginResourceLocator.splitLastPathPart(parts[0]);
        assertEquals(2, anotherParts.length);
        assertEquals("http://localhost:8080/confluence/download/foo/", anotherParts[0]);
        assertEquals("bar/", anotherParts[1]);

        assertNull(pluginResourceLocator.splitLastPathPart("noslashes"));
    }
}