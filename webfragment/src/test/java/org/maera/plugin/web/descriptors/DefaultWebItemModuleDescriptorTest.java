package org.maera.plugin.web.descriptors;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.impl.AbstractPlugin;

import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DefaultWebItemModuleDescriptorTest {

    private final Plugin plugin = new MockPlugin(this.getClass().getName());
    private WebItemModuleDescriptor descriptor;

    @Before
    public void setUp() throws Exception {
        descriptor = new DefaultWebItemModuleDescriptor(new MockWebInterfaceManager());
    }

    @Test
    public void testGetStyleClass() throws DocumentException, PluginParseException {
        final String className = "testClass";
        final String styleClass = "<styleClass>" + className + "</styleClass>";

        final Element element = createElement(styleClass);
        descriptor.init(plugin, element);

        assertEquals(className, descriptor.getStyleClass());
    }

    @Test
    public void testGetStyleClassEmpty() throws DocumentException, PluginParseException {
        final String styleClass = "<styleClass></styleClass>";

        final Element element = createElement(styleClass);
        descriptor.init(plugin, element);

        assertEquals("", descriptor.getStyleClass());
    }

    @Test
    public void testGetStyleClassNone() throws DocumentException, PluginParseException {
        final String styleClass = "";

        final Element element = createElement(styleClass);
        descriptor.init(plugin, element);

        assertNotNull(descriptor.getStyleClass());
        assertEquals("", descriptor.getStyleClass());
    }

    @Test
    public void testGetStyleClassSpaceSeparated() throws DocumentException, PluginParseException {
        final String className = "testClass testClass2";
        final String styleClass = "<styleClass>" + className + "</styleClass>";

        final Element element = createElement(styleClass);
        descriptor.init(plugin, element);

        assertEquals(className, descriptor.getStyleClass());
    }

    @Test
    public void testGetStyleClassTrimmed() throws DocumentException, PluginParseException {
        final String className = "testClass";
        final String styleClass = "<styleClass>   " + className + "   </styleClass>";

        final Element element = createElement(styleClass);
        descriptor.init(plugin, element);

        assertEquals(className, descriptor.getStyleClass());
    }

    private Element createElement(final String childElement) throws DocumentException {
        final String rootElement = "<root key=\"key\">" + childElement + "</root>";
        final Document document = DocumentHelper.parseText(rootElement);
        return document.getRootElement();
    }

    private class MockPlugin extends AbstractPlugin {

        MockPlugin(final String key) {
            setKey(key);
            setName(key);
        }

        public ClassLoader getClassLoader() {
            return this.getClass().getClassLoader();
        }

        public URL getResource(final String path) {
            return null;
        }

        public InputStream getResourceAsStream(final String name) {
            return null;
        }

        public boolean isDeleteable() {
            return false;
        }

        public boolean isDynamicallyLoaded() {
            return false;
        }

        public boolean isUninstallable() {
            return false;
        }

        public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException {
            return null;
        }
    }
}
