package org.maera.plugin.util.validation;

import junit.framework.TestCase;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.maera.plugin.PluginParseException;

import static org.maera.plugin.util.validation.ValidationPattern.createPattern;
import static org.maera.plugin.util.validation.ValidationPattern.test;

public class TestValidationPattern extends TestCase {
    private Element root;

    @Override
    public void setUp() {
        DocumentFactory factory = DocumentFactory.getInstance();
        root = factory.createElement("root");
        root.addAttribute("foo", "bar");
        Element child = factory.createElement("child");
        child.addAttribute("some", "thing");
        child.setText("mybody");
        Element emptyChild = factory.createElement("child");
        root.add(child);
        root.add(emptyChild);
    }

    public void testSuccess() {
        createPattern().
                rule(".",
                        test("child").withError("Child is required"),
                        test("not(baz)").withError("Baz should not exist")).
                rule("child[1]",
                        test(".[@some = 'thing']").withError("Need some attribute")).
                evaluate(root);

    }

    /*public void testSuccessPerfTest()
    {
        ValidationPattern ptn = createPattern().
                rule(".",
                        test("child").withError("Child is required"),
                        test("not(baz)").withError("Baz should not exist")).
                rule("child[1]",
                        test(".[@some = 'thing']").withError("Need some attribute"));

        for (int x=0; x<1000; x++)
        {
            ptn.evaluate(root);
        }

        long start = System.currentTimeMillis();
        for (int x=0; x<5000; x++)
        {
            ptn.evaluate(root);
        }
        long end = System.currentTimeMillis();
        System.out.println("Time elapsed: "+(end-start)+" ms");

    }*/

    public void testErrorMessageWithEmptyList() {
        try {
            createPattern().
                    rule(".",
                            test("baz").withError("Baz should exist")).
                    evaluate(root);
            fail();
        }
        catch (PluginParseException ex) {
            assertTrue(ex.getMessage().startsWith("Baz should exist"));
        }
    }

    public void testErrorMessageWithNull() {
        try {
            createPattern().
                    rule(".",
                            test("baz[1]").withError("Baz should exist")).
                    evaluate(root);
            fail();
        }
        catch (PluginParseException ex) {
            assertTrue(ex.getMessage().startsWith("Baz should exist"));
        }
    }

    public void testErrorMessageWithBoolean() {
        try {
            createPattern().
                    rule(".",
                            test("not(not(baz))").withError("Baz should exist")).
                    evaluate(root);
            fail();
        }
        catch (PluginParseException ex) {
            assertTrue(ex.getMessage().startsWith("Baz should exist"));
        }
    }


}
