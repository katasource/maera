package org.maera.plugin.spring.plugins;

import org.maera.plugin.spring.PluginBeanDefinitionRegistry;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

/**
 * Processes an "available" attribute in the plugin namespace.
 * Also handles registering the {@link org.maera.plugin.osgi.hostcomponents.HostComponentProvider} through
 * the {@link org.maera.plugin.spring.SpringHostComponentProviderFactoryBean}.
 * <p/>
 * In the case of hierarchical contexts we will put the host component provider in the lowest possible context.
 */
public class PluginAvailableBeanDefinitionDecorator implements BeanDefinitionDecorator {
    /**
     * Called when the Spring parser encounters an "available" attribute.
     *
     * @param source The attribute
     * @param holder The containing bean definition
     * @param ctx    The parser context
     * @return The containing bean definition
     */
    public BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder holder, ParserContext ctx) {
        final String isAvailable = ((Attr) source).getValue();
        if (Boolean.parseBoolean(isAvailable)) {
            new PluginBeanDefinitionRegistry(ctx.getRegistry()).addBeanName(holder.getBeanName());
        }
        return holder;
    }
}
