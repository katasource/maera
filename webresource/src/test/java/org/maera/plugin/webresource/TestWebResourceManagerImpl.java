package org.maera.plugin.webresource;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.dom4j.DocumentException;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.elements.ResourceDescriptor;
import org.maera.plugin.impl.StaticPlugin;
import org.maera.plugin.servlet.AbstractFileServerServlet;

import java.io.StringWriter;
import java.util.*;

public class TestWebResourceManagerImpl extends TestCase {

    private Mock mockWebResourceIntegration;
    private Mock mockPluginAccessor;
    private WebResourceManagerImpl webResourceManager;
    private PluginResourceLocator pluginResourceLocator;
    private TestResourceBatchingConfiguration resourceBatchingConfiguration;
    private Plugin testPlugin;

    private static final String ANIMAL_PLUGIN_VERSION = "2";
    private static final String BASEURL = "http://www.foo.com";
    private static final String SYSTEM_COUNTER = "123";
    private static final String SYSTEM_BUILD_NUMBER = "650";

    protected void setUp() throws Exception {
        super.setUp();

        mockPluginAccessor = new Mock(PluginAccessor.class);

        mockWebResourceIntegration = new Mock(WebResourceIntegration.class);
        mockWebResourceIntegration.matchAndReturn("getPluginAccessor", mockPluginAccessor.proxy());

        resourceBatchingConfiguration = new TestResourceBatchingConfiguration();
        pluginResourceLocator = new PluginResourceLocatorImpl((WebResourceIntegration) mockWebResourceIntegration.proxy(), null);
        webResourceManager = new WebResourceManagerImpl(pluginResourceLocator, (WebResourceIntegration) mockWebResourceIntegration.proxy(), resourceBatchingConfiguration);


        mockWebResourceIntegration.matchAndReturn("getBaseUrl", BASEURL);
        mockWebResourceIntegration.matchAndReturn("getBaseUrl", C.args(C.eq(UrlMode.ABSOLUTE)), BASEURL);
        mockWebResourceIntegration.matchAndReturn("getBaseUrl", C.args(C.eq(UrlMode.RELATIVE)), "");
        mockWebResourceIntegration.matchAndReturn("getBaseUrl", C.args(C.eq(UrlMode.AUTO)), "");
        mockWebResourceIntegration.matchAndReturn("getSystemBuildNumber", SYSTEM_BUILD_NUMBER);
        mockWebResourceIntegration.matchAndReturn("getSystemCounter", SYSTEM_COUNTER);
        mockWebResourceIntegration.matchAndReturn("getSuperBatchVersion", "12");

        testPlugin = TestUtils.createTestPlugin();
    }

    protected void tearDown() throws Exception {
        webResourceManager = null;
        mockPluginAccessor = null;
        mockWebResourceIntegration = null;
        testPlugin = null;
        resourceBatchingConfiguration = null;

        super.tearDown();
    }

    public void testRequireResources() {
        String resource1 = "test.maera:cool-stuff";
        String resource2 = "test.maera:hot-stuff";

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resource1)),
                TestUtils.createWebResourceModuleDescriptor(resource1, testPlugin));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resource2)),
                TestUtils.createWebResourceModuleDescriptor(resource2, testPlugin));

        Map requestCache = setupRequestCache();
        webResourceManager.requireResource(resource1);
        webResourceManager.requireResource(resource2);
        webResourceManager.requireResource(resource1); // require again to test it only gets included once

        Collection resources = (Collection) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(2, resources.size());
        assertTrue(resources.contains(resource1));
        assertTrue(resources.contains(resource2));
    }

    public void testRequireResourcesWithDependencies() {
        String resource = "test.maera:cool-stuff";
        String dependencyResource = "test.maera:hot-stuff";

        // cool-stuff depends on hot-stuff
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resource)),
                TestUtils.createWebResourceModuleDescriptor(resource, testPlugin, Collections.<ResourceDescriptor>emptyList(), Collections.singletonList(dependencyResource)));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(dependencyResource)),
                TestUtils.createWebResourceModuleDescriptor(dependencyResource, testPlugin));

        Map requestCache = setupRequestCache();
        webResourceManager.requireResource(resource);

        Collection resources = (Collection) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(2, resources.size());
        Object[] resourceArray = resources.toArray();
        assertEquals(dependencyResource, resourceArray[0]);
        assertEquals(resource, resourceArray[1]);
    }

    public void testRequireResourcesWithCyclicDependency() {
        String resource1 = "test.maera:cool-stuff";
        String resource2 = "test.maera:hot-stuff";

        // cool-stuff and hot-stuff depend on each other
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resource1)),
                TestUtils.createWebResourceModuleDescriptor(resource1, testPlugin, Collections.<ResourceDescriptor>emptyList(), Collections.singletonList(resource2)));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resource2)),
                TestUtils.createWebResourceModuleDescriptor(resource2, testPlugin, Collections.<ResourceDescriptor>emptyList(), Collections.singletonList(resource1)));

        Map requestCache = setupRequestCache();
        webResourceManager.requireResource(resource1);

        Collection resources = (Collection) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(2, resources.size());
        Object[] resourceArray = resources.toArray();
        assertEquals(resource2, resourceArray[0]);
        assertEquals(resource1, resourceArray[1]);
    }

    public void testRequireResourcesWithComplexCyclicDependency() {
        String resourceA = "test.maera:a";
        String resourceB = "test.maera:b";
        String resourceC = "test.maera:c";
        String resourceD = "test.maera:d";
        String resourceE = "test.maera:e";

        // A depends on B, C
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceA)),
                TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin, Collections.<ResourceDescriptor>emptyList(), Arrays.asList(resourceB, resourceC)));
        // B depends on D
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceB)),
                TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin, Collections.<ResourceDescriptor>emptyList(), Collections.singletonList(resourceD)));
        // C has no dependencies
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceC)),
                TestUtils.createWebResourceModuleDescriptor(resourceC, testPlugin));
        // D depends on E, A (cyclic dependency)
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceD)),
                TestUtils.createWebResourceModuleDescriptor(resourceD, testPlugin, Collections.<ResourceDescriptor>emptyList(), Arrays.asList(resourceE, resourceA)));
        // E has no dependencies
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceE)),
                TestUtils.createWebResourceModuleDescriptor(resourceE, testPlugin));

        Map requestCache = setupRequestCache();
        webResourceManager.requireResource(resourceA);
        // requiring a resource already included by A's dependencies shouldn't change the order
        webResourceManager.requireResource(resourceD);

        Collection resources = (Collection) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(5, resources.size());
        Object[] resourceArray = resources.toArray();
        assertEquals(resourceE, resourceArray[0]);
        assertEquals(resourceD, resourceArray[1]);
        assertEquals(resourceB, resourceArray[2]);
        assertEquals(resourceC, resourceArray[3]);
        assertEquals(resourceA, resourceArray[4]);
    }

    public void testGetResourceContext() throws Exception {
        final String resourceA = "test.maera:a";
        final String resourceB = "test.maera:b";
        final String resourceC = "test.maera:c";

        final WebResourceModuleDescriptor descriptor1 = TestUtils.createWebResourceModuleDescriptor(
                resourceA, testPlugin, TestUtils.createResourceDescriptors("resourceA.css"), Collections.<String>emptyList(), Collections.<String>emptySet());
        final WebResourceModuleDescriptor descriptor2 = TestUtils.createWebResourceModuleDescriptor(
                resourceB, testPlugin, TestUtils.createResourceDescriptors("resourceB.css"), Collections.<String>emptyList(), new HashSet<String>() {{
                    add("foo");
                }});
        final WebResourceModuleDescriptor descriptor3 = TestUtils.createWebResourceModuleDescriptor(
                resourceC, testPlugin, TestUtils.createResourceDescriptors("resourceC.css"), Collections.<String>emptyList(), new HashSet<String>() {{
                    add("foo");
                    add("bar");
                }});

        final List<WebResourceModuleDescriptor> descriptors = Arrays.asList(descriptor1, descriptor2, descriptor3);

        mockPluginAccessor.matchAndReturn("getEnabledModuleDescriptorsByClass", C.args(C.eq(WebResourceModuleDescriptor.class)), descriptors);
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceA)), descriptor1);
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceB)), descriptor2);
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceC)), descriptor3);

        setupRequestCache();

        // write includes for all resources for "foo":
        webResourceManager.requireResourcesForContext("foo");
        StringWriter writer = new StringWriter();
        webResourceManager.includeResources(writer);
        String resources = writer.toString();
        assertFalse(resources.contains(resourceA + ".css"));
        assertTrue(resources.contains(resourceB + ".css"));
        assertTrue(resources.contains(resourceC + ".css"));

        // write includes for all resources for "bar":
        webResourceManager.requireResourcesForContext("bar");
        writer = new StringWriter();
        webResourceManager.includeResources(writer);
        resources = writer.toString();
        assertFalse(resources.contains(resourceA + ".css"));
        assertFalse(resources.contains(resourceB + ".css"));
        assertTrue(resources.contains(resourceC + ".css"));
    }

    private Map setupRequestCache() {
        Map requestCache = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requestCache);
        return requestCache;
    }

    public void testRequireResourceWithDuplicateDependencies() {
        String resourceA = "test.maera:a";
        String resourceB = "test.maera:b";
        String resourceC = "test.maera:c";
        String resourceD = "test.maera:d";
        String resourceE = "test.maera:e";

        // A depends on B, C
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceA)),
                TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin, Collections.<ResourceDescriptor>emptyList(), Arrays.asList(resourceB, resourceC)));
        // B depends on D
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceB)),
                TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin, Collections.<ResourceDescriptor>emptyList(), Collections.singletonList(resourceD)));
        // C depends on E
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceC)),
                TestUtils.createWebResourceModuleDescriptor(resourceC, testPlugin, Collections.<ResourceDescriptor>emptyList(), Collections.singletonList(resourceE)));
        // D depends on C
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceD)),
                TestUtils.createWebResourceModuleDescriptor(resourceD, testPlugin, Collections.<ResourceDescriptor>emptyList(), Collections.singletonList(resourceC)));
        // E has no dependencies
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceE)),
                TestUtils.createWebResourceModuleDescriptor(resourceE, testPlugin));

        Map requestCache = setupRequestCache();
        webResourceManager.requireResource(resourceA);

        Collection resources = (Collection) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(5, resources.size());
        Object[] resourceArray = resources.toArray();
        assertEquals(resourceE, resourceArray[0]);
        assertEquals(resourceC, resourceArray[1]);
        assertEquals(resourceD, resourceArray[2]);
        assertEquals(resourceB, resourceArray[3]);
        assertEquals(resourceA, resourceArray[4]);
    }

    public void testRequireSingleResourceGetsDeps() throws Exception {
        String resourceA = "test.maera:a";
        String resourceB = "test.maera:b";

        final List<ResourceDescriptor> resourceDescriptorsA = TestUtils.createResourceDescriptors("resourceA.css");
        final List<ResourceDescriptor> resourceDescriptorsB = TestUtils.createResourceDescriptors("resourceB.css", "resourceB-more.css");

        // A depends on B
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceA)),
                TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin, resourceDescriptorsA, Collections.singletonList(resourceB)));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceB)),
                TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin, resourceDescriptorsB, Collections.<String>emptyList()));

        String s = webResourceManager.getResourceTags(resourceA);
        int indexA = s.indexOf(resourceA);
        int indexB = s.indexOf(resourceB);

        assertNotSame(-1, indexA);
        assertNotSame(-1, indexB);
        assertTrue(indexB < indexA);
    }

    public void testIncludeResourcesWithResourceList() throws Exception {
        String resourceA = "test.maera:a";

        final List<ResourceDescriptor> resourceDescriptorsA = TestUtils.createResourceDescriptors("resourceA.css");

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceA)),
                TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin, resourceDescriptorsA, Collections.<String>emptyList()));

        StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.includeResources(Arrays.<String>asList(resourceA), requiredResourceWriter, UrlMode.ABSOLUTE);
        String result = requiredResourceWriter.toString();
        assertTrue(result.contains(resourceA));
    }

    public void testIncludeResourcesWithResourceListIgnoresRequireResource() throws Exception {
        String resourceA = "test.maera:a";
        String resourceB = "test.maera:b";

        final List<ResourceDescriptor> resourceDescriptorsA = TestUtils.createResourceDescriptors("resourceA.css");
        final List<ResourceDescriptor> resourceDescriptorsB = TestUtils.createResourceDescriptors("resourceB.css", "resourceB-more.css");

        final Map requestCache = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requestCache);


        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceA)),
                TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin, resourceDescriptorsA, Collections.<String>emptyList()));

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceB)),
                TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin, resourceDescriptorsB, Collections.<String>emptyList()));


        StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.requireResource(resourceB);
        webResourceManager.includeResources(Arrays.<String>asList(resourceA), requiredResourceWriter, UrlMode.ABSOLUTE);
        String result = requiredResourceWriter.toString();
        assertFalse(result.contains(resourceB));
    }


    public void testIncludeResourcesWithResourceListIncludesDependences() throws Exception {
        String resourceA = "test.maera:a";
        String resourceB = "test.maera:b";

        final List<ResourceDescriptor> resourceDescriptorsA = TestUtils.createResourceDescriptors("resourceA.css");
        final List<ResourceDescriptor> resourceDescriptorsB = TestUtils.createResourceDescriptors("resourceB.css", "resourceB-more.css");

        // A depends on B
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceA)),
                TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin, resourceDescriptorsA, Collections.singletonList(resourceB)));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceB)),
                TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin, resourceDescriptorsB, Collections.<String>emptyList()));

        StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.includeResources(Arrays.<String>asList(resourceA), requiredResourceWriter, UrlMode.ABSOLUTE);
        String result = requiredResourceWriter.toString();
        assertTrue(result.contains(resourceB));
    }

    public void testIncludeResourcesWithResourceListIncludesDependencesFromSuperBatch() throws Exception {
        final String resourceA = "test.maera:a";
        final String resourceB = "test.maera:b";

        ResourceBatchingConfiguration batchingConfiguration = new ResourceBatchingConfiguration() {
            public boolean isSuperBatchingEnabled() {
                return true;
            }

            public List<String> getSuperBatchModuleCompleteKeys() {
                return Arrays.asList(resourceB);
            }
        };

        webResourceManager = new WebResourceManagerImpl(pluginResourceLocator, (WebResourceIntegration) mockWebResourceIntegration.proxy(), batchingConfiguration);

        final List<ResourceDescriptor> resourceDescriptorsA = TestUtils.createResourceDescriptors("resourceA.css");
        final List<ResourceDescriptor> resourceDescriptorsB = TestUtils.createResourceDescriptors("resourceB.css");

        // A depends on B
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceA)),
                TestUtils.createWebResourceModuleDescriptor(resourceA, testPlugin, resourceDescriptorsA, Collections.singletonList(resourceB)));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(resourceB)),
                TestUtils.createWebResourceModuleDescriptor(resourceB, testPlugin, resourceDescriptorsB, Collections.<String>emptyList()));

        StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.includeResources(Arrays.<String>asList(resourceA), requiredResourceWriter, UrlMode.ABSOLUTE);
        String result = requiredResourceWriter.toString();
        assertTrue(result.contains(resourceB));
    }

    public void testRequireResourcesAreClearedAfterIncludesResourcesIsCalled() throws Exception {
        final String moduleKey = "cool-resources";
        final String completeModuleKey = "test.maera:" + moduleKey;

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("cool.css", "more-cool.css", "cool.js");

        setupRequestCache();

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey)),
                TestUtils.createWebResourceModuleDescriptor(completeModuleKey, testPlugin, resourceDescriptors1));

        // test requireResource() methods
        webResourceManager.requireResource(completeModuleKey);
        webResourceManager.includeResources(new StringWriter(), UrlMode.RELATIVE);

        StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.includeResources(requiredResourceWriter, UrlMode.RELATIVE);
        assertEquals("", requiredResourceWriter.toString());
    }

    // testRequireResourceAndResourceTagMethods

    public void testRequireResourceAndResourceTagMethods() throws Exception {
        final String completeModuleKey = "test.maera:cool-resources";
        final String staticBase = setupRequireResourceAndResourceTagMethods(false, completeModuleKey);

        // test requireResource() methods
        StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.requireResource(completeModuleKey);
        String requiredResourceResult = webResourceManager.getRequiredResources(UrlMode.RELATIVE);
        webResourceManager.includeResources(requiredResourceWriter, UrlMode.RELATIVE);
        assertEquals(requiredResourceResult, requiredResourceWriter.toString());

        assertTrue(requiredResourceResult.contains("href=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".css"));
        assertTrue(requiredResourceResult.contains("src=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".js"));

        // test resourceTag() methods
        StringWriter resourceTagsWriter = new StringWriter();
        webResourceManager.requireResource(completeModuleKey, resourceTagsWriter, UrlMode.RELATIVE);
        String resourceTagsResult = webResourceManager.getResourceTags(completeModuleKey, UrlMode.RELATIVE);
        assertEquals(resourceTagsResult, resourceTagsWriter.toString());

        // calling requireResource() or resourceTag() on a single webresource should be the same
        assertEquals(requiredResourceResult, resourceTagsResult);
    }

    public void testRequireResourceAndResourceTagMethodsWithAbsoluteUrlMode() throws Exception {
        testRequireResourceAndResourceTagMethods(UrlMode.ABSOLUTE, true);
    }

    public void testRequireResourceAndResourceTagMethodsWithRelativeUrlMode() throws Exception {
        testRequireResourceAndResourceTagMethods(UrlMode.RELATIVE, false);
    }

    public void testRequireResourceAndResourceTagMethodsWithAutoUrlMode() throws Exception {
        testRequireResourceAndResourceTagMethods(UrlMode.AUTO, false);
    }

    private void testRequireResourceAndResourceTagMethods(UrlMode urlMode, boolean baseUrlExpected) throws Exception {
        final String completeModuleKey = "test.maera:cool-resources";
        final String staticBase = setupRequireResourceAndResourceTagMethods(baseUrlExpected, completeModuleKey);

        // test requireResource() methods
        final StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.requireResource(completeModuleKey);
        final String requiredResourceResult = webResourceManager.getRequiredResources(urlMode);
        webResourceManager.includeResources(requiredResourceWriter, urlMode);
        assertEquals(requiredResourceResult, requiredResourceWriter.toString());

        assertTrue(requiredResourceResult.contains("href=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".css"));
        assertTrue(requiredResourceResult.contains("src=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".js"));

        // test resourceTag() methods
        StringWriter resourceTagsWriter = new StringWriter();
        webResourceManager.requireResource(completeModuleKey, resourceTagsWriter, urlMode);
        String resourceTagsResult = webResourceManager.getResourceTags(completeModuleKey, urlMode);
        assertEquals(resourceTagsResult, resourceTagsWriter.toString());

        // calling requireResource() or resourceTag() on a single webresource should be the same
        assertEquals(requiredResourceResult, resourceTagsResult);
    }

    private String setupRequireResourceAndResourceTagMethods(boolean baseUrlExpected, String completeModuleKey)
            throws DocumentException {
        final List<ResourceDescriptor> descriptors = TestUtils.createResourceDescriptors("cool.css", "more-cool.css", "cool.js");

        final Map requestCache = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requestCache);

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey)),
                TestUtils.createWebResourceModuleDescriptor(completeModuleKey, testPlugin, descriptors));

        return (baseUrlExpected ? BASEURL : "") +
                "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER +
                "/" + testPlugin.getPluginInformation().getVersion() + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX + BatchPluginResource.URL_PREFIX;
    }

    // testRequireResourceWithCacheParameter

    public void testRequireResourceWithCacheParameter() throws Exception {
        final String completeModuleKey = "test.maera:no-cache-resources";

        final String expectedResult = setupRequireResourceWithCacheParameter(false, completeModuleKey);
        assertTrue(webResourceManager.getResourceTags(completeModuleKey).contains(expectedResult));
    }

    public void testRequireResourceWithCacheParameterAndAbsoluteUrlMode() throws Exception {
        testRequireResourceWithCacheParameter(UrlMode.ABSOLUTE, true);
    }

    public void testRequireResourceWithCacheParameterAndRelativeUrlMode() throws Exception {
        testRequireResourceWithCacheParameter(UrlMode.RELATIVE, false);
    }

    public void testRequireResourceWithCacheParameterAndAutoUrlMode() throws Exception {
        testRequireResourceWithCacheParameter(UrlMode.AUTO, false);
    }

    private void testRequireResourceWithCacheParameter(UrlMode urlMode, boolean baseUrlExpected) throws Exception {
        final String completeModuleKey = "test.maera:no-cache-resources";
        final String expectedResult = setupRequireResourceWithCacheParameter(baseUrlExpected, completeModuleKey);
        assertTrue(webResourceManager.getResourceTags(completeModuleKey, urlMode).contains(expectedResult));
    }

    private String setupRequireResourceWithCacheParameter(boolean baseUrlExpected, String completeModuleKey)
            throws DocumentException {
        final Map<String, String> params = new HashMap<String, String>();
        params.put("cache", "false");
        ResourceDescriptor resourceDescriptor = TestUtils.createResourceDescriptor("no-cache.js", params);

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey)),
                TestUtils.createWebResourceModuleDescriptor(completeModuleKey, testPlugin,
                        Collections.singletonList(resourceDescriptor)));

        return "src=\"" + (baseUrlExpected ? BASEURL : "") + BatchPluginResource.URL_PREFIX +
                "/" + completeModuleKey + "/" + completeModuleKey + ".js?cache=false";
    }

    // testGetStaticResourcePrefix

    public void testGetStaticResourcePrefix() {
        final String expectedPrefix = setupGetStaticResourcePrefix(false);
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix());
    }

    public void testGetStaticResourcePrefixWithAbsoluteUrlMode() {
        testGetStaticResourcePrefix(UrlMode.ABSOLUTE, true);
    }

    public void testGetStaticResourcePrefixWithRelativeUrlMode() {
        testGetStaticResourcePrefix(UrlMode.RELATIVE, false);
    }

    public void testGetStaticResourcePrefixWithAutoUrlMode() {
        testGetStaticResourcePrefix(UrlMode.AUTO, false);
    }

    private void testGetStaticResourcePrefix(UrlMode urlMode, boolean baseUrlExpected) {
        final String expectedPrefix = setupGetStaticResourcePrefix(baseUrlExpected);
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix(urlMode));
    }

    private String setupGetStaticResourcePrefix(boolean baseUrlExpected) {
        return (baseUrlExpected ? BASEURL : "") +
                "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" +
                SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX;
    }

    // testGetStaticResourcePrefixWithCounter

    public void testGetStaticResourcePrefixWithCounter() {
        final String resourceCounter = "456";
        final String expectedPrefix = setupGetStaticResourcePrefixWithCounter(false, resourceCounter);
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix(resourceCounter));
    }

    public void testGetStaticResourcePrefixWithCounterAndAbsoluteUrlMode() {
        testGetStaticResourcePrefixWithCounter(UrlMode.ABSOLUTE, true);
    }

    public void testGetStaticResourcePrefixWithCounterAndRelativeUrlMode() {
        testGetStaticResourcePrefixWithCounter(UrlMode.RELATIVE, false);
    }

    public void testGetStaticResourcePrefixWithCounterAndAutoUrlMode() {
        testGetStaticResourcePrefixWithCounter(UrlMode.AUTO, false);
    }

    private void testGetStaticResourcePrefixWithCounter(UrlMode urlMode, boolean baseUrlExpected) {
        final String resourceCounter = "456";
        final String expectedPrefix = setupGetStaticResourcePrefixWithCounter(baseUrlExpected, resourceCounter);
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix(resourceCounter, urlMode));
    }

    private String setupGetStaticResourcePrefixWithCounter(boolean baseUrlExpected, String resourceCounter) {
        return (baseUrlExpected ? BASEURL : "") +
                "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" +
                SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + resourceCounter +
                "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX;
    }

    // testGetStaticPluginResourcePrefix

    public void testGetStaticPluginResourcePrefix() {
        final String moduleKey = "confluence.extra.animal:animal";
        final String resourceName = "foo.js";

        final String expectedPrefix = setupGetStaticPluginResourcePrefix(false, moduleKey, resourceName);

        assertEquals(expectedPrefix, webResourceManager.getStaticPluginResource(moduleKey, resourceName));
    }

    public void testGetStaticPluginResourcePrefixWithAbsoluteUrlMode() {
        testGetStaticPluginResourcePrefix(UrlMode.ABSOLUTE, true);
    }

    public void testGetStaticPluginResourcePrefixWithRelativeUrlMode() {
        testGetStaticPluginResourcePrefix(UrlMode.RELATIVE, false);
    }

    public void testGetStaticPluginResourcePrefixWithAutoUrlMode() {
        testGetStaticPluginResourcePrefix(UrlMode.AUTO, false);
    }

    private void testGetStaticPluginResourcePrefix(UrlMode urlMode, boolean baseUrlExpected) {
        final String moduleKey = "confluence.extra.animal:animal";
        final String resourceName = "foo.js";

        final String expectedPrefix = setupGetStaticPluginResourcePrefix(baseUrlExpected, moduleKey, resourceName);

        assertEquals(expectedPrefix, webResourceManager.getStaticPluginResource(moduleKey, resourceName, urlMode));
    }

    private String setupGetStaticPluginResourcePrefix(boolean baseUrlExpected, String moduleKey, String resourceName) {
        final Plugin animalPlugin = new StaticPlugin();
        animalPlugin.setKey("confluence.extra.animal");
        animalPlugin.setPluginsVersion(Integer.parseInt(ANIMAL_PLUGIN_VERSION));

        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(moduleKey)),
                TestUtils.createWebResourceModuleDescriptor(moduleKey, animalPlugin));

        return (baseUrlExpected ? BASEURL : "") +
                "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER +
                "/" + SYSTEM_COUNTER + "/" + ANIMAL_PLUGIN_VERSION + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX +
                "/" + AbstractFileServerServlet.SERVLET_PATH + "/" + AbstractFileServerServlet.RESOURCE_URL_PREFIX +
                "/" + moduleKey + "/" + resourceName;
    }

    public void testGetRequiredResourcesWithFilter() throws Exception {
        final String moduleKey = "cool-resources";
        final String completeModuleKey = "test.maera:" + moduleKey;

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("cool.css", "cool.js", "more-cool.css");

        setupRequestCache();

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey)),
                TestUtils.createWebResourceModuleDescriptor(completeModuleKey, testPlugin, resourceDescriptors1));

        // test includeResources(writer, type) method
        webResourceManager.requireResource(completeModuleKey);

        String staticBase = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER
                + "/" + SYSTEM_COUNTER + "/" + testPlugin.getPluginInformation().getVersion() + "/"
                + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX + BatchPluginResource.URL_PREFIX;

        String cssRef = "href=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".css";
        String jsRef = "src=\"" + staticBase + "/" + completeModuleKey + "/" + completeModuleKey + ".js";

        // CSS
        String requiredResourceResult = webResourceManager.getRequiredResources(UrlMode.ABSOLUTE, new CssWebResource());
        assertTrue(requiredResourceResult.contains(cssRef));
        assertFalse(requiredResourceResult.contains(jsRef));

        // JS
        requiredResourceResult = webResourceManager.getRequiredResources(UrlMode.ABSOLUTE, new JavascriptWebResource());
        assertFalse(requiredResourceResult.contains(cssRef));
        assertTrue(requiredResourceResult.contains(jsRef));

        // BOTH
        requiredResourceResult = webResourceManager.getRequiredResources(UrlMode.ABSOLUTE);
        assertTrue(requiredResourceResult.contains(cssRef));
        assertTrue(requiredResourceResult.contains(jsRef));
    }

    public void testGetRequiredResourcesWithCustomFilters() throws Exception {
        WebResourceFilter atlassianFilter = new WebResourceFilter() {
            public boolean matches(String resourceName) {
                return resourceName.contains("maera");
            }
        };
        WebResourceFilter bogusFilter = new WebResourceFilter() {
            public boolean matches(String resourceName) {
                return true;
            }
        };

        final String moduleKey = "cool-resources";
        final String completeModuleKey = "test.maera:" + moduleKey;

        final List<ResourceDescriptor> resources = TestUtils.createResourceDescriptors(
                "foo.css", "foo-bar.js",
                "atlassian.css", "atlassian-plugins.js");

        setupRequestCache();
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey)),
                TestUtils.createWebResourceModuleDescriptor(completeModuleKey, testPlugin, resources));

        // easier to test which resources were included by the filter with batching turned off
        System.setProperty(PluginResourceLocatorImpl.PLUGIN_WEBRESOURCE_BATCHING_OFF, "true");
        try {
            webResourceManager.requireResource(completeModuleKey);
            String atlassianResources = webResourceManager.getRequiredResources(UrlMode.RELATIVE, atlassianFilter);
            assertEquals(-1, atlassianResources.indexOf("foo"));
            assertTrue(atlassianResources.contains("atlassian.css"));
            assertTrue(atlassianResources.contains("atlassian-plugins.js"));

            String allResources = webResourceManager.getRequiredResources(UrlMode.RELATIVE, bogusFilter);
            for (ResourceDescriptor resource : resources) {
                assertTrue(allResources.contains(resource.getName()));
            }
        }
        finally {
            System.setProperty(PluginResourceLocatorImpl.PLUGIN_WEBRESOURCE_BATCHING_OFF, "false");
        }
    }

    public void testGetRequiredResourcesOrdersByType() throws Exception {
        final String moduleKey1 = "cool-resources";
        final String moduleKey2 = "hot-resources";
        final String completeModuleKey1 = "test.maera:" + moduleKey1;
        final String completeModuleKey2 = "test.maera:" + moduleKey2;

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("cool.js", "cool.css", "more-cool.css");
        final List<ResourceDescriptor> resourceDescriptors2 = TestUtils.createResourceDescriptors("hot.js", "hot.css", "more-hot.css");

        final Plugin plugin = TestUtils.createTestPlugin();

        setupRequestCache();

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey1)),
                TestUtils.createWebResourceModuleDescriptor(completeModuleKey1, plugin, resourceDescriptors1));
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey2)),
                TestUtils.createWebResourceModuleDescriptor(completeModuleKey2, plugin, resourceDescriptors2));

        // test includeResources(writer, type) method
        webResourceManager.requireResource(completeModuleKey1);
        webResourceManager.requireResource(completeModuleKey2);

        String staticBase = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER
                + "/" + SYSTEM_COUNTER + "/" + plugin.getPluginInformation().getVersion() + "/"
                + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX + BatchPluginResource.URL_PREFIX;

        String cssRef1 = "href=\"" + staticBase + "/" + completeModuleKey1 + "/" + completeModuleKey1 + ".css";
        String cssRef2 = "href=\"" + staticBase + "/" + completeModuleKey2 + "/" + completeModuleKey2 + ".css";
        String jsRef1 = "src=\"" + staticBase + "/" + completeModuleKey1 + "/" + completeModuleKey1 + ".js";
        String jsRef2 = "src=\"" + staticBase + "/" + completeModuleKey2 + "/" + completeModuleKey2 + ".js";

        String requiredResourceResult = webResourceManager.getRequiredResources(UrlMode.ABSOLUTE);

        assertTrue(requiredResourceResult.contains(cssRef1));
        assertTrue(requiredResourceResult.contains(cssRef2));
        assertTrue(requiredResourceResult.contains(jsRef1));
        assertTrue(requiredResourceResult.contains(jsRef2));

        int cssRef1Index = requiredResourceResult.indexOf(cssRef1);
        int cssRef2Index = requiredResourceResult.indexOf(cssRef2);
        int jsRef1Index = requiredResourceResult.indexOf(jsRef1);
        int jsRef2Index = requiredResourceResult.indexOf(jsRef2);

        assertTrue(cssRef1Index < jsRef1Index);
        assertTrue(cssRef2Index < jsRef2Index);
        assertTrue(cssRef2Index < jsRef1Index);
    }

    public void testRequireResourceInSuperbatch() {
        resourceBatchingConfiguration.enabled = true;
        Map requestCache = setupRequestCache();
        mockOutSuperbatchPluginAccesses();

        webResourceManager.requireResource("test.maera:superbatch");

        Collection resources = (Collection) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(0, resources.size());
    }

    public void testRequireResourceWithDependencyInSuperbatch() throws DocumentException {
        resourceBatchingConfiguration.enabled = true;
        mockOutSuperbatchPluginAccesses();

        Map requestCache = setupRequestCache();

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq("test.maera:included-resource")),
                TestUtils.createWebResourceModuleDescriptor("test.maera:included-resource", testPlugin, Collections.<ResourceDescriptor>emptyList(), Collections.singletonList("test.maera:superbatch")));

        webResourceManager.requireResource("test.maera:included-resource");

        Collection resources = (Collection) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(1, resources.size());
        assertEquals("test.maera:included-resource", resources.iterator().next());
    }

    public void testSuperBatchResolution() throws DocumentException {
        TestUtils.setupSuperbatchTestContent(resourceBatchingConfiguration, mockPluginAccessor, testPlugin);
        mockOutSuperbatchPluginAccesses();

        List<PluginResource> cssResources = webResourceManager.getSuperBatchResources(CssWebResource.FORMATTER);
        assertEquals(2, cssResources.size());

        SuperBatchPluginResource superBatch1 = (SuperBatchPluginResource) cssResources.get(0);
        assertEquals("batch.css", superBatch1.getResourceName());
        assertTrue(superBatch1.getParams().isEmpty());

        SuperBatchPluginResource superBatch2 = (SuperBatchPluginResource) cssResources.get(1);
        assertEquals("batch.css", superBatch2.getResourceName());
        assertEquals("true", superBatch2.getParams().get("ieonly"));

        List<PluginResource> jsResources = webResourceManager.getSuperBatchResources(JavascriptWebResource.FORMATTER);
        assertEquals(1, jsResources.size());
        assertEquals("batch.js", jsResources.get(0).getResourceName());
        assertEquals(0, jsResources.get(0).getParams().size());
    }

    private void mockOutSuperbatchPluginAccesses() {
        mockOutPluginModule("test.maera:superbatch");
        mockOutPluginModule("test.maera:superbatch2");
        mockPluginAccessor.matchAndReturn("getPluginModule", "test.maera:missing-plugin", null);
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", "test.maera:missing-plugin", null);
    }

    private void mockOutPluginModule(String moduleKey) {
        Plugin p = TestUtils.createTestPlugin();
        WebResourceModuleDescriptor module = TestUtils.createWebResourceModuleDescriptor(moduleKey, p);
        mockPluginAccessor.matchAndReturn("getPluginModule", moduleKey, module);
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", moduleKey, module);
    }
}
