package org.maera.plugin.spring.plugins;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Handler for the "plugin" namespace
 */
public class PluginNamespaceHandler extends NamespaceHandlerSupport {
    /**
     * Registers the following features:
     * <ul>
     * <li>The "available" attribute for beans</li>
     * <li>The "interface" attribute for beans to declare which interfaces they should be registered against</li>
     * </ul>
     */
    public void init() {
        super.registerBeanDefinitionDecorator("interface", new PluginInterfaceBeanDefinitionDecorator());
        super.registerBeanDefinitionDecoratorForAttribute("available", new PluginAvailableBeanDefinitionDecorator());
        super.registerBeanDefinitionDecoratorForAttribute("contextClassLoader", new PluginContextClassLoaderStrategyBeanDefinitionDecorator());

        // Deprecated
        super.registerBeanDefinitionDecoratorForAttribute("ccls", new PluginContextClassLoaderStrategyBeanDefinitionDecorator());
    }
}