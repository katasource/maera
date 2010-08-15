package org.maera.plugin.spring;

import org.apache.commons.lang.ClassUtils;
import org.maera.plugin.osgi.hostcomponents.ComponentRegistrar;
import org.maera.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;
import org.maera.plugin.osgi.hostcomponents.HostComponentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.maera.plugin.osgi.hostcomponents.ContextClassLoaderStrategy.USE_HOST;
import static org.maera.plugin.util.Assertions.notNull;

@Component(PluginBeanDefinitionRegistry.HOST_COMPONENT_PROVIDER)
public class SpringHostComponentProviderFactoryBean extends AbstractFactoryBean {
    private static final Logger log = LoggerFactory.getLogger(SpringHostComponentProviderFactoryBean.class);

    /**
     * A set of bean names to make available to plugins
     */
    private Set<String> beanNames;

    /**
     * Mapping of beanNames to the interfaces it should be exposed as. Note that if a bean name is present an no interface
     * is defined then all its interfaces should be 'exposed'.
     */
    private Map<String, Class[]> beanInterfaces;

    /**
     * Mapping of beanNames with their {@link org.maera.plugin.osgi.hostcomponents.ContextClassLoaderStrategy}.
     * Default value is {@link org.maera.plugin.osgi.hostcomponents.ContextClassLoaderStrategy#USE_HOST}.
     */
    private Map<String, ContextClassLoaderStrategy> beanContextClassLoaderStrategies;

    /**
     * Whether or not to scan for {@link org.maera.plugin.spring.AvailableToPlugins} annotations on beans defined in the bean
     * factory, defaults to {@code true}
     */
    private boolean useAnnotation = true;

    public Class getObjectType() {
        return HostComponentProvider.class;
    }

    protected Object createInstance() throws Exception {
        return new SpringHostComponentProvider(getBeanFactory(), beanNames, beanInterfaces, beanContextClassLoaderStrategies, useAnnotation);
    }

    public void setBeanNames(Set<String> beanNames) {
        this.beanNames = beanNames;
    }

    public void setBeanInterfaces(Map<String, Class[]> beanInterfaces) {
        this.beanInterfaces = beanInterfaces;
    }

    public void setBeanContextClassLoaderStrategies(Map<String, ContextClassLoaderStrategy> beanContextClassLoaderStrategies) {
        this.beanContextClassLoaderStrategies = beanContextClassLoaderStrategies;
    }

    public void setUseAnnotation(boolean useAnnotation) {
        this.useAnnotation = useAnnotation;
    }

    private static class SpringHostComponentProvider implements HostComponentProvider {
        private final BeanFactory beanFactory;
        private boolean useAnnotation;
        private final Set<String> beanNames;
        private final Map<String, Class[]> beanInterfaces;
        private final Map<String, ContextClassLoaderStrategy> beanContextClassLoaderStrategies;

        public SpringHostComponentProvider(BeanFactory beanFactory, Set<String> beanNames, Map<String, Class[]> beanInterfaces, Map<String, ContextClassLoaderStrategy> beanContextClassLoaderStrategies, boolean useAnnotation) {
            this.beanFactory = notNull("beanFactory", beanFactory);
            this.useAnnotation = useAnnotation;
            this.beanNames = beanNames != null ? beanNames : new HashSet<String>();
            this.beanInterfaces = beanInterfaces != null ? beanInterfaces : new HashMap<String, Class[]>();
            this.beanContextClassLoaderStrategies = beanContextClassLoaderStrategies != null ? beanContextClassLoaderStrategies : new HashMap<String, ContextClassLoaderStrategy>();
        }

        public void provide(ComponentRegistrar registrar) {
            final Set<String> beansToProvide = new HashSet<String>(beanNames);
            final Map<String, Class[]> interfacesToProvide = new HashMap<String, Class[]>(beanInterfaces);
            final Map<String, ContextClassLoaderStrategy> contextClassLoaderStrategiesToProvide = new HashMap<String, ContextClassLoaderStrategy>(beanContextClassLoaderStrategies);

            if (useAnnotation) {
                scanForAnnotatedBeans(beansToProvide, interfacesToProvide, contextClassLoaderStrategiesToProvide);
            }

            provideBeans(registrar, beansToProvide, interfacesToProvide, contextClassLoaderStrategiesToProvide);

            // make sure that host component providers we might have defined in parent bean factories also get a chance to provide their beans.
            if (beanFactory instanceof HierarchicalBeanFactory) {
                final BeanFactory parentBeanFactory = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
                if (parentBeanFactory != null) {
                    try {
                        HostComponentProvider provider = (HostComponentProvider) parentBeanFactory.getBean(PluginBeanDefinitionRegistry.HOST_COMPONENT_PROVIDER);
                        if (provider != null) {
                            provider.provide(registrar);
                        }
                    }
                    catch (NoSuchBeanDefinitionException e) {
                        log.debug("Unable to find '" + PluginBeanDefinitionRegistry.HOST_COMPONENT_PROVIDER + "' in the parent bean factory " + parentBeanFactory);
                    }
                }
            }
        }

        private void provideBeans(ComponentRegistrar registrar, Set<String> beanNames, Map<String, Class[]> beanInterfaces, Map<String, ContextClassLoaderStrategy> beanContextClassLoaderStrategies) {
            for (String beanName : beanNames) {
                if (beanFactory.isSingleton(beanName)) {
                    final Object bean = beanFactory.getBean(beanName);
                    Class[] interfaces = beanInterfaces.get(beanName);
                    if (interfaces == null) {
                        interfaces = findInterfaces(getBeanClass(bean));
                    }
                    registrar.register(interfaces)
                            .forInstance(bean)
                            .withName(beanName)
                            .withContextClassLoaderStrategy(beanContextClassLoaderStrategies.containsKey(beanName) ? beanContextClassLoaderStrategies.get(beanName) : USE_HOST);
                } else {
                    log.warn("Cannot register bean '{}' as it's scope is not singleton", beanName);
                }
            }
        }

        private void scanForAnnotatedBeans(Set<String> beansToProvide, Map<String, Class[]> interfacesToProvide, Map<String, ContextClassLoaderStrategy> contextClassLoaderStrategiesToProvide) {
            if (beanFactory instanceof ListableBeanFactory) {
                for (String beanName : ((ListableBeanFactory) beanFactory).getBeanDefinitionNames()) {
                    try {
                        final Class beanClass = getBeanClass(beanFactory.getBean(beanName));
                        final AvailableToPlugins annotation = AnnotationUtils.findAnnotation(beanClass, AvailableToPlugins.class);
                        if (annotation != null) {
                            if (beanFactory.isSingleton(beanName)) {

                                beansToProvide.add(beanName);
                                if (annotation.value() != Void.class) // an interface is defined in the annotation
                                {
                                    if (!interfacesToProvide.containsKey(beanName)) {
                                        interfacesToProvide.put(beanName, new Class[]{annotation.value()});
                                    } else {
                                        log.debug("Interfaces for bean '{}' have been defined in XML or in a Module definition, ignoring the interface defined in the annotation", beanName);
                                    }
                                }

                                if (!contextClassLoaderStrategiesToProvide.containsKey(beanName)) {
                                    contextClassLoaderStrategiesToProvide.put(beanName, annotation.contextClassLoaderStrategy());
                                } else {
                                    log.debug("Context class loader strategy for bean '{}' has been defined in XML or in a Module definition, ignoring the one defined in the annotation", beanName);
                                }
                            } else {
                                log.warn("Could not make bean '{}' available to plugins as it is not scoped 'singleton'", beanName);
                            }
                        }
                    }
                    catch (BeanIsAbstractException ex) {
                        // skipping abstract beans (is there a better way to check for this?)
                    }
                }
            } else {
                log.warn("Could not scan bean factory for beans to make available to plugins, bean factory is not 'listable'");
            }
        }

        private Class[] findInterfaces(Class cls) {
            final List<Class> validInterfaces = new ArrayList<Class>();
            for (Class inf : getAllInterfaces(cls)) {
                if (!inf.getName().startsWith("org.springframework")) {
                    validInterfaces.add(inf);
                }
            }
            return validInterfaces.toArray(new Class[validInterfaces.size()]);
        }

        @SuppressWarnings("unchecked")
        private List<Class> getAllInterfaces(Class cls) {
            return (List<Class>) ClassUtils.getAllInterfaces(cls);
        }

        private Class getBeanClass(Object bean) {
            return AopUtils.getTargetClass(bean);
        }
    }
}
