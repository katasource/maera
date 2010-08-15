package org.maera.plugin.spring;

import org.maera.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.*;

import static org.maera.plugin.util.Assertions.notNull;

public class PluginBeanDefinitionRegistry {
    public static final String HOST_COMPONENT_PROVIDER = "hostComponentProvider";

    private static final String BEAN_NAMES = "beanNames";
    private static final String BEAN_INTERFACES = "beanInterfaces";
    private static final String BEAN_CONTEXT_CLASS_LOADER_STRATEGIES = "beanContextClassLoaderStrategies";

    private final BeanDefinitionRegistry registry;

    public PluginBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        this.registry = notNull("registry", registry);
    }

    public BeanDefinition getBeanDefinition() {
        if (!registry.containsBeanDefinition(HOST_COMPONENT_PROVIDER)) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(SpringHostComponentProviderFactoryBean.class);
            builder.addPropertyValue(BEAN_NAMES, new ArrayList<String>());
            builder.addPropertyValue(BEAN_INTERFACES, new HashMap<String, List<String>>());
            builder.addPropertyValue(BEAN_CONTEXT_CLASS_LOADER_STRATEGIES, new HashMap<String, ContextClassLoaderStrategy>());
            builder.addPropertyValue("useAnnotation", false); // by default we don't want to scan for annotation

            registry.registerBeanDefinition(HOST_COMPONENT_PROVIDER, builder.getBeanDefinition());
        }

        final BeanDefinition beanDef = registry.getBeanDefinition(HOST_COMPONENT_PROVIDER);
        if (beanDef == null) {
            throw new IllegalStateException("Host component provider not found nor created. This should never happen.");
        }
        return beanDef;
    }

    public void addBeanName(String beanName) {
        getBeanNames().add(beanName);
    }

    public void addBeanInterface(String beanName, String ifce) {
        addBeanInterfaces(beanName, Collections.singleton(ifce));
    }

    public void addBeanInterfaces(String beanName, Collection<String> ifces) {
        final Map<String, List<String>> beanInterfaces = getBeanInterfaces();

        List<String> interfaces = beanInterfaces.get(beanName);
        if (interfaces == null) {
            interfaces = new ArrayList<String>();
            beanInterfaces.put(beanName, interfaces);
        }
        interfaces.addAll(ifces);
    }

    public void addContextClassLoaderStrategy(String beanName, ContextClassLoaderStrategy strategy) {
        getBeanContextClassLoaderStrategies().put(beanName, strategy);
    }

    @SuppressWarnings("unchecked")
    private Map<String, ContextClassLoaderStrategy> getBeanContextClassLoaderStrategies() {
        return (Map<String, ContextClassLoaderStrategy>) getPropertyValue(BEAN_CONTEXT_CLASS_LOADER_STRATEGIES);
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> getBeanInterfaces() {
        return (Map<String, List<String>>) getPropertyValue(BEAN_INTERFACES);
    }

    @SuppressWarnings("unchecked")
    private List<String> getBeanNames() {
        return (List<String>) getPropertyValue(BEAN_NAMES);
    }

    private Object getPropertyValue(String propertyName) {
        return getBeanDefinition().getPropertyValues().getPropertyValue(propertyName).getValue();
    }
}
