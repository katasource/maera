package org.maera.plugin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ModuleCompleteKeyTest {

    @Test
    public void testBadKey() {
        assertKeyFails("foo");
        assertKeyFails("foo:");
        assertKeyFails(":foo");
        assertKeyFails("");
        assertKeyFails(null);
    }

    @Test
    public void testWorkingKey() {
        ModuleCompleteKey key = new ModuleCompleteKey("foo:bar");
        assertEquals("foo", key.getPluginKey());
        assertEquals("bar", key.getModuleKey());
        assertEquals("foo:bar", key.getCompleteKey());
    }

    private void assertKeyFails(String completeKey) {
        try {
            new ModuleCompleteKey(completeKey);

            fail("Should have thrown IAE");
        }
        catch (IllegalArgumentException ignored) {

        }
    }
}