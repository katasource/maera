package org.maera.plugin.spring;

import junit.framework.TestCase;
import org.aopalliance.aop.Advice;
import org.maera.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;
import org.maera.plugin.osgi.hostcomponents.HostComponentProvider;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.maera.plugin.osgi.hostcomponents.PropertyBuilder;
import org.maera.plugin.osgi.hostcomponents.impl.DefaultComponentRegistrar;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestSpringHostComponentProviderFactoryBeanWithAnnotations extends TestCase {
    public void testProvide() throws Exception {
        StaticListableBeanFactory factory = new StaticListableBeanFactory() {
            @Override
            public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
                return true;
            }
        };
        factory.addBean("bean", new FooableBean());
        factory.addBean("string", "hello");

        HostComponentProvider provider = getHostComponentProvider(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("bean", list.get(0).getProperties().get("bean-name"));
        List<Class<?>> ifs = Arrays.asList(list.get(0).getMainInterfaceClasses());

        // Test locally declared interface
        assertTrue(ifs.contains(Fooable.class));

        // Test super interface of locally declared interface
        assertTrue(ifs.contains(Barable.class));

        // Test interface of super class
        assertTrue(ifs.contains(Map.class));
    }

    private HostComponentProvider getHostComponentProvider(BeanFactory factory) throws Exception {
        SpringHostComponentProviderFactoryBean providerFactoryBean = new SpringHostComponentProviderFactoryBean();
        providerFactoryBean.setBeanFactory(factory);
        providerFactoryBean.afterPropertiesSet();
        return (HostComponentProvider) providerFactoryBean.getObject();
    }

    public void testProvideWithCCLStrategy() throws Exception {
        StaticListableBeanFactory factory = new StaticListableBeanFactory() {
            @Override
            public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
                return true;
            }
        };
        factory.addBean("bean", new FooablePluginService());
        factory.addBean("string", "hello");

        HostComponentProvider provider = getHostComponentProvider(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("bean", list.get(0).getProperties().get(PropertyBuilder.BEAN_NAME));
        assertEquals(ContextClassLoaderStrategy.USE_PLUGIN.name(), list.get(0).getProperties().get(PropertyBuilder.CONTEXT_CLASS_LOADER_STRATEGY));
    }

    public void testProvideWithProxy() throws Exception {
        StaticListableBeanFactory factory = new StaticListableBeanFactory() {
            @Override
            public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
                return true;
            }
        };

        ProxyFactory pFactory = new ProxyFactory(new FooableBean());
        pFactory.addInterface(Fooable.class);
        pFactory.addAdvice(new Advice() {
        });

        factory.addBean("bean", pFactory.getProxy());
        factory.addBean("string", "hello");

        HostComponentProvider provider = getHostComponentProvider(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("bean", list.get(0).getProperties().get("bean-name"));
        assertEquals(Fooable.class.getName(), list.get(0).getMainInterfaces()[0]);
    }
}
