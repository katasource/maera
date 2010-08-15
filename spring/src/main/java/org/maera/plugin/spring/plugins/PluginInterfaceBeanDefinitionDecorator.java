package org.maera.plugin.spring.plugins;

import org.maera.plugin.spring.PluginBeanDefinitionRegistry;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Node;

/**
 * Matches the <plugin:interface> element and registers it against the bean for later processing.
 */
public class PluginInterfaceBeanDefinitionDecorator implements BeanDefinitionDecorator {
    /**
     * Called when the Spring parser encounters an "interface" element.
     *
     * @param source The interface element
     * @param holder The containing bean definition
     * @param ctx    The parser context
     * @return The containing bean definition
     */
    public BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder holder, ParserContext ctx) {
        final String inf = source.getTextContent();
        if (inf != null) {
            new PluginBeanDefinitionRegistry(ctx.getRegistry()).addBeanInterface(holder.getBeanName(), inf.trim());
        }
        return holder;
    }
}
