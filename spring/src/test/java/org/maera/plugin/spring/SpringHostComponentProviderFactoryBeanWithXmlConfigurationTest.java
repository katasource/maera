package org.maera.plugin.spring;

import org.junit.Test;
import org.maera.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;
import org.maera.plugin.osgi.hostcomponents.HostComponentProvider;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.maera.plugin.osgi.hostcomponents.PropertyBuilder;
import org.maera.plugin.osgi.hostcomponents.impl.DefaultComponentRegistrar;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.maera.plugin.spring.PluginBeanDefinitionRegistry.HOST_COMPONENT_PROVIDER;

public class SpringHostComponentProviderFactoryBeanWithXmlConfigurationTest {

    private static final HashSet<Class> FOOABLE_BEAN_INTERFACES = new HashSet<Class>(Arrays.asList(Serializable.class, Map.class, Cloneable.class, Fooable.class, Barable.class));
    private static final HashSet<Class> FOO_BARABLE_INTERFACES = new HashSet<Class>(Arrays.asList(Fooable.class, Barable.class));

    @Test
    public void testProvide() {
        XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("org/maera/plugin/spring/plugins/plugins-spring-test.xml"));

        HostComponentProvider provider = getHostProvider(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("foo", list.get(0).getProperties().get("bean-name"));
        assertEquals(5, list.get(0).getMainInterfaces().length);
        assertEquals(FOOABLE_BEAN_INTERFACES, new HashSet<Class>(Arrays.asList(list.get(0).getMainInterfaceClasses())));
        assertEquals(ContextClassLoaderStrategy.USE_PLUGIN.name(), list.get(0).getProperties().get(PropertyBuilder.CONTEXT_CLASS_LOADER_STRATEGY));
    }

    @Test
    public void testProvideWithCustomInterface() {
        XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("org/maera/plugin/spring/plugins/plugins-spring-test-interface.xml"));

        HostComponentProvider provider = getHostProvider(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(2, list.size());

        if ("foo".equals(list.get(0).getProperties().get("bean-name"))) {
            assertFoo(list.get(0));
            assertFooMultipleInterfaces(list.get(1));
        } else {
            assertFoo(list.get(1));
            assertFooMultipleInterfaces(list.get(0));
        }
    }

    @Test
    public void testProvideWithDeprecations() {
        XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("org/maera/plugin/spring/plugins/plugins-spring-deprecations.xml"));

        HostComponentProvider provider = getHostProvider(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("foo", list.get(0).getProperties().get("bean-name"));
        assertEquals(5, list.get(0).getMainInterfaces().length);
        assertEquals(FOOABLE_BEAN_INTERFACES, new HashSet<Class>(Arrays.asList(list.get(0).getMainInterfaceClasses())));
        assertEquals(ContextClassLoaderStrategy.USE_PLUGIN.name(), list.get(0).getProperties().get(PropertyBuilder.CONTEXT_CLASS_LOADER_STRATEGY));
    }

    @Test
    public void testProvideWithInterfaceOnSuperClass() {
        XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("org/maera/plugin/spring/plugins/plugins-spring-test-super-interface.xml"));

        HostComponentProvider provider = getHostProvider(factory);

        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("foobarable", list.get(0).getProperties().get("bean-name"));
        assertEquals(2, list.get(0).getMainInterfaces().length);
        assertEquals(FOO_BARABLE_INTERFACES, new HashSet<Class>(Arrays.asList(list.get(0).getMainInterfaceClasses())));
    }

    @Test
    public void testProvideWithNestedContexts() {
        XmlBeanFactory parentFactory = new XmlBeanFactory(new ClassPathResource("org/maera/plugin/spring/plugins/plugins-spring-test.xml"));
        XmlBeanFactory childFactory = new XmlBeanFactory(new ClassPathResource("org/maera/plugin/spring/plugins/plugins-spring-test-child.xml"), parentFactory);

        HostComponentProvider provider = getHostProvider(childFactory);

        assertTrue(parentFactory.containsBeanDefinition(HOST_COMPONENT_PROVIDER));
        assertTrue(childFactory.containsBeanDefinition(HOST_COMPONENT_PROVIDER));


        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(2, list.size());
    }

    @Test
    public void testProvideWithPrototype() {
        XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("org/maera/plugin/spring/plugins/plugins-spring-test-prototype.xml"));

        HostComponentProvider provider = getHostProvider(factory);


        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        provider.provide(registrar);

        List<HostComponentRegistration> list = registrar.getRegistry();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("foo", list.get(0).getProperties().get("bean-name"));
        assertEquals(5, list.get(0).getMainInterfaces().length);
        assertEquals(FOOABLE_BEAN_INTERFACES, new HashSet<Class>(Arrays.asList(list.get(0).getMainInterfaceClasses())));
    }

    private void assertFoo(HostComponentRegistration registration) {
        assertEquals("foo", registration.getProperties().get("bean-name"));
        assertEquals(1, registration.getMainInterfaces().length);
        assertEquals(BeanFactoryAware.class.getName(), registration.getMainInterfaces()[0]);
    }

    private void assertFooMultipleInterfaces(HostComponentRegistration registration) {
        assertEquals("fooMultipleInterface", registration.getProperties().get("bean-name"));
        assertEquals(2, registration.getMainInterfaces().length);
        assertEquals(BeanFactoryAware.class.getName(), registration.getMainInterfaces()[0]);
        assertEquals(Barable.class.getName(), registration.getMainInterfaces()[1]);
    }

    private HostComponentProvider getHostProvider(BeanFactory factory) {
        final HostComponentProvider provider = (HostComponentProvider) factory.getBean(HOST_COMPONENT_PROVIDER);
        assertNotNull(provider);
        return provider;
    }
}
