package org.maera.plugin.servlet.descriptors;

import org.dom4j.Element;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.util.validation.ValidationPattern;

import static org.maera.plugin.util.validation.ValidationPattern.test;

/**
 * Allows plugin developers to specify init parameters they would like added to the plugin local {@link javax.servlet.ServletContext}.
 *
 * @since 2.1.0
 */
public class ServletContextParamModuleDescriptor extends AbstractModuleDescriptor<Void> {
    private String paramName;
    private String paramValue;

    public ServletContextParamModuleDescriptor() {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException {
        super.init(plugin, element);

        paramName = element.elementTextTrim("param-name");
        paramValue = element.elementTextTrim("param-value");
    }

    @Override
    protected void provideValidationRules(ValidationPattern pattern) {
        super.provideValidationRules(pattern);
        pattern.
                rule(
                        test("param-name").withError("Parameter name is required"),
                        test("param-value").withError("Parameter value is required"));
    }

    public String getParamName() {
        return paramName;
    }

    public String getParamValue() {
        return paramValue;
    }

    @Override
    public Void getModule() {
        return null;
    }
}
