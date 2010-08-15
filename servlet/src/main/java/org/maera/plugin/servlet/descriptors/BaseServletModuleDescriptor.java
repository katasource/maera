package org.maera.plugin.servlet.descriptors;

import org.dom4j.Element;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.util.validation.ValidationPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.maera.plugin.util.validation.ValidationPattern.test;

/**
 * Acts as a base for other servlet type module descriptors to inherit.  It adds parsing and retrieval of any paths
 * declared in the descriptor with &lt;url-pattern&gt; as well as &lt;init-param&gt;s.
 *
 * @since 2.1.0
 */
public abstract class BaseServletModuleDescriptor<T> extends AbstractModuleDescriptor<T> {
    protected static final Logger log = LoggerFactory.getLogger(BaseServletModuleDescriptor.class);

    private List<String> paths;
    private Map<String, String> initParams;

    /**
     * @deprecated Since 2.5.0, use {@link BaseServletModuleDescriptor( org.maera.plugin.module.ModuleFactory )} instead (as of 2.5.0).
     */
    public BaseServletModuleDescriptor() {
    }

    /**
     * @param moduleCreator
     * @since 2.5.0
     */
    public BaseServletModuleDescriptor(ModuleFactory moduleCreator) {
        super(moduleCreator);
    }

    public void init(Plugin plugin, Element element) throws PluginParseException {
        super.init(plugin, element);

        List<Element> urlPatterns = element.elements("url-pattern");
        paths = new ArrayList<String>(urlPatterns.size());

        for (Element urlPattern : urlPatterns) {
            paths.add(urlPattern.getTextTrim());
        }

        initParams = new HashMap<String, String>();
        List<Element> paramsList = element.elements("init-param");
        for (Element initParamEl : paramsList) {
            Element paramNameEl = initParamEl.element("param-name");
            Element paramValueEl = initParamEl.element("param-value");
            initParams.put(paramNameEl.getTextTrim(), paramValueEl.getTextTrim());
        }
    }

    @Override
    protected void provideValidationRules(ValidationPattern pattern) {
        super.provideValidationRules(pattern);
        pattern.
                rule(
                        test("@class").withError("The class is required"),
                        test("url-pattern").withError("There must be at least one path specified")).
                rule("init-param",
                        test("param-name").withError("Parameter name is required"),
                        test("param-value").withError("Parameter value is required"));
    }

    public List<String> getPaths() {
        return paths;
    }

    public Map<String, String> getInitParams() {
        return initParams;
    }

}
