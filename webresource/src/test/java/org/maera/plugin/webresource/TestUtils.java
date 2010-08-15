package org.maera.plugin.webresource;

import com.mockobjects.dynamic.Mock;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginInformation;
import org.maera.plugin.elements.ResourceDescriptor;
import org.maera.plugin.elements.ResourceLocation;

import java.util.*;

public class TestUtils {
    static WebResourceModuleDescriptor createWebResourceModuleDescriptor(final String completeKey, final Plugin p) {
        return createWebResourceModuleDescriptor(completeKey, p, Collections.<ResourceDescriptor>emptyList(), Collections.<String>emptyList());
    }

    static WebResourceModuleDescriptor createWebResourceModuleDescriptor(final String completeKey,
                                                                         final Plugin p, final List<ResourceDescriptor> resourceDescriptors) {
        return createWebResourceModuleDescriptor(completeKey, p, resourceDescriptors, Collections.<String>emptyList());
    }

    static WebResourceModuleDescriptor createWebResourceModuleDescriptor(final String completeKey,
                                                                         final Plugin p, final List<ResourceDescriptor> resourceDescriptors, final List<String> dependencies) {
        return createWebResourceModuleDescriptor(completeKey, p, resourceDescriptors, dependencies, Collections.<String>emptySet());
    }

    static WebResourceModuleDescriptor createWebResourceModuleDescriptor(final String completeKey,
                                                                         final Plugin p, final List<ResourceDescriptor> resourceDescriptors, final List<String> dependencies, final Set<String> contexts) {
        return new WebResourceModuleDescriptor() {
            @Override
            public String getCompleteKey() {
                return completeKey;
            }

            @Override
            public List<ResourceDescriptor> getResourceDescriptors() {
                return resourceDescriptors;
            }

            @Override
            public List<ResourceDescriptor> getResourceDescriptors(String type) {
                return resourceDescriptors;
            }

            @Override
            public String getPluginKey() {
                return p.getKey();
            }

            @Override
            public Plugin getPlugin() {
                return p;
            }

            @Override
            public List<String> getDependencies() {
                return dependencies;
            }

            @Override
            public Set<String> getContexts() {
                return contexts;
            }

            @Override
            public ResourceLocation getResourceLocation(String type, String name) {
                if ("download".equals(type)) {
                    return new ResourceLocation("", name, type, "", "", Collections.<String, String>emptyMap());
                }
                return super.getResourceLocation(type, name);
            }
        };
    }

    static List<ResourceDescriptor> createResourceDescriptors(String... resourceNames) throws DocumentException {
        List<ResourceDescriptor> resourceDescriptors = new ArrayList<ResourceDescriptor>();
        for (String resourceName : resourceNames) {
            resourceDescriptors.add(createResourceDescriptor(resourceName));
        }
        return resourceDescriptors;
    }

    static ResourceDescriptor createResourceDescriptor(String resourceName) throws DocumentException {
        return createResourceDescriptor(resourceName, new TreeMap<String, String>());
    }

    static ResourceDescriptor createResourceDescriptor(String resourceName, Map<String, String> parameters) throws DocumentException {
        String xml = "<resource type=\"download\" name=\"" + resourceName + "\" location=\"/includes/css/" + resourceName + "\">\n" +
                "<param name=\"source\" value=\"webContextStatic\"/>\n";

        if (resourceName.indexOf("ie") != -1)
            parameters.put("ieonly", "true");

        for (String key : parameters.keySet()) {
            xml += "<param name=\"" + escapeXMLCharacters(key) + "\" value=\"" + escapeXMLCharacters(parameters.get(key)) + "\"/>\n";
        }

        xml += "</resource>";
        return new ResourceDescriptor(DocumentHelper.parseText(xml).getRootElement());
    }

    static void setupSuperbatchTestContent(TestResourceBatchingConfiguration resourceBatchingConfiguration, Mock mockPluginAccessor, Plugin testPlugin)
            throws DocumentException {
        resourceBatchingConfiguration.enabled = true;

        ResourceDescriptor masterCssResource = TestUtils.createResourceDescriptor("master.css");
        ResourceDescriptor ieOnlyasterCssResource = TestUtils.createResourceDescriptor("master.css", Collections.singletonMap("ieonly", "true"));
        ResourceDescriptor irrelevantParameterCssResource = TestUtils.createResourceDescriptor("two.css", Collections.singletonMap("fish", "true"));
        ResourceDescriptor masterJavascriptResource = TestUtils.createResourceDescriptor("master.js");

        ResourceDescriptor pluginCssResource = TestUtils.createResourceDescriptor("plugin.css");
        ResourceDescriptor pluginJsResource = TestUtils.createResourceDescriptor("plugin.js");

        WebResourceModuleDescriptor master = TestUtils.createWebResourceModuleDescriptor("test.atlassian:superbatch", testPlugin, Arrays.asList(masterCssResource, ieOnlyasterCssResource, masterJavascriptResource, irrelevantParameterCssResource));
        WebResourceModuleDescriptor plugin = TestUtils.createWebResourceModuleDescriptor("test.atlassian:superbatch2", testPlugin, Arrays.asList(pluginCssResource, pluginJsResource));

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", "test.atlassian:superbatch", master);
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", "test.atlassian:superbatch2", plugin);
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", "test.atlassian:missing-plugin", null);
    }

    private static String escapeXMLCharacters(String input) {
        return input.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt");
    }

    static Plugin createTestPlugin() {
        return createTestPlugin("test.atlassian", "1");
    }

    static Plugin createTestPlugin(String pluginKey, String version) {
        final Mock mockPlugin = new Mock(Plugin.class);
        PluginInformation pluginInfo = new PluginInformation();
        pluginInfo.setVersion(version);
        mockPlugin.matchAndReturn("getPluginInformation", pluginInfo);
        mockPlugin.matchAndReturn("getKey", pluginKey);

        return (Plugin) mockPlugin.proxy();
    }
}
