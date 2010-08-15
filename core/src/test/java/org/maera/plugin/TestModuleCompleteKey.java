/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 29, 2004
 * Time: 3:28:26 PM
 */
package org.maera.plugin;

import junit.framework.TestCase;

public class TestModuleCompleteKey extends TestCase {
    public void testWorkingKey() {
        ModuleCompleteKey key = new ModuleCompleteKey("foo:bar");
        assertEquals("foo", key.getPluginKey());
        assertEquals("bar", key.getModuleKey());
        assertEquals("foo:bar", key.getCompleteKey());
    }

    public void testBadKey() {
        assertKeyFails("foo");
        assertKeyFails("foo:");
        assertKeyFails(":foo");
        assertKeyFails("");
        assertKeyFails(null);
    }

    private void assertKeyFails(String completeKey) {
        try {
            new ModuleCompleteKey(completeKey);
            fail("Should have thrown IAE");
        }
        catch (IllegalArgumentException e) {
            return;
        }
    }
}