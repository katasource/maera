package org.maera.plugin.web.descriptors;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.StateAware;
import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.elements.ResourceDescriptor;
import org.maera.plugin.hostcontainer.HostContainer;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.util.validation.ValidationPattern;
import org.maera.plugin.web.Condition;
import org.maera.plugin.web.ContextProvider;
import org.maera.plugin.web.WebInterfaceManager;
import org.maera.plugin.web.model.EmbeddedTemplateWebPanel;
import org.maera.plugin.web.model.ResourceTemplateWebPanel;
import org.maera.plugin.web.model.WebPanel;

import java.util.Iterator;

import static org.maera.plugin.util.validation.ValidationPattern.test;

/**
 * <p>
 * The web panel module declares a single web panel in maera-plugin.xml. Its
 * XML element contains a location string that should match existing locations
 * in the host application where web panels can be embedded.
 * </p>
 * <p>
 * A web panel also contains a single resource child element that contains the
 * contents of the web panel. This can be plain HTML, or a (velocity) template
 * to provide dynamic content.
 * </p>
 * <p>
 * A resource element's <code>type</code> attribute identifies the format of the
 * panel's content (currently "static" and "velocity" are supported) which
 * allows the plugin framework to use the appropriate
 * {@link org.maera.plugin.web.renderer.WebPanelRenderer}.
 * </p>
 * <p>
 * A web panel's resource element can either contain its contents embedded in
 * the resource element itself, as part of the <code>maera-plugin.xml</code>
 * file, or it can link to a file on the classpath when the
 * <code>location</code> attribute is used.
 * </p>
 * <b>Examples</b>
 * <p>
 * A web panel that contains static, embedded HTML:
 * <p/>
 * <pre>
 *     &lt;web-panel key="myPanel" location="general">
 *         &lt;resource name="view" type="static">&lt;![CDATA[&lt;b>Hello World!&lt;/b>]]>&lt;/resource>
 *     &lt;/web-panel>
 * </pre>
 * <p/>
 * </p>
 * <p>
 * A web panel that contains an embedded velocity template:
 * <p/>
 * <pre>
 *     &lt;web-panel key="myPanel" location="general">
 *         &lt;resource name="view" type="velocity">&lt;![CDATA[#set($name = 'foo')My name is $name]]>&lt;/resource>
 *     &lt;/web-panel>
 * </pre>
 * <p/>
 * </p>
 * <p>
 * A web panel that contains uses a velocity template that is on the classpath
 * (part of the plugin's jar file):
 * <p/>
 * <pre>
 *     &lt;web-panel key="myPanel" location="general">
 *         &lt;resource name="view" type="velocity" location="templates/pie.vm"/>
 *     &lt;/web-panel>
 * </pre>
 * <p/>
 * </p>
 * <p>
 * Finally it is also possible to provide your own custom class that is
 * responsible for producing the panel's HTML, by using the descriptor's
 * <code>class</code> attribute:
 * <p/>
 * <pre>
 *     &lt;web-panel key="myPanel" location="general" class="com.example.FooWebPanel"/>
 * </pre>
 * <p/>
 * Note that <code>FooWebPanel</code> must implement
 * {@link org.maera.plugin.web.model.WebPanel}.
 * </p>
 *
 * @since 2.5.0
 */
public final class DefaultWebPanelModuleDescriptor extends AbstractModuleDescriptor<WebPanel> implements WeightedDescriptor, StateAware, ConditionalDescriptor {
    /**
     * Host applications should use this string when registering the web panel
     * module descriptor.
     */
    public static final String XML_ELEMENT_NAME = "web-panel";

    private final WebInterfaceManager webInterfaceManager;
    private final HostContainer hostContainer;

    /**
     * These suppliers are used to delay instantiation because the required
     * spring beans are not available for injection during the init() phase.
     */
    private Supplier<WebPanel> webPanelFactory;
    private Supplier<Condition> conditionFactory;
    private Supplier<ContextProvider> contextProviderFactory;

    private int weight;
    private String location;

    public DefaultWebPanelModuleDescriptor(final HostContainer hostContainer, final ModuleFactory moduleClassFactory, final WebInterfaceManager webInterfaceManager) {
        super(moduleClassFactory);
        this.hostContainer = hostContainer;
        this.webInterfaceManager = webInterfaceManager;
    }

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException {
        super.init(plugin, element);

        weight = WeightElementParser.getWeight(element);
        location = element.attributeValue("location");
        conditionFactory = new Supplier<Condition>() {
            public Condition get() {
                return new ConditionElementParser(webInterfaceManager.getWebFragmentHelper()).makeConditions(plugin, element, ConditionElementParser.CompositeType.AND);
            }
        };
        contextProviderFactory = new Supplier<ContextProvider>() {
            private ContextProvider contextProvider;

            public ContextProvider get() {
                if (contextProvider == null) {
                    contextProvider = new ContextProviderElementParser(webInterfaceManager.getWebFragmentHelper()).makeContextProvider(plugin, element);
                }
                return contextProvider;
            }
        };

        if (moduleClassName == null) {
            final ResourceDescriptor resource = getRequiredViewResource();
            final String filename = resource.getLocation();
            if (StringUtils.isEmpty(filename)) {
                final String body = Preconditions.checkNotNull(resource.getContent());
                webPanelFactory = new Supplier<WebPanel>() {
                    public WebPanel get() {
                        final EmbeddedTemplateWebPanel panel = hostContainer.create(EmbeddedTemplateWebPanel.class);
                        panel.setTemplateBody(body);
                        panel.setResourceType(getRequiredResourceType(resource));
                        panel.setPlugin(plugin);
                        return panel;
                    }
                };
            } else {
                webPanelFactory = new Supplier<WebPanel>() {
                    public WebPanel get() {
                        final ResourceTemplateWebPanel panel = hostContainer.create(ResourceTemplateWebPanel.class);
                        panel.setResourceFilename(filename);
                        panel.setResourceType(getRequiredResourceType(resource));
                        panel.setPlugin(plugin);
                        return panel;
                    }
                };
            }
        } else {
            final String moduleClassNameCopy = moduleClassName;
            webPanelFactory = new Supplier<WebPanel>() {
                public WebPanel get() {
                    return moduleFactory.createModule(moduleClassNameCopy, DefaultWebPanelModuleDescriptor.this);
                }
            };
        }
    }

    @Override
    protected void provideValidationRules(final ValidationPattern pattern) {
        super.provideValidationRules(pattern);
        pattern.rule(test("@location").withError("The Web Panel location attribute is required."));
    }

    public String getLocation() {
        return location;
    }

    public int getWeight() {
        return weight;
    }

    public Condition getCondition() {
        return conditionFactory.get();
    }

    public ContextProvider getContextProvider() {
        return contextProviderFactory.get();
    }

    @Override
    public WebPanel getModule() {
        return webPanelFactory.get();
    }

    private String getRequiredResourceType(final ResourceDescriptor resource) {
        final String type = resource.getType();
        if (StringUtils.isEmpty(type)) {
            throw new PluginParseException("Resource element is lacking a type attribute.");
        } else {
            return type;
        }
    }

    /**
     * @return the (first) resource with attribute <code>name="view"</code>
     * @throws PluginParseException when no resources with name "view" were
     *                              found.
     */
    private ResourceDescriptor getRequiredViewResource() throws PluginParseException {
        final Iterable<ResourceDescriptor> resources = Iterables.filter(getResourceDescriptors(), new Predicate<ResourceDescriptor>() {
            public boolean apply(final ResourceDescriptor input) {
                return "view".equals(input.getName());
            }
        });
        final Iterator<ResourceDescriptor> iterator = resources.iterator();
        if (!iterator.hasNext()) {
            throw new PluginParseException("Required resource with name 'view' does not exist.");
        } else {
            return iterator.next();
        }
    }

    @Override
    public void enabled() {
        super.enabled();
        webInterfaceManager.refresh();
    }

    @Override
    public void disabled() {
        webInterfaceManager.refresh();
        super.disabled();
    }
}
