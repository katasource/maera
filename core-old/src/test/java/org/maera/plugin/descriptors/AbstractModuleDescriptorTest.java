/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 29, 2004
 * Time: 4:28:18 PM
 */
package org.maera.plugin.descriptors;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.StateAware;
import org.maera.plugin.elements.ResourceDescriptor;
import org.maera.plugin.impl.StaticPlugin;
import org.maera.plugin.mock.MockAnimal;
import org.maera.plugin.mock.MockAnimalModuleDescriptor;
import org.maera.plugin.mock.MockMineral;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.util.ClassLoaderUtils;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AbstractModuleDescriptorTest {

    @Test
    public void testAssertModuleClassImplements() throws DocumentException, PluginParseException {
        ModuleDescriptor descriptor = new AbstractModuleDescriptor<MockMineral>(ModuleFactory.LEGACY_MODULE_FACTORY) {

            public void init(Plugin plugin, Element element) throws PluginParseException {
                super.init(plugin, element);
                enabled();
                assertModuleClassImplements(MockMineral.class);
            }

            public MockMineral getModule() {
                return null;
            }
        };

        try {
            descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal key=\"key\" name=\"bear\" class=\"org.maera.plugin.mock.MockBear\" />").getRootElement());
            ((StateAware) descriptor).enabled();
            fail("Should have blown up.");
        }
        catch (PluginParseException e) {
            assertEquals("Given module class: org.maera.plugin.mock.MockBear does not implement org.maera.plugin.mock.MockMineral", e.getMessage());
        }

        // now succeed
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal key=\"key\" name=\"bear\" class=\"org.maera.plugin.mock.MockGold\" />").getRootElement());
    }

    @Test
    public void testGetModuleReturnClass() {
        AbstractModuleDescriptor desc = new MockAnimalModuleDescriptor();
        assertEquals(MockAnimal.class, desc.getModuleReturnClass());
    }

    @Test
    public void testGetModuleReturnClassWithExtendsNumber() {
        ModuleFactory moduleFactory = mock(ModuleFactory.class);
        AbstractModuleDescriptor moduleDescriptor = new ExtendsNothingModuleDescriptor(moduleFactory, "foo");
        assertEquals(Object.class, moduleDescriptor.getModuleReturnClass());
    }

    @Test
    public void testGetResourceDescriptor() throws DocumentException, PluginParseException {
        ModuleDescriptor descriptor = makeSingletonDescriptor();
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal key=\"key\" name=\"bear\" class=\"org.maera.plugin.mock.MockBear\">" +
                "<resource type='velocity' name='view' location='foo' />" +
                "</animal>").getRootElement());

        assertNull(descriptor.getResourceLocation("foo", "bar"));
        assertNull(descriptor.getResourceLocation("velocity", "bar"));
        assertNull(descriptor.getResourceLocation("foo", "view"));
        assertEquals(new ResourceDescriptor(DocumentHelper.parseText("<resource type='velocity' name='view' location='foo' />").getRootElement()).getResourceLocationForName("view").getLocation(), descriptor.getResourceLocation("velocity", "view").getLocation());
    }

    @Test
    public void testGetResourceDescriptorByType() throws DocumentException, PluginParseException {
        ModuleDescriptor descriptor = makeSingletonDescriptor();
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal key=\"key\" name=\"bear\" class=\"org.maera.plugin.mock.MockBear\">" +
                "<resource type='velocity' name='view' location='foo' />" +
                "<resource type='velocity' name='input-params' location='bar' />" +
                "</animal>").getRootElement());

        final List resourceDescriptors = descriptor.getResourceDescriptors("velocity");
        assertNotNull(resourceDescriptors);
        assertEquals(2, resourceDescriptors.size());

        ResourceDescriptor resourceDescriptor = (ResourceDescriptor) resourceDescriptors.get(0);
        assertEquals(new ResourceDescriptor(DocumentHelper.parseText("<resource type='velocity' name='view' location='foo' />").getRootElement()), resourceDescriptor);

        resourceDescriptor = (ResourceDescriptor) resourceDescriptors.get(1);
        assertEquals(new ResourceDescriptor(DocumentHelper.parseText("<resource type='velocity' name='input-params' location='bar' />").getRootElement()), resourceDescriptor);
    }

    @Test
    public void testLoadClassFromNewModuleFactory() {
        ModuleFactory moduleFactory = mock(ModuleFactory.class);
        AbstractModuleDescriptor moduleDescriptor = new StringModuleDescriptor(moduleFactory, "foo");
        Plugin plugin = mock(Plugin.class);
        moduleDescriptor.loadClass(plugin, "foo");
        assertEquals(String.class, moduleDescriptor.getModuleClass());
    }

    @Test
    public void testLoadClassFromNewModuleFactoryButUnknownType() {
        ModuleFactory moduleFactory = mock(ModuleFactory.class);
        AbstractModuleDescriptor moduleDescriptor = new AbstractModuleDescriptor(moduleFactory) {
            public AbstractModuleDescriptor init() {
                moduleClassName = "foo";
                return this;
            }

            @Override
            public Object getModule() {
                return null;
            }
        }.init();
        try {
            Plugin plugin = mock(Plugin.class);
            moduleDescriptor.loadClass(plugin, "foo");
            fail("Should have complained about unknown type");
        }
        catch (IllegalStateException ex) {
            // success
        }
    }

    @Test
    public void testLoadClassFromNewModuleFactoryWithExtendsNothingType() {
        ModuleFactory moduleFactory = mock(ModuleFactory.class);
        AbstractModuleDescriptor moduleDescriptor = new ExtendsNothingModuleDescriptor(moduleFactory, "foo");
        Plugin plugin = mock(Plugin.class);

        try {
            moduleDescriptor.loadClass(plugin, "foo");
            fail("Should have complained about extends type");
        }
        catch (IllegalStateException ex) {
            // success
        }
    }

    @Test
    public void testLoadClassFromNewModuleFactoryWithExtendsNumberType() {
        ModuleFactory moduleFactory = mock(ModuleFactory.class);
        AbstractModuleDescriptor moduleDescriptor = new ExtendsNumberModuleDescriptor(moduleFactory, "foo");
        Plugin plugin = mock(Plugin.class);

        try {
            moduleDescriptor.loadClass(plugin, "foo");
            fail("Should have complained about extends type");
        }
        catch (IllegalStateException ex) {
            // success
        }
    }

    @Test
    public void testSingletonness() throws DocumentException, PluginParseException {
        ModuleDescriptor descriptor = makeSingletonDescriptor();

        // try a default descriptor with no singleton="" element. Should _be_ a singleton
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal key=\"key\" name=\"bear\" class=\"org.maera.plugin.mock.MockBear\" />").getRootElement());
        ((StateAware) descriptor).enabled();
        Object module = descriptor.getModule();
        assertTrue(module == descriptor.getModule());

        // now try a default descriptor with singleton="true" element. Should still be a singleton
        descriptor = makeSingletonDescriptor();
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal key=\"key\" name=\"bear\" class=\"org.maera.plugin.mock.MockBear\" singleton=\"true\" />").getRootElement());
        ((StateAware) descriptor).enabled();
        module = descriptor.getModule();
        assertTrue(module == descriptor.getModule());

        // now try reiniting as a non-singleton
        descriptor = makeSingletonDescriptor();
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal key=\"key\" name=\"bear\" class=\"org.maera.plugin.mock.MockBear\" singleton=\"false\" />").getRootElement());
        ((StateAware) descriptor).enabled();
        module = descriptor.getModule();
        assertTrue(module != descriptor.getModule());
    }

    private ModuleDescriptor makeSingletonDescriptor() {
        return new AbstractModuleDescriptor(ModuleFactory.LEGACY_MODULE_FACTORY) {

            private Object module;

            public void init(Plugin plugin, Element element) throws PluginParseException {
                super.init(plugin, element);
            }

            public Object getModule() {
                try {
                    if (!isSingleton()) {
                        return ClassLoaderUtils.loadClass(getModuleClass().getName(), AbstractModuleDescriptorTest.class).newInstance();
                    } else {
                        if (module == null) {
                            module = ClassLoaderUtils.loadClass(getModuleClass().getName(), AbstractModuleDescriptorTest.class).newInstance();
                        }

                        return module;
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException("What happened Dave?");
                }
            }
        };
    }

    private static class ExtendsNothingModuleDescriptor<T> extends AbstractModuleDescriptor<T> {

        public ExtendsNothingModuleDescriptor(ModuleFactory moduleFactory, String className) {
            super(moduleFactory);
            moduleClassName = className;
        }

        @Override
        public T getModule() {
            return null;
        }
    }

    private static class ExtendsNumberModuleDescriptor<T extends Number> extends AbstractModuleDescriptor<T> {

        public ExtendsNumberModuleDescriptor(ModuleFactory moduleFactory, String className) {
            super(moduleFactory);
            moduleClassName = className;
        }

        @Override
        public T getModule() {
            return null;
        }
    }

    private static class StringModuleDescriptor extends AbstractModuleDescriptor<String> {

        public StringModuleDescriptor(ModuleFactory moduleFactory, String className) {
            super(moduleFactory);
            moduleClassName = className;
        }

        @Override
        public String getModule() {
            return null;
        }

    }
}
