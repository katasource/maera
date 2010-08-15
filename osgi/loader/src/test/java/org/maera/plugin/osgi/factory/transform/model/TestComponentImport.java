package org.maera.plugin.osgi.factory.transform.model;

import junit.framework.TestCase;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.maera.plugin.PluginParseException;

public class TestComponentImport extends TestCase {
    public void testValidate() {
        Element e = DocumentFactory.getInstance().createElement("component-import");
        e.addAttribute("key", "foo");
        e.addAttribute("interface", "foo.Bar");

        ComponentImport ci = new ComponentImport(e);
        assertEquals("foo", ci.getKey());
        assertEquals("foo.Bar", ci.getInterfaces().iterator().next());

        try {
            e.remove(e.attribute("interface"));
            new ComponentImport(e);
            fail();
        }
        catch (PluginParseException ex) {
            // test passed
        }


        Element inf = DocumentFactory.getInstance().createElement("interface");
        e.add(inf);
        try {
            new ComponentImport(e);
            fail();
        }
        catch (PluginParseException ex) {
            // test passed
        }

        inf.setText("foo.Bar");
        ci = new ComponentImport(e);
        assertEquals("foo.Bar", ci.getInterfaces().iterator().next());

    }
}
