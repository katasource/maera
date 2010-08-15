package org.maera.plugin.osgi.factory.descriptor;

import junit.framework.TestCase;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.maera.plugin.Plugin;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestComponentModuleDescriptor extends TestCase {
    public void testEnableDoesNotLoadClass() throws ClassNotFoundException {
        ComponentModuleDescriptor desc = new ComponentModuleDescriptor();

        Element e = DocumentHelper.createElement("foo");
        e.addAttribute("key", "foo");
        e.addAttribute("class", Foo.class.getName());

        Plugin plugin = mock(Plugin.class);
        when(plugin.<Foo>loadClass((String) anyObject(), (Class<?>) anyObject())).thenReturn(Foo.class);
        desc.init(plugin, e);

        Foo.called = false;
        desc.enabled();
        assertFalse(Foo.called);
    }

    public static class Foo {
        public static boolean called;

        public Foo() {
            called = true;
        }
    }
}
