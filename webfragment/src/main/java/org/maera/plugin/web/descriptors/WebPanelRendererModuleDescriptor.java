package org.maera.plugin.web.descriptors;

import org.dom4j.Element;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.util.validation.ValidationPattern;
import org.maera.plugin.web.renderer.WebPanelRenderer;

import static org.maera.plugin.util.validation.ValidationPattern.test;

/**
 * The web panel renderer module is used to add web panel renderers to the
 * plugin system.
 *
 * @since 2.5.0
 */
public class WebPanelRendererModuleDescriptor extends AbstractModuleDescriptor<WebPanelRenderer> {
    /**
     * Host applications should use this string when registering the
     * {@link WebPanelRendererModuleDescriptor}.
     */
    public static final String XML_ELEMENT_NAME = "web-panel-renderer";
    private WebPanelRenderer rendererModule;

    public WebPanelRendererModuleDescriptor(ModuleFactory moduleClassFactory) {
        super(moduleClassFactory);
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException {
        super.init(plugin, element);
    }

    @Override
    protected void provideValidationRules(ValidationPattern pattern) {
        super.provideValidationRules(pattern);
        pattern.
                rule(
                        test("@class").withError("The class is required"));
    }

    @Override
    public void enabled() {
        super.enabled();
        if (!(WebPanelRenderer.class.isAssignableFrom(getModuleClass()))) {
            throw new PluginParseException(String.format(
                    "Supplied module class (%s) is not a %s", getModuleClass().getName(), WebPanelRenderer.class.getName()));
        }
    }

    @Override
    public WebPanelRenderer getModule() {
        if (rendererModule == null) {
            rendererModule = moduleFactory.createModule(moduleClassName, this);
        }
        return rendererModule;
    }
}
