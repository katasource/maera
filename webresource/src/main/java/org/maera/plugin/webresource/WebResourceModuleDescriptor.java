package org.maera.plugin.webresource;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.descriptors.AbstractModuleDescriptor;

import java.util.*;

/**
 * A way of linking to web 'resources', such as javascript or css.  This allows us to include resources once
 * on any given page, as well as ensuring that plugins can declare resources, even if they are included
 * at the bottom of a page.
 */
public class WebResourceModuleDescriptor extends AbstractModuleDescriptor<Void> {
    private List<String> dependencies = Collections.emptyList();
    private boolean disableMinification;
    private Set<String> contexts = Collections.emptySet();
    private List<WebResourceTransformation> webResourceTransformations = Collections.emptyList();

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException {
        super.init(plugin, element);

        final List<String> deps = new ArrayList<String>();
        for (Element dependency : (List<Element>) element.elements("dependency")) {
            deps.add(dependency.getTextTrim());
        }
        dependencies = Collections.unmodifiableList(deps);

        final Set<String> ctxs = new HashSet<String>();
        for (Element contextElement : (List<Element>) element.elements("context")) {
            ctxs.add(contextElement.getTextTrim());
        }
        contexts = Collections.unmodifiableSet(ctxs);

        final List<WebResourceTransformation> trans = new ArrayList<WebResourceTransformation>();
        for (Element e : (List<Element>) element.elements("transformation")) {
            trans.add(new WebResourceTransformation(e));
        }
        webResourceTransformations = Collections.unmodifiableList(trans);

        final Attribute minifiedAttribute = element.attribute("disable-minification");
        disableMinification = minifiedAttribute == null ? false : Boolean.valueOf(minifiedAttribute.getValue());
    }

    /**
     * As this descriptor just handles resources, you should never call this
     */
    @Override
    public Void getModule() {
        throw new UnsupportedOperationException("There is no module for Web Resources");
    }

    /**
     * Returns the web resource contexts this resource is associated with.
     *
     * @return the web resource contexts this resource is associated with.
     * @since 2.5.0
     */
    public Set<String> getContexts() {
        return contexts;
    }

    /**
     * Returns a list of dependencies on other web resources.
     *
     * @return a list of module complete keys
     */
    public List<String> getDependencies() {
        return dependencies;
    }

    public List<WebResourceTransformation> getTransformations() {
        return webResourceTransformations;
    }

    /**
     * @return <code>true</code> if resource minification should be skipped, <code>false</code> otherwise.
     */
    public boolean isDisableMinification() {
        return disableMinification;
    }
}
