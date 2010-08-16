package org.maera.plugin.osgi.hostcomponents.impl;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.junit.Test;
import org.maera.plugin.hostcontainer.HostContainer;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.osgi.framework.BundleContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import static org.junit.Assert.*;

public class DefaultComponentRegistrarTest {

    @Test
    public void testRegister() {
        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        Class[] ifs = new Class[]{Serializable.class};
        registrar.register(ifs).forInstance("Foo").withName("foo").withProperty("jim", "bar");
        HostComponentRegistration reg = registrar.getRegistry().get(0);

        assertNotNull(reg);
        assertEquals("Foo", reg.getInstance());
        assertEquals(Serializable.class.getName(), reg.getMainInterfaces()[0]);
        assertEquals("foo", reg.getProperties().get(DefaultPropertyBuilder.BEAN_NAME));
        assertEquals("bar", reg.getProperties().get("jim"));
    }

    @Test
    public void testRegisterMultiple() {
        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        Class[] ifs = new Class[]{Serializable.class};
        registrar.register(ifs).forInstance("Foo").withName("foo").withProperty("jim", "bar");
        registrar.register(ifs).forInstance("Foo").withName("foo").withProperty("sarah", "bar");
        assertEquals(2, registrar.getRegistry().size());
    }

    @Test
    public void testRegisterOnlyInterfaces() {
        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        Class[] ifs = new Class[]{Object.class};
        try {
            registrar.register(ifs).forInstance("Foo").withName("foo").withProperty("jim", "bar");
            fail("Should have failed");
        }
        catch (IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testWriteRegistry() {
        Class[] ifs = new Class[]{Serializable.class};
        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        registrar.register(ifs).forInstance("Foo").withName("foo");

        Mock mockBundleContext = new Mock(BundleContext.class);
        registerInMock(mockBundleContext, ifs, "Foo", "foo");

        registrar.writeRegistry((BundleContext) mockBundleContext.proxy());

        mockBundleContext.verify();
    }

    @Test
    public void testWriteRegistryGenBeanName() {
        Class[] ifs = new Class[]{Serializable.class};
        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        registrar.register(ifs).forInstance("Foo");

        Mock mockBundleContext = new Mock(BundleContext.class);
        registerInMock(mockBundleContext, ifs, "Foo", "hostComponent-" + Arrays.asList(registrar.getRegistry().get(0).getMainInterfaces()).hashCode());

        registrar.writeRegistry((BundleContext) mockBundleContext.proxy());

        mockBundleContext.verify();
    }

    @Test
    public void testWriteRegistryNoInterface() {
        Class[] ifs = new Class[]{};
        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        registrar.register(ifs).forInstance("Foo").withName("foo");

        Mock mockBundleContext = new Mock(BundleContext.class);
        registerInMock(mockBundleContext, ifs, "Foo", "foo");

        registrar.writeRegistry((BundleContext) mockBundleContext.proxy());

        mockBundleContext.verify();
    }

    @Test
    public void testWriteRegistryRemovesHostContainer() {
        Class[] ifs = new Class[]{HostContainer.class};
        DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        registrar.register(ifs).forInstance("Foo").withName("foo");

        Mock mockBundleContext = new Mock(BundleContext.class);

        registrar.writeRegistry((BundleContext) mockBundleContext.proxy());

        mockBundleContext.verify();
        assertEquals(0, registrar.getRegistry().size());
    }

    private void registerInMock(Mock mockBundleContext, Class[] ifs, Object instance, String name) {
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(DefaultPropertyBuilder.BEAN_NAME, name);
        properties.put(DefaultComponentRegistrar.HOST_COMPONENT_FLAG, "true");

        mockBundleContext.expect("registerService", C.args(isEqInterfaceList(ifs),
                C.eq(instance),
                C.eq(properties)));
    }

    static Constraint isEqInterfaceList(Class[] array) {
        return new ArrayConstraint(array);
    }

    static class ArrayConstraint implements Constraint {

        private Class[] expected;

        public ArrayConstraint(Class[] expected) {
            this.expected = expected;
        }

        public boolean eval(Object o) {
            if (o != null) {
                String[] totest = (String[]) o;
                if (expected.length == totest.length) {
                    boolean fail = false;
                    for (int x = 0; x < expected.length; x++) {
                        if (!expected[x].getName().equals(totest[x])) {
                            fail = true;
                            break;
                        }
                    }
                    if (!fail)
                        return true;
                }

            }
            return false;
        }
    }
}
